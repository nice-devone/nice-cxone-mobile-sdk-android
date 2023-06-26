package com.nice.cxonechat

import com.nice.cxonechat.internal.ChatWithParameters
import com.nice.cxonechat.state.Connection
import com.nice.cxonechat.tool.SocketFactoryMock
import com.nice.cxonechat.tool.awaitResult
import kotlin.time.Duration.Companion.milliseconds

internal abstract class AbstractChatTest : AbstractChatTestSubstrate() {

    protected lateinit var connection: Connection
    protected lateinit var chat: Chat

    protected open val authorization
        get() = Authorization("", "")

    override fun prepare() {
        chat = awaitResult(100.milliseconds) {
            val factory = SocketFactoryMock(socket, proxyListener)
            ChatBuilder(entrails, factory)
                .setAuthorization(authorization)
                .setDevelopmentMode(true)
                .build(it)
        }
        connection = (chat as ChatWithParameters).connection
    }
}
