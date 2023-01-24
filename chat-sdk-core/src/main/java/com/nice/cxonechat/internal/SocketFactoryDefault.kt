package com.nice.cxonechat.internal

import com.neovisionaries.ws.client.WebSocket
import com.neovisionaries.ws.client.WebSocketExtension
import com.neovisionaries.ws.client.WebSocketFactory
import com.nice.cxonechat.SocketFactoryConfiguration
import com.nice.cxonechat.internal.model.ConnectionInternal
import com.nice.cxonechat.storage.ValueStorage
import kotlin.time.Duration.Companion.seconds

internal class SocketFactoryDefault(
    private val config: SocketFactoryConfiguration,
) : SocketFactory {

    private var timeout: Int = 5.seconds.inWholeMilliseconds.toInt()
    private var interval: Long = 10.seconds.inWholeMilliseconds

    override fun create(): WebSocket {
        val socketUrl = buildString {
            append(config.environment.socketUrl)
            append("?brandId=${config.brandId}")
            append("&channelId=${config.channelId}")
            append("&applicationType=native")
            append("&os=Android")
            append("&clientVersion=${config.version}")
        }
        return WebSocketFactory()
            .setConnectionTimeout(timeout)
            .createSocket(socketUrl)
            .setPingInterval(interval)
            .addExtension(WebSocketExtension.PERMESSAGE_DEFLATE)
            .connect()
    }

    override fun getConfiguration(storage: ValueStorage) = ConnectionInternal(
        brandId = config.brandId.toInt(),
        channelId = config.channelId,
        firstName = "",
        lastName = "",
        consumerId = storage.consumerId,
        environment = config.environment
    )
}
