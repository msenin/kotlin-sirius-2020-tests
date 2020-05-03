package ru.kotlin.sirius.messenger.server

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import java.security.SecureRandom
import java.util.*

/**
 * Серверная часть мессенджера
 */
class MessengerServer(
    private val storage: IStorage,
    private val refreshTokenGenerator: () -> String) {

    internal companion object {
        val systemUser = UserInfo("admin", "Administration", "")
        private val passwordEncoder = BCryptPasswordEncoder(4, SecureRandom())
    }

    init {
        if (!storage.containsUser(systemUser.userId)) {
            storage.addUser(systemUser)
        }
    }

    fun usersCreate(userId: String, name: String, password: String) : UserInfo {
        if (storage.containsUser(userId)) {
            throw UserAlreadyExistsException()
        }
        val newUser = UserInfo(
            userId,
            name,
            passwordEncoder.encode(
                password
            )
        )
        storage.addUser(newUser)
        createSystemChatForUser(newUser)
        return newUser
    }

    private fun createSystemChatForUser(userInfo: UserInfo) {
        val systemChat = doCreateChat("System for ${userInfo.userId}", systemUser)
        val member = NewMemberInfo(systemChat.chatId,
            "System",
            userInfo.displayName,
            userInfo.userId
        )
        storage.addChatMember(member)
    }

    fun signIn(userId: String, password: String) : String {
        val user = getUserById(userId)
        if (!passwordEncoder.matches(password, user.passwordHash)) {
            throw UserNotAuthorizedException()
        }
        val refreshToken = refreshTokenGenerator()
        storage.addRefreshToken(userId, refreshToken)
        return refreshToken
    }

    fun signOut(user: UserInfo) {
        // accessToken-ы перестанут быть валидными быстро, а вот refreshToken-ы пользователя надо явно удалять
        storage.removeRefreshTokensByUserId(user.userId)
    }

    fun usersListById(userIdToFind: String) : List<UserInfo> {
        val requestedUser = storage.findUserById(userIdToFind)
        if (requestedUser != null) {
            return listOf(requestedUser)
        }
        return emptyList()
    }

    fun findUserById(userIdToFind: String) : UserInfo? {
        return storage.findUserById(userIdToFind)
    }

    fun usersListByName(namePattern : String? = null) : List<UserInfo> {
        return storage.findUsersByPartOfName(namePattern)
    }

    @Suppress("UNUSED_PARAMETER")
    fun usersDelete(userId: String) {
        // FIXME: Not implemented. Что делать с данными, ссылающимися на пользователя?
        // Надо очень аккуратно удалить все его токены, чаты и сообщения
        // Лучше не удалять, а ставить отметку о том, что пользователь удалён
        // или заменять на специального "удалённого пользователя"
    }

    fun chatsCreate(chatName: String, user: UserInfo) : ChatInfo {
        return doCreateChat(chatName, user)
    }

    private fun doCreateChat(chatName: String, userInfo: UserInfo): ChatInfo {
        val chatInfo = storage.addChat(NewChatInfo(chatName))
        val chatId = chatInfo.chatId
        storage.addChatSecret(chatId, generateChatSecret(chatId))
        val newMember = NewMemberInfo(chatId, chatName, userInfo.displayName, userInfo.userId)
        storage.addChatMember(newMember)
        return chatInfo
    }

    // FIXME: debug secrets for first chats
    private fun generateChatSecret(chatId: Int) =
        when (chatId) {
            2 -> "5cae1fec"
            3 -> "f74c73e0"
            else -> UUID.randomUUID().toString().substring(0..7)
        }

    fun usersInviteToChat(userIdToInvite: String, chatId: Int, user: UserInfo) {
        // Проверяем, что пользователь сам является участником чата
        checkUserIsMemberOfChat(chatId, user)

        val secret = storage.getChatSecret(chatId) ?: throw ChatNotFoundException()
        // Отправляем приглашение в виде системного сообщения, содержащего chatId и secret
        val text = "Пользователь ${user.displayName} (${user.userId}) приглашает вас в чат $chatId. Используйте пароль '$secret'"
        createSystemMessage(userIdToInvite, text)

    }

    private fun createSystemMessage(userIdToInvite: String, text: String) {
        val systemChat = getSystemChatId(userIdToInvite)
        val systemMember = storage.findMemberByChatIdAndUserId(systemChat, systemUser.userId)
                ?: throw InternalError("System user is not member of system chat?")
        doMessagesCreate(systemMember.memberId, text)
    }

    fun chatsJoin(chatId: Int, secret: String, user: UserInfo, chatName: String? = null) {
        if (storage.findMemberByChatIdAndUserId(chatId, user.userId) != null) {
            throw UserAlreadyMemberException()
        }
        val defaultChatName = storage.findChatById(chatId)?.defaultName ?: throw ChatNotFoundException()
        val realSecret = storage.getChatSecret(chatId) ?: throw ChatNotFoundException()
        if (realSecret != secret) {
            throw WrongChatSecretException()
        }
        val member = NewMemberInfo(
            chatId,
            chatName ?: defaultChatName,
            user.displayName,
            user.userId
        )
        storage.addChatMember(member)
    }

    fun chatsLeave(chatId: Int, user: UserInfo) {
        val member = checkUserIsMemberOfChat(chatId, user)
        // FIXME: Что будет с сообщениями от этого участника? - они просто пропадут
        // Лучше ввести флаг, показывающий является ли участник активным. Неактивный участник не видит чат у себя в списке чатов,
        // но при этом остальные участники продолжают видеть его старые сообщения
        storage.removeMember(member)
    }

    fun usersListChats(user: UserInfo) : List<ChatInfo> {
        val chatIds = storage.findChatIdsByUserId(user.userId)
        val result = mutableListOf<ChatInfo>()
        chatIds.forEach {
            val chat = storage.findChatById(it)
            if (chat != null) {
                result.add(chat)
            }
        }
        return result
    }

    fun chatsMembersList(chatId: Int, user: UserInfo) : List<MemberInfo> {
        checkUserIsMemberOfChat(chatId, user)
        return storage.findMembersByChatId(chatId)
    }

    fun chatMessagesCreate(chatId: Int, text: String, user: UserInfo) : MessageInfo {
        val member = checkUserIsMemberOfChat(chatId, user)
        return doMessagesCreate(member.memberId, text)
    }

    private fun doMessagesCreate(memberId: Int, text: String): MessageInfo {
        return storage.addMessage(NewMessageInfo(memberId, text))
    }

    fun chatMessagesList(chatId: Int, user: UserInfo, afterId: Int = 1) : List<MessageInfo> {
        if (afterId < 0) {
            throw IllegalArgumentException("afterId parameters must be > 0")
        }
        checkUserIsMemberOfChat(chatId, user)
        return storage.findMessages(chatId, afterId)
    }

    fun chatMessagesDeleteById(messageId: Int, user: UserInfo) {
        val message = storage.findMessageById(messageId)
        if (message != null) {
            val member = storage.findMemberById(message.memberId) ?: throw ChatNotFoundException()
            checkUserIsMemberOfChat(member.chatId, user)
            // FIXME: Что делать, если мы хотим показывать заглушки на месте удалённых сообщений?
            // Лучше удалять текст и помечать сообщение как удалённое
            storage.removeMessage(message)
        }
    }

    private fun checkUserIsMemberOfChat(chatId: Int, userInfo: UserInfo) =
            storage.findMemberByChatIdAndUserId(chatId, userInfo.userId) ?: throw UserNotMemberException()

    private fun getSystemChatId(userId: String) = storage.findCommonChatIds(userId, systemUser.userId).first()

    private fun getUserById(userId: String) = storage.findUserById(userId) ?: throw UserNotFoundException()

    fun getSystemUserId(): String {
        return systemUser.userId
    }

    fun getSystemUser(): UserInfo {
        return systemUser
    }

    fun invalidateRefreshToken(userId: String, refreshToken: String) {
        val tokenUserId = storage.getUserIdByRefreshToken(refreshToken)
        if (tokenUserId != userId) {
            throw UserNotAuthorizedException()
        }
        storage.removeRefreshToken(refreshToken)
    }

    fun replaceRefreshToken(userId: String, refreshToken: String): String {
        val tokenUserId = storage.getUserIdByRefreshToken(refreshToken)
        if (tokenUserId != userId) {
            throw UserNotAuthorizedException()
        }
        val newRefreshToken = refreshTokenGenerator()
        storage.addRefreshToken(userId, newRefreshToken)
        storage.removeRefreshToken(refreshToken)
        return newRefreshToken
    }
}

class UserNotMemberException : Exception()
class UserAlreadyMemberException : Exception()
class MessageAlreadyExistsException : Exception()
class ChatNotFoundException : Exception()
class WrongChatSecretException : Exception()
class SecretAlreadyExistsException : Exception()
class UserNotFoundException : Exception()
class UserNotAuthorizedException : Exception()
class UserAlreadyExistsException : Exception()
