package ru.kotlin.sirius.messenger.server

import java.time.Instant

/**
 * Сообщение
 */
data class MessageInfo (val messageId: Int, val memberId: Int, var text: String, val createdOn: Long = System.currentTimeMillis())
data class NewMessageInfo (val memberId: Int, var text: String)

data class MessageText (var text: String)
