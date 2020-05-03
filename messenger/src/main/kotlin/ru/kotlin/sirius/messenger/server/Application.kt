package ru.kotlin.sirius.messenger.server

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTVerificationException
import com.fasterxml.jackson.databind.SerializationFeature
import io.ktor.application.Application
import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.auth.Authentication
import io.ktor.auth.authenticate
import io.ktor.auth.authentication
import io.ktor.auth.jwt.jwt
import io.ktor.features.CallLogging
import io.ktor.features.ContentNegotiation
import io.ktor.features.StatusPages
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.jackson.jackson
import io.ktor.request.header
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.response.respondText
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.routing
import io.ktor.server.netty.EngineMain
import io.ktor.util.KtorExperimentalAPI
import io.ktor.util.pipeline.PipelineContext
import org.slf4j.event.Level
import java.util.*

const val databaseUrl = "ktor.settings.databaseUrl"
const val databaseDriver = "ktor.settings.databaseDriver"
const val accessTokenValidity = "ktor.jwt.access_token_validity_in_ms"
const val refreshTokenValidity = "ktor.jwt.refresh_token_validity_in_days"

lateinit var storage : IStorage
lateinit var server : MessengerServer

fun main(args: Array<String>) {
    EngineMain.main(args)
}

open class JwtUtil(secret: String, private val accessTokenValidityInMs: Long, private val refreshTokenValidityInDays: Long) {
    private val algorithm = Algorithm.HMAC256(secret)
    val verifier: JWTVerifier = JWT.require(algorithm).build()
    val verifierExpired: JWTVerifier = JWT.require(algorithm).acceptExpiresAt(refreshTokenValidityInDays * 86400L).build()
    fun createAccessToken(id: String): String = JWT.create()
        .withSubject("Authentication")
        .withExpiresAt(Date(System.currentTimeMillis() + accessTokenValidityInMs))
        .withClaim("id", id)
        .sign(algorithm)
    fun createRefreshToken(): String = JWT.create()
        .withSubject("Refresh")
        .withExpiresAt(Date(System.currentTimeMillis() + refreshTokenValidityInDays * 86400000L))
        .sign(algorithm)
}

val ApplicationCall.user get() = authentication.principal<UserInfo>()

