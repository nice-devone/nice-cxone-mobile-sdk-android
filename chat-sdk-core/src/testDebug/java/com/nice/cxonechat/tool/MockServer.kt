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

package com.nice.cxonechat.tool

import com.nice.cxonechat.internal.socket.ProxyWebSocketListener
import io.mockk.every
import io.mockk.mockk
import okhttp3.WebSocket

internal class MockServer {

    val socket: WebSocket = mockk {
        every { send(text = any()) } returns true
    }
    val proxyListener: ProxyWebSocketListener = ProxyWebSocketListener()

    fun open() {
        proxyListener.onOpen(socket, mockk())
    }

    fun sendServerMessage(text: String) {
        proxyListener.onMessage(socket, text)
    }
}
