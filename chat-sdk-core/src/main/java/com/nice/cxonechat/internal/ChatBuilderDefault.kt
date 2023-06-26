package com.nice.cxonechat.internal

import com.nice.cxonechat.Authorization
import com.nice.cxonechat.Cancellable
import com.nice.cxonechat.ChatBuilder
import com.nice.cxonechat.ChatBuilder.OnChatBuiltCallback
import com.nice.cxonechat.ChatStateListener
import com.nice.cxonechat.ChatThreadingImpl
import com.nice.cxonechat.internal.copy.ConnectionCopyable.Companion.asCopyable
import com.nice.cxonechat.internal.socket.SocketFactory
import com.nice.cxonechat.internal.socket.StateReportingSocketFactory
import java.io.IOException

internal class ChatBuilderDefault(
    private val entrails: ChatEntrails,
    private val factory: SocketFactory,
) : ChatBuilder {

    private var isDevelopment: Boolean = false
    private var authorization: Authorization = Authorization.None
    private var firstName: String? = null
    private var lastName: String? = null
    private var chatStateListener: ChatStateListener? = null

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

    override fun setChatStateListener(listener: ChatStateListener): ChatBuilder = apply {
        chatStateListener = listener
    }

    @Throws(IllegalStateException::class, IOException::class, RuntimeException::class)
    override fun build(callback: OnChatBuiltCallback): Cancellable {
        val socketFactory = chatStateListener?.let { StateReportingSocketFactory(it, factory) } ?: factory
        var connection = socketFactory.getConfiguration(entrails.storage)
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
        chat = ChatImpl(connection, entrails, socketFactory, body.toConfiguration(connection.channelId))
        chat = ChatAuthorization(chat, authorization)
        chat = ChatStoreVisitor(chat)
        chat = ChatWelcomeMessageUpdate(chat)
        chat = ChatThreadingImpl(chat)
        if (isDevelopment) chat = ChatLogging(chat, entrails.logger)
        callback.onChatBuilt(chat)
        return Cancellable.noop
    }
}
