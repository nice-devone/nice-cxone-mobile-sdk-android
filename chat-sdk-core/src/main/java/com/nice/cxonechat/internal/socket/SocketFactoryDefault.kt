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
