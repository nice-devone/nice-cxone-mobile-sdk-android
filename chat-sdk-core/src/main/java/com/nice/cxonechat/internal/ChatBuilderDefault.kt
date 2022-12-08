package com.nice.cxonechat.internal

import com.nice.cxonechat.Authorization
import com.nice.cxonechat.Cancellable
import com.nice.cxonechat.ChatBuilder
import com.nice.cxonechat.ChatBuilder.OnChatBuiltCallback
import com.nice.cxonechat.internal.copy.ConnectionCopyable.Companion.asCopyable
import java.io.IOException

internal class ChatBuilderDefault(
    private val entrails: ChatEntrails,
    private val factory: SocketFactory,
) : ChatBuilder {

    private var isDevelopment: Boolean = false
    private var authorization: Authorization = Authorization.None
    private var firstName: String? = null
    private var lastName: String? = null

    override fun setAuthorization(authorization: Authorization) = apply {
        this.authorization = authorization
    }

    override fun setDevelopmentMode(enabled: Boolean) = apply {
        this.isDevelopment = enabled
    }

    override fun setUserName(first: String, last: String) = apply {
        this.firstName = first
        this.lastName = last
    }

    @Throws(IllegalStateException::class, IOException::class, RuntimeException::class)
    override fun build(callback: OnChatBuiltCallback): Cancellable {
        val socket = factory.create()
        var connection = factory.getConfiguration(entrails.storage)
        val firstName = firstName
        val lastName = lastName
        if (firstName != null && lastName != null) {
            connection = connection.asCopyable().copy(
                firstName = firstName,
                lastName = lastName,
            )
        }
        val response = entrails.service.getChannel(connection.brandId.toString(), connection.channelId).execute()
        check(response.isSuccessful) { "Response from the server was not successful" }
        val body = checkNotNull(response.body()) { "Response body was null" }
        var chat: ChatWithParameters
        chat = ChatImpl(connection, entrails, socket, body.toConfiguration())
        chat = ChatAuthorization(chat, authorization)
        chat = ChatWelcomeMessageUpdate(chat)
        if (isDevelopment) chat = ChatLogging(chat, entrails.logger)
        callback.onChatBuilt(chat)
        return Cancellable.noop
    }
}
