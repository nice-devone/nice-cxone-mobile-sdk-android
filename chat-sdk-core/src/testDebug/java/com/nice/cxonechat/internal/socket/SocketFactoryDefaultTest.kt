/*
 * Copyright (c) 2021-2025. NICE Ltd. All rights reserved.
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

import com.nice.cxonechat.SocketFactoryConfiguration
import com.nice.cxonechat.core.BuildConfig
import com.nice.cxonechat.enums.CXoneEnvironment
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.junit.Test
import java.util.UUID
import kotlin.random.Random

class SocketFactoryDefaultTest {
    @Test
    fun create() {
        val webSocket: WebSocket = mockk()
        val listenerSlot = slot<WebSocketListener>()
        val requestSlot = slot<Request>()
        val client: OkHttpClient = mockk {
            every { newWebSocket(capture(requestSlot), capture(listenerSlot)) } returns webSocket
        }
        val builder: OkHttpClient.Builder = mockk {
            every { connectTimeout(any(), any()) } returns this
            every { pingInterval(any(), any()) } returns this
            every { build() } returns client
        }
        val sharedClient: OkHttpClient = mockk {
            every { newBuilder() } returns builder
        }
        val listener: WebSocketListener = mockk()
        val socketFactory = SocketFactoryDefault(configuration, sharedClient)

        socketFactory.create(listener, visitorId)

        verify {
            sharedClient.newBuilder()
            builder.build()
            client.newWebSocket(any(), any())
        }

        listenerSlot.captured shouldBe listener

        with(requestSlot.captured.url) {
            // note scheme starts as wss but Request.Builder.build makes it https, it'll later get changed back
            // to wss somewhere else down the stack.
            scheme shouldBe "https"
            host shouldBe "chat-gateway-de-eu1.niceincontact.com"
            query shouldBe buildString {
                append("brandId=")
                append(brandId)
                append("&channelId=")
                append(channelId)
                append("&visitorId=")
                append(visitorId)
                append("&sdkPlatform=android&sdkVersion=")
                append(BuildConfig.VERSION_NAME)
            }
        }
    }

    private companion object {
        val brandId = Random.Default.nextLong()
        val channelId = UUID.randomUUID().toString()
        val visitorId = UUID.randomUUID().toString()
        val configuration = SocketFactoryConfiguration(
            CXoneEnvironment.EU1.value,
            brandId = brandId,
            channelId = channelId
        )
    }
}
