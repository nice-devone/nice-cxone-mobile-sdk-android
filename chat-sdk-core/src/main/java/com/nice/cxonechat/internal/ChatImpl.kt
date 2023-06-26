package com.nice.cxonechat.internal

import com.nice.cxonechat.Cancellable
import com.nice.cxonechat.ChatActionHandler
import com.nice.cxonechat.ChatEventHandler
import com.nice.cxonechat.ChatFieldHandler
import com.nice.cxonechat.ChatThreadsHandler
import com.nice.cxonechat.internal.model.ConfigurationInternal
import com.nice.cxonechat.internal.model.network.ActionStoreVisitor
import com.nice.cxonechat.internal.socket.ProxyWebSocketListener
import com.nice.cxonechat.internal.socket.SocketFactory
import com.nice.cxonechat.internal.socket.WebSocketSpec
import com.nice.cxonechat.internal.socket.send
import com.nice.cxonechat.state.Connection
import com.nice.cxonechat.thread.CustomField
import okhttp3.WebSocket

internal class ChatImpl(
    override var connection: Connection,
    override val entrails: ChatEntrails,
    private val socketFactory: SocketFactory,
    override val configuration: ConfigurationInternal,
) : ChatWithParameters {

    override val socketListener: ProxyWebSocketListener = socketFactory.createProxyListener()
    override val socket: WebSocket
        get() = socketSession

    override var fields = listOf<CustomField>()
    override val environment get() = entrails.environment

    private val actions = ChatActionHandlerImpl(this)

    private var socketSession: WebSocket = socketFactory.create(socketListener)

    override fun setDeviceToken(token: String?) {
        val event = ActionStoreVisitor(
            connection = connection,
            visitor = storage.visitorId,
            deviceToken = token
        )

        socketSession.send(event)
    }

    override fun threads(): ChatThreadsHandler {
        var handler: ChatThreadsHandler
        handler = ChatThreadsHandlerImpl(this, configuration.preContactSurvey)
        handler = ChatThreadsHandlerReplayLastEmpty(handler)
        handler = ChatThreadsHandlerConfigProxy(handler, this)
        handler = ChatThreadsHandlerWelcome(handler, this)
        handler = ChatThreadsHandlerMessages(handler)
        return handler
    }

    override fun events(): ChatEventHandler {
        var handler: ChatEventHandler
        handler = ChatEventHandlerImpl(this)
        handler = ChatEventHandlerTokenGuard(handler, this)
        return handler
    }

    override fun customFields(): ChatFieldHandler = ChatFieldHandlerGlobal(this)

    override fun actions(): ChatActionHandler = actions

    override fun signOut() {
        storage.clearStorage()
        close()
    }

    override fun close() {
        socketSession.close(WebSocketSpec.CLOSE_NORMAL_CODE, null)
    }

    override fun reconnect(): Cancellable {
        socketSession = socketFactory.create(socketListener)
        return Cancellable.noop
    }
}
