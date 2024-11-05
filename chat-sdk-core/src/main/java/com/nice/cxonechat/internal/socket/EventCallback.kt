/*
 * Copyright (c) 2021-2024. NICE Ltd. All rights reserved.
 *
 * Licensed under the NICE License;
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    https://github.com/nice-devone/nice-cxone-mobile-sdk-android/blob/main/LICENSE
 *
 * TO THE EXTENT PERMITTED BY APPLICABLE LAW, THE CXONE MOBILE SDK IS PROVIDED ON
 * AN “AS IS” BASIS. NICE HEREBY DISCLAIMS ALL WARRANTIES AND CONDITIONS, EXPRESS
 * OR IMPLIED, INCLUDING (WITHOUT LIMITATION) WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE, NON-INFRINGEMENT, AND TITLE.
 */

package com.nice.cxonechat.internal.socket

import com.nice.cxonechat.Cancellable
import com.nice.cxonechat.enums.ErrorType
import com.nice.cxonechat.enums.EventType
import com.nice.cxonechat.internal.serializer.Default
import com.nice.cxonechat.internal.socket.ErrorCallback.Companion.addErrorCallback
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.serializer
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.UUID

internal abstract class EventCallback<Event>(
    private val type: EventType,
    private val eventType: Class<Event>,
) : WebSocketListener() {
    private val serializer = Default.serializer.serializersModule.serializer(eventType) as DeserializationStrategy<Event>
    interface ReceivedEvent<Type : Any> {
        val type: EventType
    }

    interface EventWithId {
        val eventId: UUID
    }

    override fun onMessage(webSocket: WebSocket, text: String) {
        val blueprint: EventBlueprint? = Default.serializer.runCatching {
            decodeFromString<EventBlueprint>(text)
        }.getOrNull()
        if (blueprint?.anyType === type) {
            onEvent(webSocket, Default.serializer.decodeFromString(serializer, text))
        }
    }

    abstract fun onEvent(websocket: WebSocket, event: Event)

    internal companion object {

        inline operator fun <reified Event> invoke(
            type: EventType,
            crossinline callback: WebSocket.(Event) -> Unit,
        ) = object : EventCallback<Event>(type, Event::class.java) {
            override fun onEvent(websocket: WebSocket, event: Event) = callback(websocket, event)
        }

        inline fun <reified Event : Any> ProxyWebSocketListener.addCallback(
            type: ReceivedEvent<Event>,
            crossinline callback: WebSocket.(Event) -> Unit,
        ) = addCallback<Event>(type.type, callback)

        inline fun <reified Event> ProxyWebSocketListener.addCallback(
            type: EventType,
            crossinline callback: WebSocket.(Event) -> Unit,
        ): Cancellable {
            val listener = EventCallback(type, callback)
            addListener(listener)
            return Cancellable { removeListener(listener) }
        }

        inline fun <reified Received : ReceivedEvent<Event>, reified Event : EventWithId> ProxyWebSocketListener.acceptResponse(
            sent: EventWithId,
            received: ReceivedEvent<Event>,
            errorType: ErrorType? = null,
            crossinline failure: (WebSocket.() -> Unit) = {},
            crossinline success: WebSocket.(Event) -> Unit,
        ) = Cancellable(
            addCallback(received) {
                if (sent.eventId == it.eventId) {
                    success(it)
                }
            },
            errorType?.let {
                addErrorCallback(errorType) {
                    failure()
                }
            }
        )
    }
}
