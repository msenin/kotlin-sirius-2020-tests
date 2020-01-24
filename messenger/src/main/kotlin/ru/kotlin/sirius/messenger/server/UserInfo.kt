package ru.kotlin.sirius.messenger.server

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import io.ktor.auth.Principal

/**
 * Пользователь
 */
@JsonIgnoreProperties("passwordHash")
data class UserInfo(val userId: String,
                    val displayName: String,
                    @field:JsonProperty("passwordHash")
                    val passwordHash: String) : Principal

data class NewUserInfo(val userId: String,
                       val displayName: String,
                       val password: String)

data class PasswordInfo(val password: String)

data class AuthInfo(val accessToken: String, val refreshToken: String)