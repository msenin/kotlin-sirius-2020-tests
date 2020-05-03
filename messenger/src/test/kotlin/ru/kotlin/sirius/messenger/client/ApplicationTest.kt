package ru.kotlin.sirius.messenger.client

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.application.Application
import io.ktor.config.MapApplicationConfig
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import ru.kotlin.sirius.messenger.server.*
import java.io.File
import java.lang.Thread.sleep
import kotlin.test.assertNotNull

const val testDbFolder = "./build/test-db"
const val expirationTimeInMs = 3000L

fun Application.testModule() {

    (environment.config as MapApplicationConfig).apply {
        put(databaseUrl, "jdbc:h2:$testDbFolder/database")
        put(databaseDriver, "org.h2.Driver")
        put(accessTokenValidity, "$expirationTimeInMs")
    }
    module()
}


class ApplicationTest {
    private val objectMapper = jacksonObjectMapper()

    private val testPassword = "password"
    private val testUserId = "pupkin"
    private val userData = NewUserInfo(testUserId, "Pupkin", testPassword)

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
    fun testRegisterLoginLogout() = withSignedInTestUser { token, _ ->
        handleRequest {
            method = HttpMethod.Post
            addHeader("Authorization", "Bearer $token")
            uri = "/v1/me/signout"
        }.apply {
            assertEquals(HttpStatusCode.OK, response.status())
        }
    }

    @Test
    fun testWrongLogin()  = withSignedInTestUser { _, _ ->
        handleRequest {
            method = HttpMethod.Post
            uri = "/v1/users/$testUserId/signin"
            addHeader("Content-type", "application/json")
            setBody(objectMapper.writeValueAsString(PasswordInfo("wrong_password")))
        }.apply {
            assertEquals(HttpStatusCode.Unauthorized, response.status())
        }
    }

    @Test
    fun testWrongToken() = withSignedInTestUser { _, _ ->
        handleRequest {
            method = HttpMethod.Post
            addHeader("Authorization", "Bearer wrong_token")
            uri = "/v1/me/signout"
        }.apply {
            assertEquals(HttpStatusCode.Unauthorized, response.status())
        }
    }

    @Test
    fun testTokenExpired() = withSignedInTestUser { token, _ ->
        // Wait token to expire
        sleep(expirationTimeInMs + 3000L)
        // Logout
        handleRequest {
            method = HttpMethod.Post
            addHeader("Authorization", "Bearer $token")
            uri = "/v1/me/signout"
        }.apply {
            assertEquals(HttpStatusCode.Unauthorized, response.status())
        }
    }

    @Test
    fun testRefreshExpiredToken() = withSignedInTestUser { accessToken, refreshToken ->
        // Wait access token to expire
        sleep(expirationTimeInMs + 3000L)
        // Refresh
        handleRequest {
            method = HttpMethod.Post
            addHeader("Authorization", "Bearer $accessToken")
            uri = "/v1/me/refresh"
            addHeader("Content-type", "application/json")
            setBody(objectMapper.writeValueAsString(RefreshTokenInfo(refreshToken)))
        }.apply {
            assertEquals(HttpStatusCode.OK, response.status())
            val authInfo = objectMapper.readValue<HashMap<String,String>>(response.content!!)
            val newAccessToken = authInfo["accessToken"]
            val newRefreshToken = authInfo["refreshToken"]
            assertNotNull(newAccessToken)
            assertNotNull(newRefreshToken)
            // Test newAccessToken
            handleRequest {
                method = HttpMethod.Post
                addHeader("Authorization", "Bearer $newAccessToken")
                uri = "/v1/me/signout"
            }.apply {
                assertEquals(HttpStatusCode.OK, response.status())
            }
        }
    }

    private fun withRegisteredTestUser(block: TestApplicationEngine.() -> Unit) {
        withTestApplication({ testModule() }) {
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
                this@withTestApplication.block()
            }
        }
    }

    private fun withSignedInTestUser(block: TestApplicationEngine.(String,String) -> Unit) = withRegisteredTestUser {
        handleRequest {
            method = HttpMethod.Post
            uri = "/v1/users/$testUserId/signin"
            addHeader("Content-type", "application/json")
            setBody(objectMapper.writeValueAsString(PasswordInfo(testPassword)))
        }.apply {
            assertEquals(HttpStatusCode.OK, response.status())
            val authInfo = objectMapper.readValue<HashMap<String,String>>(response.content!!)
            val accessToken = authInfo["accessToken"]
            val refreshToken = authInfo["refreshToken"]
            assertNotNull(accessToken)
            assertNotNull(refreshToken)
            this@withRegisteredTestUser.block(accessToken, refreshToken)
        }
    }

}
