package com.nice.cxonechat.internal

import com.nice.cxonechat.Authorization
import com.nice.cxonechat.Cancellable
import com.nice.cxonechat.ChatBuilder
import com.nice.cxonechat.ChatBuilder.OnChatBuiltCallback
import com.nice.cxonechat.log.LoggerScope
import com.nice.cxonechat.log.severe

internal class ChatBuilderLogging(
    private val origin: ChatBuilder,
    entrails: ChatEntrails,
) : ChatBuilder, LoggerScope by LoggerScope<ChatBuilder>(entrails.logger) {

    private var developmentMode: Boolean = false

    override fun setAuthorization(authorization: Authorization) = apply {
        origin.setAuthorization(authorization)
    }

    override fun setDevelopmentMode(enabled: Boolean) = apply {
        this.developmentMode = enabled
        origin.setDevelopmentMode(enabled)
    }

    override fun setUserName(first: String, last: String) = apply {
        origin.setUserName(first, last)
    }

    override fun build(callback: OnChatBuiltCallback): Cancellable {
        return try {
            origin.build(callback)
        } catch (e: Throwable) {
            if (developmentMode) severe("Failed to initialize", e)
            throw e
        }
    }

}
