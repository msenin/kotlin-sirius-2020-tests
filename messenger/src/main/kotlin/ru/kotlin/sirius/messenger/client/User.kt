package ru.kotlin.sirius.messenger.client

import ru.kotlin.sirius.messenger.api.AuthInfo
import java.time.Instant

/**
 * Пользователь
 */
class User internal constructor(val userId: String,
                                var authInfo: AuthInfo,
                                client: MessengerClient) : ClientAware(client) {
    val chats =  mutableListOf<Chat>()
    lateinit var name: String
    lateinit var systemChat: Chat
    private lateinit var lastUpdated : Instant

    init {
        refresh()
    }

    fun signOut() {
        client.signOut(authInfo)
    }

    fun refresh() {
        val userInfo = client.usersListById(userId, authInfo) ?: throw UserNotFoundException()
        name = userInfo.displayName
        refreshChats()
        lastUpdated = Instant.now()
    }

    fun refreshChats() {
        val chatsInfo = client.chatsListByUserId(authInfo)
        chats.clear()
        chats.addAll(chatsInfo.map { Chat(it.chatId, this) })
        systemChat = chats.first { chat ->
            chat.members.any {
                it.memberUserId == client.getSystemUserId(authInfo)
            }
        }
    }

    fun createChat(chatName: String): Chat {
        val chatInfo = client.chatsCreate(chatName, authInfo)
        val newChat = Chat(chatInfo.chatId, this)
        chats.add(newChat)
        return newChat
    }

    fun joinToChat(chatId: Int, secret: String, chatName: String? = null): Chat {
        client.chatsJoin(chatId, secret, authInfo, chatName)
        val newChat = Chat(chatId, this)
        chats.add(newChat)
        return newChat
    }

    override fun toString(): String {
        return "$name ($userId)"
    }
}
