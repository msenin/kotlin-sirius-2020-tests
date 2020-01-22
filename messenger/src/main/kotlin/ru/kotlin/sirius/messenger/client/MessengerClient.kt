package ru.kotlin.sirius.messenger.client

import ru.kotlin.sirius.messenger.server.*

/**
 * Клиент мессенджера
 */
class MessengerClient(private val server: MessengerServer) {

    fun register(login: String, name: String, password: String) {
        server.usersCreate(login, name, password)
    }

    fun signIn(userId: String, password: String): User {
        val token = server.signIn(userId, password)
        return User(userId, token, this)
    }

    fun signOut(userId: String, token: String) {
        server.signOut(userId, token)
    }

    fun usersListById(userIdToFind: String, userId: String, token: String): List<UserInfo> {
        return server.usersListById(userIdToFind, userId, token)
    }

    fun chatsListByUserId(userId: String, token: String): List<ChatInfo> {
        return server.chatsListByUserId(userId, token)
    }

    fun getSystemUserId(): String {
        return server.getSystemUserId()
    }

    fun chatsCreate(chatName: String, userId: String, token: String): ChatInfo {
        return server.chatsCreate(chatName, userId, token)
    }

    fun chatsJoin(chatId: Int, secret: String, userId: String, token: String, chatName: String? = null)  {
        server.chatsJoin(chatId, secret, userId, token, chatName)
    }

    fun usersInviteToChat(userIdToInvite: String, chatId: Int, userId: String, token: String) {
        server.usersInviteToChat(userIdToInvite, chatId, userId, token)
    }

    fun chatMessagesCreate(chatId: Int, text: String, userId: String, token: String): MessageInfo {
        return server.chatMessagesCreate(chatId, text, userId, token)
    }

    fun chatsMembersList(chatId: Int, userId: String, token: String): List<MemberInfo> {
        return server.chatsMembersList(chatId, userId, token)
    }

    fun chatMessagesList(chatId: Int, userId: String, token: String): List<MessageInfo> {
        return server.chatMessagesList(chatId, userId, token)
    }
}

class UserNotSignedInException : Throwable()

open class ClientAware (val client: MessengerClient)
open class UserAware (val user: User) : ClientAware(user.client)
open class ChatAware (chat: Chat) : UserAware(chat.user)