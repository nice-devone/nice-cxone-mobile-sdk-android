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

import com.nice.cxonechat.SocketFactoryConfiguration
import com.nice.cxonechat.internal.model.ConnectionInternal
import com.nice.cxonechat.storage.ValueStorage
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.TimeUnit.MILLISECONDS
import kotlin.time.Duration.Companion.seconds

internal class SocketFactoryDefault(
    private val config: SocketFactoryConfiguration,
    private val sharedOkHttpClient: OkHttpClient,
) : SocketFactory {

    private val timeout: Long = 5.seconds.inWholeMilliseconds
    private val interval: Long = 10.seconds.inWholeMilliseconds

    private val socketUrl: String by lazy {
        buildString {
            append(config.environment.socketUrl)
            append("?brandId=${config.brandId}")
            append("&channelId=${config.channelId}")
            append("&applicationType=native")
            append("&os=Android")
            @Suppress("DEPRECATION")
            append("&clientVersion=${config.version}")
        }
    }

    private val okHttpClient: OkHttpClient by lazy {
        sharedOkHttpClient.newBuilder()
            .connectTimeout(timeout, MILLISECONDS)
            .pingInterval(interval, MILLISECONDS)
            .build()
    }

    override fun create(listener: WebSocketListener): WebSocket = okHttpClient.newWebSocket(
        Request.Builder().url(socketUrl).build(),
        listener,
    )

    override fun createProxyListener(): ProxyWebSocketListener = ProxyWebSocketListener()

    override fun getConfiguration(storage: ValueStorage) = ConnectionInternal(
        brandId = config.brandId.toInt(),
        channelId = config.channelId,
        firstName = "",
        lastName = "",
        customerId = storage.customerId,
        environment = config.environment,
        visitorId = storage.visitorId,
    )
}
