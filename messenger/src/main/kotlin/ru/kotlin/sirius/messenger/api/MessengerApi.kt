package ru.kotlin.sirius.messenger.api

import retrofit2.Call
import retrofit2.http.*

data class UserInfo(val userId: String, val displayName: String)
data class NewUserInfo(val userId: String, val displayName: String, val password: String)
data class PasswordInfo(val password: String)
data class AuthInfo(val accessToken: String, val refreshToken: String) {
    val accessTokenHeader = "Bearer $accessToken"
}
data class ChatInfo(val chatId: Int, val defaultName: String)
data class NewChatInfo(val defaultName: String)
data class JoinChatInfo(val defaultName: String?, val secret: String)
data class InviteChatInfo(val userId: String)
data class MessageInfo (val messageId: Int, val memberId: Int, var text: String, val createdOn: Long)

data class NewMessageInfo (var text: String)
data class MemberInfo(val memberId: Int, val chatId: Int, val chatDisplayName: String, val memberDisplayName: String, val userId: String)
data class RefreshTokenInfo(val refreshToken: String)

interface MessengerApi {

    @POST("/v1/users")
    fun registerUser(@Body newUserInfo: NewUserInfo): Call<UserInfo>

    @POST("/v1/users/{userId}/signin")
    fun signIn(@Path("userId") userId: String, @Body password: PasswordInfo): Call<AuthInfo>

    @POST("/v1/me/signout")
    fun signOut(@Header("Authorization") accessTokenHeader: String): Call<Unit>

    @POST("/v1/chats")
    fun createChat(@Body newChatInfo: NewChatInfo, @Header("Authorization") accessTokenHeader: String): Call<ChatInfo>

    @POST("/v1/chats/{chatId}/invite")
    fun inviteToChat(@Path("chatId") chatId: Int, @Body inviteInfo: InviteChatInfo, @Header("Authorization") accessTokenHeader: String): Call<Map<String,String>>

    @POST("/v1/chats/{chatId}/join")
    fun joinToChat(@Path("chatId") chatId: Int, @Body joinInfo: JoinChatInfo, @Header("Authorization") accessTokenHeader: String): Call<Map<String,String>>

    @GET("/v1/me/chats")
    fun listChats(@Header("Authorization") accessTokenHeader: String): Call<List<ChatInfo>>

    @GET("/v1/chats/{chatId}/members")
    fun listChatMembers(@Path("chatId") chatId: Int, @Header("Authorization") accessTokenHeader: String): Call<List<MemberInfo>>

    @POST("/v1/chats/{chatId}/messages/")
    fun sendMessage(@Path("chatId") chatId: Int, @Body message: NewMessageInfo, @Header("Authorization") accessTokenHeader: String): Call<MessageInfo>

    @GET("/v1/chats/{chatId}/messages/")
    fun listMessages(@Path("chatId") chatId: Int, @Header("Authorization") accessTokenHeader: String, @Query("after_id") afterId: Int = 0): Call<List<MessageInfo>>

    @GET("/v1/users")
    fun findUsersByPartOfName(@Header("Authorization") accessTokenHeader: String, @Query("name") name: String): Call<List<UserInfo>>

    @GET("/v1/users/{userId}")
    fun getUserByUserId(@Path("userId") userId: String, @Header("Authorization") accessTokenHeader: String): Call<UserInfo?>

    @GET("/v1/admin")
    fun getSystemUser(@Header("Authorization") accessTokenHeader: String): Call<UserInfo>

    @POST("/v1/me/invalidate")
    fun invalidateRefreshToken(@Header("Authorization") accessTokenHeader: String, @Body refreshToken: RefreshTokenInfo): Call<Unit>

    @POST("/v1/me/refresh")
    fun refreshAccessToken(@Header("Authorization") accessTokenHeader: String, @Body refreshToken: RefreshTokenInfo): Call<AuthInfo>
}