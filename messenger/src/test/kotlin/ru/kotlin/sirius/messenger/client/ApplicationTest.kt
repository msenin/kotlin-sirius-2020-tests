package ru.kotlin.sirius.messenger.client

import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.setBody
import io.ktor.server.testing.withTestApplication
import org.junit.jupiter.api.Test
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.application.Application
import io.ktor.config.MapApplicationConfig
import io.ktor.request.header
import io.ktor.util.KtorExperimentalAPI
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import ru.kotlin.sirius.messenger.server.*
import java.io.File
import java.nio.file.Files
import kotlin.test.assertNotNull

const val testDbFolder = "./build/test-db"

fun Application.testModule() {
    (environment.config as MapApplicationConfig).apply {
        put(databaseUrl, "jdbc:h2:$testDbFolder/database")
        put(databaseDriver, "org.h2.Driver")
    }
    module()
}

class ApplicationTest {

    private val objectMapper = jacksonObjectMapper()

    @BeforeEach
    fun removeTestDatabase() {
        File(testDbFolder).deleteRecursively()
    }

    @Test
    fun testHealth() {
        withTestApplication({ testModule() }) {
            handleRequest(HttpMethod.Get, "/v1/health").apply {
                assertEquals(HttpStatusCode.OK, response.status())
                assertEquals("OK", response.content)
            }
        }
    }

    data class ClientUserInfo(val userId: String, val displayName: String)

    @Test
    fun testRegisterLoginLogout() {
        val userData = NewUserInfo("pupkin", "Pupkin", "password")
        withTestApplication({ testModule() }) {

            // Register
            handleRequest {
                method = HttpMethod.Post
                uri = "/v1/users"
                addHeader("Content-type", "application/json")
                setBody(objectMapper.writeValueAsString(userData))
            }.apply {
                assertEquals(HttpStatusCode.OK, response.status())
                val user = objectMapper.readValue<ClientUserInfo>(response.content!!)
                assertEquals(userData.userId, user.userId)
                assertEquals(userData.displayName, user.displayName)

                // Login
                handleRequest {
                    method = HttpMethod.Post
                    uri = "/v1/users/pupkin/signin"
                    addHeader("Content-type", "application/json")
                    setBody(objectMapper.writeValueAsString(
                        PasswordInfo(
                            "password"
                        )
                    ))
                }.apply {
                    assertEquals(HttpStatusCode.OK, response.status())
                    val tokenInfo = objectMapper.readValue<HashMap<String,String>>(response.content!!)
                    val token = tokenInfo["accessToken"]
                    assertNotNull(token)

                    // Logout
                    handleRequest {
                        method = HttpMethod.Post
                        addHeader("Authorization", "Bearer $token")
                        uri = "/v1/me/signout"
                    }.apply {
                        assertEquals(HttpStatusCode.OK, response.status())
                    }
                }
            }
        }
    }
}
