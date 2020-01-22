package ru.kotlin.sirius.messenger.server

/**
 * Пользователь
 */
data class UserInfo(val userId: String, val displayName: String, internal val passwordHash: String)