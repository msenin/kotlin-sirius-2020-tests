package ru.kotlin.sirius.messenger.server

/**
 * Created by Mikhail Senin (Mikhail.Senin@jetbrains.com) on 24.01.2020.
 */
interface IStorage {
    fun containsUser(userId: String): Boolean
    fun addUser(info: UserInfo)
    fun findUserById(userId: String): UserInfo?
    fun addRefreshToken(userId: String, token: String)
    fun getUserIdByRefreshToken(token: String) : String?
    fun removeRefreshToken(token: String)
    fun removeRefreshTokensByUserId(userId: String)
    fun findUsersByPartOfName(partOfName: String?): List<UserInfo>
    fun addChat(newChatInfo: NewChatInfo) : ChatInfo
    fun containsChat(chatId: Int): Boolean
    fun findChatById(chatId: Int): ChatInfo?
    fun containsMember(chatId: Int, userId: String) : Boolean
    fun findMemberByChatIdAndUserId(chatId: Int, userId: String) : MemberInfo?
    fun findMemberById(memberId: Int) : MemberInfo?
    fun containsMember(memberId: Int) : Boolean
    fun addChatMember(info: NewMemberInfo) : MemberInfo
    fun addChatSecret(chatId: Int, secret: String)
    fun findChatIdsByUserId(userId: String) : List<Int>
    fun findMemberIdsByChatId(chatId: Int) : List<Int>
    fun findMembersByChatId(chatId: Int): List<MemberInfo>
    fun findCommonChatIds(userId1: String, userId2: String): List<Int>
    fun getChatSecret(chatId: Int): String?
    fun addMessage(info: NewMessageInfo) : MessageInfo
    fun findMessages(chatId: Int, afterMessageId : Int = 0) : List<MessageInfo>
    fun findMessageById(messageId: Int) : MessageInfo?
    fun removeMessage(messageInfo: MessageInfo)
    fun removeMember(memberInfo: MemberInfo)
}