package com.nice.cxonechat.internal.socket

import com.nice.cxonechat.Cancellable
import com.nice.cxonechat.enums.EventType
import com.nice.cxonechat.internal.serializer.Default.serializer
import okhttp3.WebSocket
import okhttp3.WebSocketListener

internal abstract class EventCallback<Event>(
    private val type: EventType,
    private val eventType: Class<Event>,
) : WebSocketListener() {

    override fun onMessage(webSocket: WebSocket, text: String) {
        val blueprint: EventBlueprint? = serializer.fromJson(text, EventBlueprint::class.java)
        if (blueprint?.anyType == type) {
            val event: Event? = serializer.fromJson(text, eventType)
            if (event != null) {
                onEvent(webSocket, event)
            }
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

        inline fun <reified Event> ProxyWebSocketListener.addCallback(
            type: EventType,
            crossinline callback: WebSocket.(Event) -> Unit,
        ): Cancellable {
            val listener = EventCallback(type, callback)
            addListener(listener)
            return Cancellable { removeListener(listener) }
        }
    }
}
