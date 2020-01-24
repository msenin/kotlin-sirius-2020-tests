package ru.kotlin.sirius.messenger.client

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.apache.commons.logging.LogFactory
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.jackson.JacksonConverterFactory
import ru.kotlin.sirius.messenger.api.MessengerApi
import ru.kotlin.sirius.messenger.api.*
import java.lang.IllegalStateException

/**
 * Клиент мессенджера
 */
class MessengerClient(messengerBaseUrl: String) {
    val logger = LogFactory.getLog(MessengerClient::class.java)
    private val objectMapper = jacksonObjectMapper()
    private val api = Retrofit.Builder()
        .baseUrl(messengerBaseUrl)
        .addConverterFactory(JacksonConverterFactory.create(objectMapper))
        .build().create(MessengerApi::class.java)

    private fun <T> Response<T>.resultOrNull() : T? {
        if (isSuccessful) {
            return body()
        }
        val errorMessage = "code: ${code()} response: ${errorBody()}"
        logger.debug(errorMessage)
        throw UnexpectedSeverResponseException(errorMessage)
    }

    private fun <T> Response<T>.result() : T {
        val result = resultOrNull()
        if (result == null) {
            val errorMessage = "Unexpected empty response"
            logger.debug(errorMessage)
            throw UnexpectedSeverResponseException(errorMessage)
        }
        return result
    }

    fun register(login: String, name: String, password: String): UserInfo? {
        return api.registerUser(NewUserInfo(login, name, password)).execute().resultOrNull()
    }

    fun signIn(userId: String, password: String): User {
        val authInfo = api.signIn(userId, PasswordInfo(password)).execute().resultOrNull() ?: throw UserNotSignedInException()
        return User(userId, authInfo, this)
    }

    fun signOut(authInfo: AuthInfo) {
        api.signOut(authInfo.accessToken)
    }

    fun usersListById(userIdToFind: String, authInfo: AuthInfo): UserInfo? {
        return api.getUserByUserId(userIdToFind, authInfo.accessToken).execute().resultOrNull()
    }

    fun chatsListByUserId(authInfo: AuthInfo): List<ChatInfo> {
        return api.listChats(authInfo.accessToken).execute().resultOrNull() ?: emptyList()
    }

    fun getSystemUserId(authInfo: AuthInfo): String {
        return api.getSystemUser(authInfo.accessToken).execute().resultOrNull()?.userId ?: throw IllegalStateException("System user not found!")
    }

    fun chatsCreate(chatName: String, authInfo: AuthInfo): ChatInfo {
        return api.createChat(NewChatInfo(chatName), authInfo.accessToken).execute().result()
    }

    fun chatsJoin(chatId: Int, secret: String, authInfo: AuthInfo, chatName: String? = null)  {
        api.joinToChat(chatId, JoinChatInfo(chatName, secret), authInfo.accessToken).execute().result()
    }

    fun usersInviteToChat(userIdToInvite: String, chatId: Int, authInfo: AuthInfo) {
        api.inviteToChat(chatId, InviteChatInfo(userIdToInvite), authInfo.accessToken).execute().result()
    }

    fun chatMessagesCreate(chatId: Int, text: String, authInfo: AuthInfo): MessageInfo {
        return api.sendMessage(chatId, NewMessageInfo(text), authInfo.accessToken).execute().result()
    }

    fun chatsMembersList(chatId: Int, authInfo: AuthInfo): List<MemberInfo> {
        return api.listChatMembers(chatId, authInfo.accessToken).execute().resultOrNull() ?: emptyList()
    }

    fun chatMessagesList(chatId: Int, authInfo: AuthInfo): List<MessageInfo> {
        return api.listMessages(chatId, authInfo.accessToken).execute().resultOrNull()  ?: emptyList()
    }
}

class UserNotSignedInException : Throwable()
class UserNotFoundException : Throwable()
class UnexpectedSeverResponseException(message: String) : Throwable(message)

open class ClientAware (val client: MessengerClient)
open class UserAware (val user: User) : ClientAware(user.client)
open class ChatAware (chat: Chat) : UserAware(chat.user)