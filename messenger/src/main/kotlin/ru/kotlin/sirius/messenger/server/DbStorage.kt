package ru.kotlin.sirius.messenger.server

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

object Constants {
    const val nameLength = 100
    const val messageLength = 1024
}

object Users : Table(){
    val userId: Column<String> = varchar("userId", Constants.nameLength).primaryKey()
    val displayName: Column<String> = varchar("displayName", Constants.nameLength)
    val passwordHash: Column<String> = varchar("passwordHash", Constants.nameLength)
}

object Chats : Table(){
    val chatId: Column<Int> = integer("chatId").autoIncrement().primaryKey()
    val defaultName: Column<String> = varchar("defaultName", Constants.nameLength)
}

object Members : Table(){
    val memberId: Column<Int> = integer("memberId").autoIncrement().primaryKey()
    val chatId: Column<Int> = integer("chatId").references(Chats.chatId)
    val chatDisplayName: Column<String> = varchar("chatDisplayName", Constants.nameLength)
    val memberDisplayName: Column<String> = varchar("memberDisplayName", Constants.nameLength)
    val userId: Column<String> = varchar("userId", Constants.nameLength).references(Users.userId)
}

object  Messages : Table(){
    val messageId: Column<Int> = integer("messageId").autoIncrement().primaryKey()
    val memberId: Column<Int> = integer("memberId").references(Members.memberId)
    val text: Column<String> = varchar("text", Constants.messageLength)
    val createdOn: Column<Long> = long("createdOn")
}

object ChatId2secret : Table(){
    val chatId: Column<Int> = integer("chatId").primaryKey().references(Chats.chatId)
    val secret: Column <String> = varchar("secret", Constants.nameLength)
}

object RefreshToken2userId : Table(){
    val token: Column<String> = varchar("token", Constants.nameLength).primaryKey()
    val userId: Column <String> = varchar("userId", Constants.nameLength).references(Users.userId)
}

class DbStorage(databaseUrl: String, driver:String) : IStorage {
    private val connection:Database = Database.connect(databaseUrl, driver)
    init {
        transaction(connection) {
            SchemaUtils.createMissingTablesAndColumns(
                Users,
                Chats,
                Members,
                Messages,
                Messages,
                ChatId2secret,
                RefreshToken2userId
            )
        }
    }
    override fun containsUser(userId: String): Boolean {
        return transaction(connection) { Users.select{Users.userId eq userId}.count()} > 0
    }

    override fun addUser(info: UserInfo) {
        if (containsUser(info.userId)) {
            throw UserAlreadyExistsException()
        }
        transaction(connection){
            Users.insert{
                it[userId] = info.userId
                it[displayName] = info.displayName
                it[passwordHash] = info.passwordHash
            }
        }
    }

    override fun findUserById(userId: String): UserInfo? {
        var userInfo: UserInfo?
        userInfo = null
        transaction(connection) {
            Users.select { Users.userId eq userId }.forEach {
                userInfo = UserInfo(it[Users.userId], it[Users.displayName], it[Users.passwordHash])
            }
        }
        return userInfo
    }

    override fun findUsersByPartOfName(partOfName: String?): List<UserInfo> {
        return transaction(connection) {
            Users.select { Users.displayName like "%$partOfName%" }.map {
                UserInfo(it[Users.userId], it[Users.displayName], it[Users.passwordHash])
            }
        }
    }

    override fun addChat(newChatInfo: NewChatInfo) : ChatInfo {
        val chatId = transaction(connection){
            Chats.insert{
                it[defaultName]= newChatInfo.defaultName
            } get Chats.chatId
        }
        return ChatInfo(chatId, newChatInfo.defaultName)
    }

    override fun containsChat(chatId: Int): Boolean {
        return transaction(connection) { Chats.select{Chats.chatId eq chatId}.count() } > 0
    }

    override fun findChatById(chatId: Int): ChatInfo? {
        var chatInfo :ChatInfo? = null
        transaction(connection) {
            Chats.select { Chats.chatId eq chatId }.forEach {
                chatInfo = ChatInfo(it[Chats.chatId], it[Chats.defaultName])
            }
        }
        return chatInfo
    }

    override fun containsMember(chatId: Int, userId: String): Boolean {
        return transaction(connection) { Members.select{(Members.chatId eq chatId) and (Members.userId eq userId)}.count() } > 0
    }

    override fun containsMember(memberId: Int): Boolean {
        return transaction(connection) { Members.select{(Members.memberId eq memberId) }.count() } > 0
    }

    override fun findMemberByChatIdAndUserId(chatId: Int, userId: String): MemberInfo? {
        var memberInfo : MemberInfo? = null
        transaction(connection) {
            Members.select { (Members.chatId eq chatId) and (Members.userId eq userId) }.forEach {
                memberInfo = MemberInfo(it[Members.memberId],
                    it[Members.chatId],
                    it[Members.chatDisplayName],
                    it[Members.memberDisplayName],
                    it[Members.userId])
            }
        }
        return memberInfo
    }

    override fun findMemberById(memberId: Int): MemberInfo? {
        var memberInfo :MemberInfo? = null
        transaction(connection) {
            Members.select { (Members.memberId eq memberId) }.forEach {
                memberInfo = MemberInfo(it[Members.memberId],
                    it[Members.chatId],
                    it[Members.chatDisplayName],
                    it[Members.memberDisplayName],
                    it[Members.userId])
            }
        }
        return memberInfo
    }

