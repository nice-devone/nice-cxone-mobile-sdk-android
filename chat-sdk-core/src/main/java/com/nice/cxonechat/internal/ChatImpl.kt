package com.nice.cxonechat.internal

import com.neovisionaries.ws.client.WebSocket
import com.nice.cxonechat.ChatActionHandler
import com.nice.cxonechat.ChatEventHandler
import com.nice.cxonechat.ChatFieldHandler
import com.nice.cxonechat.ChatThreadsHandler
import com.nice.cxonechat.internal.model.network.ActionStoreVisitor
import com.nice.cxonechat.socket.SendSelfRemovingCallback.Companion.send
import com.nice.cxonechat.state.Configuration
import com.nice.cxonechat.state.Connection
import com.nice.cxonechat.thread.CustomField

internal class ChatImpl(
    override var connection: Connection,
    override val entrails: ChatEntrails,
    override val socket: WebSocket,
    override val configuration: Configuration,
) : ChatWithParameters {

    override var fields = listOf<CustomField>()
    override val environment get() = entrails.environment

    private val actions = ChatActionHandlerImpl(this)

    override fun setDeviceToken(token: String?) {
        val event = ActionStoreVisitor(
            connection = connection,
            visitor = storage.visitorId,
            deviceToken = token
        )

        socket.send(event)
    }

    override fun threads(): ChatThreadsHandler {
        var handler: ChatThreadsHandler
        handler = ChatThreadsHandlerImpl(this)
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

    override fun customFields(): ChatFieldHandler {
        return ChatFieldHandlerGlobal(this)
    }

    override fun actions(): ChatActionHandler {
        return actions
    }

    override fun signOut() {
        storage.clearStorage()
        close()
    }

    override fun close() {
        socket.sendClose()
        socket.clearListeners()
        socket.disconnect()
    }

}
