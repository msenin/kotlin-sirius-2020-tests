package ru.kotlin.sirius.messenger.client

import java.time.Instant

/**
 * Пользователь
 */
class User internal constructor(val userId: String, var token: String, client: MessengerClient) : ClientAware(client) {
    val chats =  mutableListOf<Chat>()
    lateinit var name: String
    lateinit var systemChat: Chat
    lateinit var lastUpdated : Instant

    init {
        refresh()
    }

    fun signOut() {
        client.signOut(userId, token)
    }

    fun refresh() {
        val userInfo = client.usersListById(userId, userId, token).first()
        name = userInfo.displayName
        refreshChats()
        lastUpdated = Instant.now()
    }

    fun refreshChats() {
        val chatsInfo = client.chatsListByUserId(userId, token)
        chats.clear()
        chats.addAll(chatsInfo.map { Chat(it.chatId, this) })
        systemChat = chats.first { chat ->
            chat.members.any {
                it.memberUserId == client.getSystemUserId()
            }
        }
    }

    fun createChat(chatName: String): Chat {
        val chatInfo = client.chatsCreate(chatName, userId, token)
        val newChat = Chat(chatInfo.chatId, this)
        chats.add(newChat)
        return newChat
    }

    fun joinToChat(chatId: Int, secret: String, chatName: String? = null): Chat {
        client.chatsJoin(chatId, secret, userId, token, chatName)
        val newChat = Chat(chatId, this)
        chats.add(newChat)
        return newChat
    }

    override fun toString(): String {
        return "$name ($userId)"
    }
}
