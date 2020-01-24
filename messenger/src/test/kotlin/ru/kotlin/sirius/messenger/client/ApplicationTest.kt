package ru.kotlin.sirius.messenger.client

import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.setBody
import io.ktor.server.testing.withTestApplication
import org.junit.jupiter.api.Test
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.request.header
import io.ktor.util.KtorExperimentalAPI
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import ru.kotlin.sirius.messenger.server.NewUserInfo
import ru.kotlin.sirius.messenger.server.PasswordInfo
import ru.kotlin.sirius.messenger.server.module
import kotlin.test.assertNotNull

class ApplicationTest {

    private val objectMapper = jacksonObjectMapper()

    @Test
    fun testHealth() {
        withTestApplication({ module() }) {
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
        withTestApplication({ module() }) {

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