@KtorExperimentalAPI
fun Application.module() {
    val config = this.environment.config
    val url = config.property(databaseUrl).getString()
    val driver = config.property(databaseDriver).getString()
    storage = DbStorage(url, driver)
    val jwt = JwtUtil(
        environment.config.propertyOrNull("ktor.jwt.secret")?.getString() ?: "default_secret",
        environment.config.propertyOrNull(accessTokenValidity)?.getString()?.toLong() ?: 300 * 1000L,
        environment.config.propertyOrNull(refreshTokenValidity)?.getString()?.toLong() ?: 30L
    )
    server = MessengerServer(storage, jwt::createRefreshToken)

    install(ContentNegotiation) {
        jackson {
            enable(SerializationFeature.INDENT_OUTPUT)
        }
    }

    install(StatusPages) {
        exception<UserNotAuthorizedException> {
            call.respond(HttpStatusCode.Unauthorized)
        }
        exception<JWTVerificationException> {
            call.respond(HttpStatusCode.Unauthorized)
        }
        exception<UserNotMemberException> {
            call.respond(HttpStatusCode.Forbidden)
        }
        exception<WrongChatSecretException> {
            call.respond(HttpStatusCode.Forbidden)
        }
        exception<UserNotFoundException> {
            call.respond(HttpStatusCode.NotFound)
        }
        exception<ChatNotFoundException> {
            call.respond(HttpStatusCode.NotFound)
        }
        exception<UserAlreadyExistsException> {
            call.respond(HttpStatusCode.Conflict)
        }
        exception<UserAlreadyMemberException> {
            call.respond(HttpStatusCode.Conflict)
        }
        exception<MessageAlreadyExistsException> {
            call.respond(HttpStatusCode.Conflict)
        }
        exception<SecretAlreadyExistsException> {
            call.respond(HttpStatusCode.Conflict)
        }
        exception<NotImplementedError> {
            call.respond(HttpStatusCode.NotImplemented)
        }
    }

    install(CallLogging) {
        level = Level.INFO
    }

    install(Authentication) {
        jwt {
            verifier(jwt.verifier)
            validate {
                val userId = it.payload.getClaim("id").asString()
                server.findUserById(userId)
            }
        }
    }

    routing {
        get("/v1/health") {
            call.respondText("OK", ContentType.Text.Html)
        }

        post("/v1/me/refresh") {
            val header = call.request.header("Authorization") ?: throw UserNotAuthorizedException()
            val info = call.receive<RefreshTokenInfo>()
            if (!header.startsWith("Bearer ")) throw UserNotAuthorizedException()
            val token = header.substring("Bearer ".length)
            val decodedAccessToken = jwt.verifierExpired.verify(token)
            if (decodedAccessToken.subject != "Authentication") {
                throw UserNotAuthorizedException()
            }
            val userId = decodedAccessToken.getClaim("id").asString() ?: throw UserNotAuthorizedException()
            val decodedRefreshToken =jwt.verifier.verify(info.refreshToken)
            if (decodedRefreshToken.subject != "Refresh") {
                throw UserNotAuthorizedException()
            }
            val refreshToken = server.replaceRefreshToken(userId, info.refreshToken)
            val accessToken = jwt.createAccessToken(userId)
            call.respond(AuthInfo(accessToken, refreshToken))
        }

        post("/v1/users") {
            val info = call.receive<NewUserInfo>()
            val newUser = server.usersCreate(info.userId, info.displayName, info.password)
            call.respond(newUser)
        }

        post("/v1/users/{id}/signin") {
            val userId = call.parameters["id"]
            if (userId == null) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "user id not provided"))
                return@post
            }
            val info = call.receive<PasswordInfo>()
            val refreshToken = server.signIn(userId, info.password)
            val accessToken = jwt.createAccessToken(userId)
            call.respond(AuthInfo(accessToken, refreshToken))
        }

        authenticate {
            post("/v1/me/signout") {
                withUser { user ->
                    call.respond(server.signOut(user))
                }
            }

            post("/v1/chats") {
                withUser { user ->
                    val info = call.receive<NewChatInfo>()
                    val newChat = server.chatsCreate(info.defaultName, user)
                    call.respond(newChat)
                }
            }

            post("/v1/chats/{id}/invite") {
                withUser { user ->
                    val chatId = call.parameters["id"]?.toInt() ?: throw ChatNotFoundException()
                    val info = call.receive<InviteChatInfo>()
                    server.usersInviteToChat(info.userId, chatId, user)
                    call.respond(mapOf("status" to "OK"))
                }
            }

            post("/v1/chats/{id}/join") {
                withUser { user ->
                    val chatId = call.parameters["id"]?.toInt() ?: throw ChatNotFoundException()
                    val info = call.receive<JoinChatInfo>()
                    server.chatsJoin(chatId, info.secret, user, info.defaultName)
                    call.respond(mapOf("status" to "OK"))
                }
            }

            get("/v1/me/chats") {
                withUser { user ->
                    val chats = server.usersListChats(user)
                    call.respond(chats)
                }
            }

            get("/v1/chats/{id}/members") {
                withUser { user ->
                    val chatId = call.parameters["id"]?.toInt() ?: throw ChatNotFoundException()
                    val members = server.chatsMembersList(chatId, user)
                    call.respond(members)
                }
            }

            post("/v1/chats/{id}/messages") {
                withUser { user ->
                    val chatId = call.parameters["id"]?.toInt() ?: throw ChatNotFoundException()
                    val info = call.receive<MessageText>()
                    val messageInfo = server.chatMessagesCreate(chatId, info.text, user)
                    call.respond(messageInfo)
                }
            }

            get("/v1/chats/{id}/messages") {
                withUser { user ->
                    val chatId = call.parameters["id"]?.toInt() ?: throw ChatNotFoundException()
                    val afterId = call.parameters["after_id"]?.toInt() ?: 0
                    val messages = server.chatMessagesList(chatId, user, afterId)
                    call.respond(messages)
                }
            }

            get("/v1/users") {
                val namePattern = call.parameters["name"]
                val users = server.usersListByName(namePattern)
                call.respond(users)
            }

            get("/v1/users/{id}") {
                val userToFindId = call.parameters["id"] ?: throw UserNotFoundException()
                call.respond(server.usersListById(userToFindId).first())
            }

            get("/v1/admin") {
                call.respond(server.getSystemUser())
            }

            post("/v1/me/invalidate") {
                withUser { user ->
                    val info = call.receive<RefreshTokenInfo>()
                    call.respond(server.invalidateRefreshToken(user.userId, info.refreshToken))
                }
            }
        }
    }
}

suspend inline fun PipelineContext<Unit,ApplicationCall>.withUser(function: PipelineContext<Unit,ApplicationCall>.(user: UserInfo) -> Unit) {
    try {
        val user = call.user
        if (user == null) {
            call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "user not authorized"))
            return
        }
        function(user)
    }
    catch (e: UserNotFoundException) {
        call.respond(HttpStatusCode.NotFound, mapOf("error" to "user not found"))
    }
    catch (e: UserNotMemberException) {
        call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "user not authorized"))
    }
    catch (e: UserAlreadyMemberException) {
        call.respond(HttpStatusCode.Conflict, mapOf("error" to "user already member of chat"))
    }
    catch (e: MessageAlreadyExistsException) {
        call.respond(HttpStatusCode.Conflict, mapOf("error" to "message already exists"))
    }
    catch (e: SecretAlreadyExistsException) {
        call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "internal error"))
    }
    catch (e: UserNotAuthorizedException) {
        call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "user not authorized"))
    }
    catch (e: UserAlreadyExistsException) {
        call.respond(HttpStatusCode.Conflict, mapOf("error" to "user already exists"))
    }
    catch (e: Exception) {
        call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "internal error"))
    }
}


