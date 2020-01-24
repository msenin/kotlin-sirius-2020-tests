package ru.kotlin.sirius.messenger.client

/**
 * Чат
 */
class Chat(val chatId: Int, user: User) : UserAware(user) {
    lateinit var name: String
    val messages = mutableListOf<Message>()
    val members = mutableListOf<Member>()
    lateinit var userMember: Member

    init {
        refresh()
    }

    fun inviteUser(userIdToInvite: String) {
        client.usersInviteToChat(userIdToInvite, chatId, user.authInfo)
    }

    fun sendMessage(text: String) : Message {
        val info = client.chatMessagesCreate(chatId, text, user.authInfo)
        val newMessage = Message(
            info.messageId,
            userMember,
            info.text,
            info.createdOn,
            this
        )
        messages.add(newMessage)
        return newMessage
    }

    fun refresh() {
        val membersInfo = client.chatsMembersList(chatId, user.authInfo)
        name = membersInfo.first { it.userId == user.userId }.chatDisplayName
        // FIXME: эффективнее определять разницу между membersInfo и members и добавлять/удалять только изменившиеся элементы
        members.clear()
        members.addAll( membersInfo.map {
            Member(
                it.memberId,
                it.memberDisplayName,
                it.userId,
                this
            )
        })
        userMember = members.first { it.memberUserId == user.userId }
        refreshMessages()
    }

    private fun refreshMessages() {
        val messagesInfo = client.chatMessagesList(chatId, user.authInfo)
        // FIXME: эффективнее определять разницу между messagesInfo и messages и добавлять/удалять только изменившиеся элементы
        messages.clear()
        messages.addAll(messagesInfo.map { info ->
            val member = members.first { it.memberId == info.memberId }
            Message(
                info.messageId,
                member,
                info.text,
                info.createdOn,
                this
            )
        })
    }
}