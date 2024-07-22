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

import com.nice.cxonechat.enums.CXOneEnvironment.EU1
import com.nice.cxonechat.internal.model.ConnectionInternal
import com.nice.cxonechat.internal.socket.ProxyWebSocketListener
import com.nice.cxonechat.internal.socket.SocketFactory
import com.nice.cxonechat.storage.ValueStorage
import okhttp3.WebSocket
import okhttp3.WebSocketListener

internal class SocketFactoryMock(
    private val socket: WebSocket,
    private val proxyListener: ProxyWebSocketListener,
) : SocketFactory {

    override fun create(listener: WebSocketListener): WebSocket = socket

    override fun createProxyListener(): ProxyWebSocketListener = proxyListener

    override fun getConfiguration(storage: ValueStorage) = ConnectionInternal(
        brandId = 0,
        channelId = channelId,
        firstName = firstName,
        lastName = lastName,
        customerId = storage.customerId,
        environment = EU1.value,
        visitorId = storage.visitorId,
    )

    companion object {
        const val firstName = "first"
        const val lastName = "last"
        const val channelId = "channelId"
    }
}