    override fun addChatMember(info: NewMemberInfo): MemberInfo {
        if (containsMember(info.chatId, info.userId)) {
            throw UserAlreadyMemberException()
        }
        val memberId = transaction(connection) {
            Members.insert {
                it[chatDisplayName] = info.chatDisplayName
                it[memberDisplayName] = info.memberDisplayName
                it[chatId] = info.chatId
                it[userId] = info.userId
            }
        } get Members.memberId
        return MemberInfo(memberId, info.chatId, info.chatDisplayName, info.memberDisplayName, info.userId)
    }

    override fun addChatSecret(chatId: Int, secret: String) {
        if (transaction(connection) { ChatId2secret.select{ChatId2secret.chatId eq chatId}.count() } > 0) {
            throw SecretAlreadyExistsException()
        }
        transaction(connection) {
            ChatId2secret.insert {
                it[ChatId2secret.chatId] = chatId
                it[ChatId2secret.secret] = secret
            }
        }
    }

    override fun findChatIdsByUserId(userId: String): List<Int> {
        return transaction(connection) {
            Members.select { Members.userId eq userId }.map {
                it[Members.chatId]
            }
        }
    }

    override fun findMemberIdsByChatId(chatId: Int): List<Int> {
        return transaction(connection) {
            Members.select { Members.chatId eq chatId }.map {
                it[Members.memberId]
            }
        }
    }

    override fun findMembersByChatId(chatId: Int): List<MemberInfo> {
        return transaction(connection) {
            Members.select { Members.chatId eq chatId }.map {
                MemberInfo(it[Members.memberId],
                    it[Members.chatId],
                    it[Members.chatDisplayName],
                    it[Members.memberDisplayName],
                    it[Members.userId])
            }
        }
    }

    override fun findCommonChatIds(userId1: String, userId2: String): List<Int> {
        val chatIds = findChatIdsByUserId(userId1)
        return chatIds.filter { containsMember(it, userId2) }
    }

    override fun getChatSecret(chatId: Int): String? {
        var secret:String? = null
        transaction(connection) {
            ChatId2secret.select{ChatId2secret.chatId eq chatId}.forEach {
                secret = it[ChatId2secret.secret]
            }
        }
        return secret
    }

    override fun addMessage(info: NewMessageInfo): MessageInfo {
        val timestamp = System.currentTimeMillis()
        val messageId = transaction(connection) {
            Messages.insert {
                it[createdOn] = timestamp
                it[memberId] = info.memberId
                it[text] = info.text
            } get Messages.messageId
        }
        return MessageInfo(messageId, info.memberId, info.text, timestamp)
    }

    override fun findMessages(chatId: Int, afterMessageId: Int): List<MessageInfo> {
        val chatMembers = findMemberIdsByChatId(chatId)
        val createdAfter:Long = if (afterMessageId > 0) {
            var messageInfo: Long = -1
            transaction(connection) { Messages.select { Messages.messageId eq afterMessageId}.map{it[Messages.createdOn]} }.forEach {
                messageInfo = it
            }
            messageInfo
        }
        else {
            -1
        }
        return transaction(connection) { Messages.select{(Messages.memberId inList chatMembers) and (Messages.createdOn greaterEq createdAfter )}
            .orderBy(Messages.createdOn, SortOrder.ASC).map{
                MessageInfo(it[Messages.messageId],
                    it[Messages.memberId],
                    it[Messages.text],
                    it[Messages.createdOn]
                )
            }
        }
    }

    override fun findMessageById(messageId: Int): MessageInfo? {
        var messageInfo :MessageInfo? = null
        transaction(connection) {
            Messages.select { Messages.messageId eq messageId }.forEach {
                messageInfo = MessageInfo(
                    it[Messages.messageId],
                    it[Messages.memberId],
                    it[Messages.text]
                )
            }
        }
        return messageInfo
    }

    override fun removeMessage(messageInfo: MessageInfo) {
        transaction(connection) {
            Messages.deleteWhere { Messages.messageId eq messageInfo.messageId }
        }
    }

    override fun removeMember(memberInfo: MemberInfo) {
        transaction(connection) {
            Members.deleteWhere { Members.memberId eq memberInfo.memberId }
        }
    }

    override fun addRefreshToken(userId: String, token: String) {
        transaction(connection) {
            RefreshToken2userId.deleteWhere { RefreshToken2userId.token eq token }
            RefreshToken2userId.insert{
                it[RefreshToken2userId.token] = token
                it[RefreshToken2userId.userId] = userId
            }
        }
    }

    override fun getUserIdByRefreshToken(token: String): String? {
        var userId: String? = null
        transaction(connection) {
            RefreshToken2userId.select{RefreshToken2userId.token eq token}.forEach {
                userId = it[RefreshToken2userId.userId]
            }
        }
        return userId
    }

    override fun removeRefreshToken(token: String) {
        transaction(connection) {
            RefreshToken2userId.deleteWhere { RefreshToken2userId.token eq token }
        }
    }

    override fun removeRefreshTokensByUserId(userId: String) {
        transaction(connection) {
            RefreshToken2userId.deleteWhere { RefreshToken2userId.userId eq userId }
        }
    }
}