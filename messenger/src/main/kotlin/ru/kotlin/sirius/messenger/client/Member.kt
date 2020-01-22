package ru.kotlin.sirius.messenger.client

/**
 * Участник чата
 */
class Member(val memberId: Int, val displayName: String, val memberUserId: String, chat: Chat) : ChatAware(chat)