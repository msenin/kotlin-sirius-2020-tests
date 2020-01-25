package ru.kotlin.sirius.messenger.server

/**
 * Участник чата
 */
data class MemberInfo(val memberId: Int, val chatId: Int, val chatDisplayName: String, val memberDisplayName: String, val userId: String)
data class NewMemberInfo(val chatId: Int, val chatDisplayName: String, val memberDisplayName: String, val userId: String)