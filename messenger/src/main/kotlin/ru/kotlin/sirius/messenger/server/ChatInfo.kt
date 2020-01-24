package ru.kotlin.sirius.messenger.server

/**
 * Чат
 */
data class ChatInfo(val chatId: Int, val defaultName: String)

data class NewChatInfo(val defaultName: String)
data class JoinChatInfo(val defaultName: String?, val secret: String)
data class InviteChatInfo(val userId: String)

