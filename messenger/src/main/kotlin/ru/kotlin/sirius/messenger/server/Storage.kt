package ru.kotlin.sirius.messenger.server

/**
 * Хранилище пользователей, чатов, сообщений и пр.
 */
class Storage {

    // FIXME: эффективнее было бы иметь структуры для быстрого поиска элементов по их id
    private val users =  mutableListOf<UserInfo>()
    private val chats = mutableListOf<ChatInfo>()
    private val members = mutableListOf<MemberInfo>()
    private val messages = mutableListOf<MessageInfo>()

    private val chatId2secret = mutableMapOf<Int, String>()
    private val refreshToken2userId = mutableMapOf<String, String>()

    internal fun clear() {
        users.clear()
        refreshToken2userId.clear()
        chats.clear()
        members.clear()
        messages.clear()
    }

    companion object {
        var nextChatId = 0
        var nextMemberId = 0
        var nextMessageId = 0
    }

    fun generateChatId(): Int {
        return nextChatId++
    }

    fun generateMemberId(): Int {
        return nextMemberId++
    }

    fun generateMessageId(): Int {
        return nextMessageId++
    }

    fun containsUser(userId: String): Boolean {
        return users.any { it.userId == userId }
    }

    fun addUser(newUserInfo: UserInfo) {
        if (containsUser(newUserInfo.userId)) {
            throw UserAlreadyExistsException()
        }
        users.add(newUserInfo)
    }

    fun findUserById(userId: String): UserInfo? {
        return users.firstOrNull{ it.userId == userId }
    }

    fun addRefreshToken(userId: String, token: String) {
        refreshToken2userId[token] = userId
    }

    fun getUserIdByRefreshToken(token: String) : String? {
        return refreshToken2userId[token]
    }

    fun removeRefreshToken(token: String) {
        refreshToken2userId.remove(token)
    }

    fun removeRefreshTokensByUserId(userId: String) {
        val pairsToRemove = refreshToken2userId.filterValues { it != userId }
        for (entry in pairsToRemove) {
            refreshToken2userId.remove(entry.key, entry.value)
        }
    }

    fun findUsersByPartOfName(partOfName: String?): List<UserInfo> {
        return users.filter { partOfName == null || it.displayName.contains(partOfName) }
    }

    fun addChat(newChatInfo: ChatInfo) {
        chats.add(newChatInfo)
    }

    fun containsChat(chatId: Int): Boolean {
        return chats.any { it.chatId == chatId }
    }

    fun findChatById(chatId: Int): ChatInfo? {
        return chats.firstOrNull { it.chatId == chatId }
    }

    fun containsMember(chatId: Int, userId: String) : Boolean {
        return members.any { it.chatId == chatId && it.userId == userId}
    }

    fun findMemberByChatIdAndUserId(chatId: Int, userId: String) : MemberInfo? {
        return members.firstOrNull { it.chatId == chatId && it.userId == userId }
    }

    fun findMemberById(memberId: Int) : MemberInfo? {
        return members.firstOrNull { it.memberId == memberId }
    }

    fun containsMember(memberId: Int) : Boolean {
        return members.any { it.memberId == memberId }
    }

    fun addChatMember(newMemberInfo: MemberInfo) {
        if (containsMember(newMemberInfo.chatId, newMemberInfo.userId)) {
            throw UserAlreadyMemberException()
        }
        members.add(newMemberInfo)
    }

    fun addChatSecret(chatId: Int, secret: String) {
        if (chatId2secret[chatId] != null) {
            throw SecretAlreadyExistsException()
        }
        chatId2secret[chatId] = secret
    }

    fun findChatIdsByUserId(userId: String) : List<Int> {
        return members.filter { it.userId == userId }.map { it.chatId }
    }

    private fun findMemberIdsByChatId(chatId: Int) : List<Int> {
        return findMembersByChatId(chatId).map { it.memberId }
    }

    fun findMembersByChatId(chatId: Int): List<MemberInfo> {
        return members.filter { it.chatId == chatId }
    }

    fun findCommonChatIds(userId1: String, userId2: String): List<Int> {
        val chatIds = findChatIdsByUserId(userId1)
        return chatIds.filter { containsMember(it, userId2) }
    }

    fun getChatSecret(chatId: Int): String? {
        return chatId2secret[chatId]
    }

    fun addMessage(messageInfo: MessageInfo) {
        if (messages.any { it.messageId == messageInfo.messageId }) {
            throw MessageAlreadyExistsException()
        }
        messages.add(messageInfo)
    }

    fun findMessages(chatId: Int, afterMessageId : Int = 0) : List<MessageInfo> {
        val chatMembers = findMemberIdsByChatId(chatId)
        val createdAfter = if (afterMessageId > 0) {
            messages.firstOrNull { it.messageId == afterMessageId }?.createdOn
        }
        else {
            null
        }
        return messages
                .filter{ it.memberId in chatMembers && (createdAfter == null || it.createdOn >= createdAfter) }
                .sortedBy { it.createdOn }
    }

    fun findMessageById(messageId: Int) : MessageInfo? {
        return messages.firstOrNull { it.messageId == messageId }
    }

    fun removeMessage(messageInfo: MessageInfo) {
        messages.remove(messageInfo)
    }

    fun removeMember(memberInfo: MemberInfo) {
        members.remove(memberInfo)
    }

}
