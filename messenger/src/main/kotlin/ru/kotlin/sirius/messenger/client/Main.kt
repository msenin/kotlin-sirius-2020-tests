package ru.kotlin.sirius.messenger.client

import kotlin.system.exitProcess

fun prompt(specialPrompt : String? = null) : String {

    val prompt = if (specialPrompt == null) {
        var standartPrompt = user?.let { "[$user]@ " } ?: "[user not signed in]@"
        standartPrompt += chat?.let { "${it.name} [${it.chatId}]" } ?: "[chat not selected]"
        standartPrompt += " > "
        standartPrompt
    }
    else {
        specialPrompt
    }
    print(prompt)
    var value: String? = readLine()
    while (value == null) {
        value = readLine()
    }
    return value
}

var user : User? = null
var chat : Chat? = null
var messengerBaseUrl = "http://127.0.0.1:9999/"

fun main(args: Array<String>) {
    if (args.isNotEmpty()) {
        messengerBaseUrl = args[0]
    }
    val client = MessengerClient(messengerBaseUrl)

    fun register() {
        val login = prompt("Login: ")
        val name = prompt("Name: ")
        val password = prompt("Password: ")
        try {
            client.register(login, name, password)
            user = client.signIn(login, password)
            user?.apply { chat = chats[0] }
        }
        catch (e: Exception) {
            println("Error! ${e.message}")
        }
    }

    fun signIn()  {
        val login = prompt("Login: ")
        val password = prompt("Password: ")
        try {
            user = client.signIn(login, password)
            user?.apply { chat = chats[0] }
        }
        catch (e: Exception) {
            println("Error! ${e.message}")
        }
    }

    fun signOut() {
        user?.apply {
            signOut()
            chat = null
            user = null
        }
    }

    fun exit() {
        exitProcess(0)
    }

    fun authInfo() {
        println(user?.authInfo)
    }

    fun createChat() {
        val chatName = prompt("Chat name: ")
        try {
            val newChat = user?.createChat(chatName)
            newChat?.let { chat = newChat }
        }
        catch (e: Exception) {
            println("Error! ${e.message}")
        }
    }

    fun chats() {
        user?.apply {
            refresh()
            chats.forEach { println("${it.name} [ ${it.chatId} ]: ${it.messages.size} messages") }
        } ?: println("User not signed in")
    }

    fun selectChat() {
        val chatId = prompt("Chat id: ")
        user?.let {
            chat = it.chats.firstOrNull { chat -> chat.chatId == chatId.toInt() }
            if (chat == null) {
                println("Chat with id $chatId not found")
            }
            else {
                chat?.apply { messages.forEach { message -> println(message) } }
            }
        } ?: println("User not signed in")
    }

    fun checkChat() {
        chat?.apply {
            refresh()
            messages.forEach { message -> println(message) }
        } ?: println("Chat not selected")
    }

    fun message(text: String) {
        chat?.sendMessage(text) ?: println("Chat not selected")
        chat?.refresh()
    }

    fun members() {
        chat?.members?.forEach { println(it) } ?: println("Chat not selected")
    }

    fun invite() {
        val person = prompt("User login: ")
        chat?.inviteUser(person) ?:  println("Chat not selected")
    }

    fun join() {
        val chatId = prompt("Chat Id: ")
        val password = prompt("Password for join: ")
        val name = prompt("Name for this chat: ")
        user?.joinToChat(chatId.toInt(), password, name) ?:  println("Chat not selected")
    }

    fun chatInfo() {
        chat?.apply { println(chatId) } ?:  println("Chat not selected")
    }

    fun refresh() {
        user?.refresh() ?: println("User not signed in")
    }

    fun help() {
        println("""
            Type command and then type argument on new line when it's needed. Here are all commands:
              :help - shows list of commands
              :register - register new user
              :signIn - sign in
              :signOut - sign out
              :authInfo - shows your authorisation information
              :createChat {chat name} - create new chat with given name
              :listChats - shows list of your chat
              :chat - change current chat to chat with given ID
              :check - shows messages from current chat
              :list members - shows list of members
              :invite - invite user with given ID to current chat 
              :join - join to chat with given ID
              :chatInfo - shows chat ID
              :refresh - refreshes all and everything
              :exit - exit
              {text} - write message to current chat 
            """.trimIndent()
        )
    }

    var input: String

    println("""
                 Welcome Sirius 2020 chat system!
                 $messengerBaseUrl
                 
                 Use :help command if needed
                 
            """.trimIndent())
    while (true) {
        input = prompt()
        when (input.trim()) {
            ":help" -> help()
            ":register" -> register()
            ":signIn" -> signIn()
            ":signOut" -> signOut()
            ":authInfo" -> authInfo()
            ":createChat" -> createChat()
            ":listChats" -> chats()
            ":chat" -> selectChat()
            ":check" -> checkChat()
            ":listMembers" -> members()
            ":invite" -> invite()
            ":join" -> join()
            ":chatInfo" -> chatInfo()
            ":refresh" -> refresh()
            ":exit" -> exit()
            "" -> {}
            else -> message(input)
        }
        println()
    }
}
