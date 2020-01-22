package ru.kotlin.sirius.messenger.client

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.fail
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Disabled
import ru.kotlin.sirius.messenger.server.MessengerServer
import ru.kotlin.sirius.messenger.server.WrongChatSecretException

class MessengerTest {

    private val server = MessengerServer()
    private val client1 = MessengerClient(server)
    private val client2 = MessengerClient(server)

    private val login1 = "user1"
    private val password1 = "password"
    private val name1 = "User 1"

    private val login2 = "user2"
    private val password2 = "123456"
    private val name2 = "User 2"

    @BeforeEach
    fun resetStorage() {
        MessengerServer.storage.clear()
    }

    @Test
    fun testMessageSending() {

        // Регистрируем двух пользователей
        client1.register(login1, name1, password1)
        client2.register(login2, name2, password2)

        // Авторизуемся под первым пользователем и проверяем правильность информации о нём
        val user1 = client1.signIn(login1, password1)
        assertEquals(name1, user1.name)

        // Авторизуемся под вторым пользователем и проверяем правильность информации о нём
        val user2 = client2.signIn(login2, password2)
        assertEquals(name2, user2.name)

        // Первый пользователь создаёт чат и приглашает в него второго пользователя
        val user1chat = user1.createChat(name2)
        user1chat.inviteUser(user2.userId)

        // Обновляем данные системного чата второго пользователя
        user2.systemChat.refresh()

        // Убеждаемся, что пришло приглашение в системный чат
        val template = """Пользователь (.+) \(.+\) приглашает вас в чат (\d+). Используйте пароль '(.+)'""".toRegex()
        val matchResult = template.find(user2.systemChat.messages.last().text) ?: fail("Нет приглашения от первого пользователя в системном чате")

        println(user2.systemChat.messages.joinToString("\n", "=== ${user2.name}, чат c ${user2.systemChat.name} ===\n", "\n"))

        // Второй пользователь присоединяется к чату
        val chatName = matchResult.groupValues[1]
        val chatId = matchResult.groupValues[2].toInt()
        val chatSecret = matchResult.groupValues[3]

        // Проверим, что нельзя присоединиться к чату с неверным паролем
        assertThrows(WrongChatSecretException::class.java, {
            user2.joinToChat(chatId, "wrongSecret", chatName)
        }, "Нет исключения WrongChatSecretException при использовании неверного пароля чата")

        // Пробуем присоединиться к чату с правильным паролем
        user2.joinToChat(chatId, chatSecret, chatName)
        val user2chat = user2.chats.firstOrNull { it.chatId == chatId } ?: fail("После присоединения чат отсутствует у второго пользователя")

        // Отправляем сообщение от первого пользователя
        user1chat.sendMessage("Ping!")

        // Обновляем данные чата со стороны второго пользователя
        user2chat.refresh()

        // Читаем сообщение от второго пользователя
        assertEquals("Ping!", user2chat.messages.last().text, "Не доставлено сообщение от первого пользователя")

        // Отправляем сообщение от второго пользователя
        user2chat.sendMessage("Pong!")

        // Обновляем данные чата со стороны первого пользователя
        user1chat.refresh()

        // Читаем сообщение от первого пользователя
        assertEquals("Pong!", user1chat.messages.last().text, "Не доставлено сообщение от второго пользователя")

        println(user1chat.messages.joinToString("\n", "=== ${user1.name}, чат c ${user1chat.name} ===\n", "\n"))
        println(user2chat.messages.joinToString("\n", "=== ${user2.name}, чат c ${user2chat.name} ===\n", "\n"))

        user1.signOut()
        user2.signOut()
    }


    @Disabled
    @Test
    fun testRefreshSpeed() {
        client1.register(login1, name1, password1)
        client2.register(login2, name2, password2)
        val user1 = client1.signIn(login1, password1)
        val user2 = client2.signIn(login2, password2)

        val user1chat = user1.createChat(name2)
        user1chat.inviteUser(user2.userId)

        val user2chat = user2.processLastInvitationAndJoinChat()

        val startTime = System.currentTimeMillis()

        var attempt = 1
        while (attempt <= 10000) {
            user1chat.sendMessage("Request $attempt")
            user2chat.refresh()
            assertEquals("Request $attempt", user2chat.messages.last().text, "Не доставлено сообщение от первого пользователя")
            user2chat.sendMessage("Response $attempt")
            user1chat.refresh()
            assertEquals("Response $attempt", user1chat.messages.last().text, "Не доставлено сообщение от второго пользователя")
            attempt++
        }
        val finishTime = System.currentTimeMillis()

        println("Время выполнения (мс): ${finishTime - startTime}")
        user1.signOut()
        user2.signOut()
    }

    private fun User.processLastInvitationAndJoinChat() : Chat {
        this.systemChat.refresh()
        val template = """Пользователь (.+) \(.+\) приглашает вас в чат (\d+). Используйте пароль '(.+)'""".toRegex()
        val matchResult = template.find(this.systemChat.messages.last().text)
                ?: fail("Последнее сообщение не является приглашением")
        val chatName = matchResult.groupValues[1]
        val chatId = matchResult.groupValues[2].toInt()
        val chatSecret = matchResult.groupValues[3]
        this.joinToChat(chatId, chatSecret, chatName)
        return this.chats.firstOrNull { it.chatId == chatId }
                ?: fail("После присоединения чат отсутствует у пользователя")
    }

}