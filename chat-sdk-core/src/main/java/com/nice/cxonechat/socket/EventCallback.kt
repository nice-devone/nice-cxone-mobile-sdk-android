package com.nice.cxonechat.socket

import com.neovisionaries.ws.client.WebSocket
import com.neovisionaries.ws.client.WebSocketAdapter
import com.nice.cxonechat.Cancellable
import com.nice.cxonechat.enums.EventType
import com.nice.cxonechat.socket.SocketDefaults.serializer

internal abstract class EventCallback<Event>(
    private val type: EventType,
    private val eventType: Class<Event>,
) : WebSocketAdapter() {

    override fun onTextMessage(websocket: WebSocket?, text: String?) {
        val blueprint: EventBlueprint? = serializer.fromJson(text ?: return, EventBlueprint::class.java)
        if (blueprint?.anyType == type) {
            val event: Event? = serializer.fromJson(text, eventType)
            if (event != null)
                onEvent(websocket ?: return, event)
        }
    }

    abstract fun onEvent(websocket: WebSocket, event: Event)

    companion object {

        inline operator fun <reified Event> invoke(
            type: EventType,
            crossinline callback: WebSocket.(Event) -> Unit,
        ) = object : EventCallback<Event>(type, Event::class.java) {
            override fun onEvent(websocket: WebSocket, event: Event) {
                callback(websocket, event)
            }
        }

        inline fun <reified Event> WebSocket.addCallback(
            type: EventType,
            crossinline callback: WebSocket.(Event) -> Unit,
        ): Cancellable {
            val listener = EventCallback(type, callback)
            addListener(listener)
            return Cancellable { removeListener(listener) }
        }

    }

}
