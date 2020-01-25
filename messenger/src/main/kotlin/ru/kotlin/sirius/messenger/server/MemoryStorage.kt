package ru.kotlin.sirius.messenger.server

/**
 * Хранилище пользователей, чатов, сообщений и пр.
 */
class MemoryStorage : IStorage {

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

    private fun generateChatId(): Int {
        return nextChatId++
    }

    private fun generateMemberId(): Int {
        return nextMemberId++
    }

    private fun generateMessageId(): Int {
        return nextMessageId++
    }

    override fun containsUser(userId: String): Boolean {
        return users.any { it.userId == userId }
    }

    override fun addUser(info: UserInfo) {
        if (containsUser(info.userId)) {
            throw UserAlreadyExistsException()
        }
        users.add(info)
    }

    override fun findUserById(userId: String): UserInfo? {
        return users.firstOrNull{ it.userId == userId }
    }

    override fun addRefreshToken(userId: String, token: String) {
        refreshToken2userId[token] = userId
    }

    override fun getUserIdByRefreshToken(token: String) : String? {
        return refreshToken2userId[token]
    }

    override fun removeRefreshToken(token: String) {
        refreshToken2userId.remove(token)
    }

    override fun removeRefreshTokensByUserId(userId: String) {
        val pairsToRemove = refreshToken2userId.filterValues { it != userId }
        for (entry in pairsToRemove) {
            refreshToken2userId.remove(entry.key, entry.value)
        }
    }

    override fun findUsersByPartOfName(partOfName: String?): List<UserInfo> {
        return users.filter { partOfName == null || it.displayName.contains(partOfName) }
    }

    override fun addChat(newChatInfo: NewChatInfo) : ChatInfo {
        val chatInfo = ChatInfo(generateChatId(), newChatInfo.defaultName)
        chats.add(chatInfo)
        return chatInfo
    }

    override fun containsChat(chatId: Int): Boolean {
        return chats.any { it.chatId == chatId }
    }

    override fun findChatById(chatId: Int): ChatInfo? {
        return chats.firstOrNull { it.chatId == chatId }
    }

    override fun containsMember(chatId: Int, userId: String) : Boolean {
        return members.any { it.chatId == chatId && it.userId == userId}
    }

    override fun findMemberByChatIdAndUserId(chatId: Int, userId: String) : MemberInfo? {
        return members.firstOrNull { it.chatId == chatId && it.userId == userId }
    }

    override fun findMemberById(memberId: Int) : MemberInfo? {
        return members.firstOrNull { it.memberId == memberId }
    }

    override fun containsMember(memberId: Int) : Boolean {
        return members.any { it.memberId == memberId }
    }

    override fun addChatMember(info: NewMemberInfo): MemberInfo {
        if (containsMember(info.chatId, info.userId)) {
            throw UserAlreadyMemberException()
        }
        val result = MemberInfo(generateMemberId(), info.chatId, info.chatDisplayName, info.memberDisplayName, info.userId)
        members.add(result)
        return result
    }

    override fun addChatSecret(chatId: Int, secret: String) {
        if (chatId2secret[chatId] != null) {
            throw SecretAlreadyExistsException()
        }
        chatId2secret[chatId] = secret
    }

    override fun findChatIdsByUserId(userId: String) : List<Int> {
        return members.filter { it.userId == userId }.map { it.chatId }
    }

    override fun findMemberIdsByChatId(chatId: Int) : List<Int> {
        return findMembersByChatId(chatId).map { it.memberId }
    }

    override fun findMembersByChatId(chatId: Int): List<MemberInfo> {
        return members.filter { it.chatId == chatId }
    }

    override fun findCommonChatIds(userId1: String, userId2: String): List<Int> {
        val chatIds = findChatIdsByUserId(userId1)
        return chatIds.filter { containsMember(it, userId2) }
    }

    override fun getChatSecret(chatId: Int): String? {
        return chatId2secret[chatId]
    }

    override fun addMessage(info: NewMessageInfo): MessageInfo {
        val message = MessageInfo(generateMessageId(), info.memberId, info.text)
        messages.add(message)
        return message
    }

    override fun findMessages(chatId: Int, afterMessageId : Int) : List<MessageInfo> {
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

    override fun findMessageById(messageId: Int) : MessageInfo? {
        return messages.firstOrNull { it.messageId == messageId }
    }

    override fun removeMessage(messageInfo: MessageInfo) {
        messages.remove(messageInfo)
    }

    override fun removeMember(memberInfo: MemberInfo) {
        members.remove(memberInfo)
    }

}
