/*
 * Copyright (c) 2021-2026. NICE Ltd. All rights reserved.
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

import androidx.annotation.VisibleForTesting
import com.nice.cxonechat.exceptions.RuntimeChatException.ServerCommunicationError
import com.nice.cxonechat.internal.serializer.Default.serializer
import kotlinx.serialization.serializer
import okhttp3.WebSocket

/**
 * Serializes [model] and sends it as text via [WebSocket].
 *
 * @throws ServerCommunicationError if the message could not be enqueued
 *   (socket is closing/closed or outbound buffer is full).
 */
internal fun WebSocket.send(model: Any) {
    val kser = serializer.serializersModule.serializer(model::class.java)
    val text = serializer.encodeToString(kser, model)
    if (!send(text = text)) {
        throw ServerCommunicationError(MESSAGE_WEBSOCKET_SEND_FAILED)
    }
}

@VisibleForTesting
internal const val MESSAGE_WEBSOCKET_SEND_FAILED = "WebSocketSendFailed"
