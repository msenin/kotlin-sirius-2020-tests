package ru.kotlin.sirius.messenger.client

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Сообщение
 */
class Message (val messageId: Int, val author: Member, val text: String, val createdOn: Instant, chat: Chat) : ChatAware(chat) {
    companion object {
        val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd-MMM-yy hh:mm:ss").withZone(ZoneId.systemDefault())
    }
    override fun toString(): String {
        return "${author.displayName} (${author.memberUserId}) [${formatter.format(createdOn)}]: $text"
    }
}
