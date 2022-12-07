package com.nice.cxonechat.internal.model

import com.nice.cxonechat.state.Configuration

internal data class ConfigurationInternal(
    override val hasMultipleThreadsPerEndUser: Boolean,
    override val isProactiveChatEnabled: Boolean,
    override val isAuthorizationEnabled: Boolean,
) : Configuration() {

    override fun toString() = buildString {
        append("Configuration(hasMultipleThreadsPerEndUser=")
        append(hasMultipleThreadsPerEndUser)
        append(", isProactiveChatEnabled=")
        append(isProactiveChatEnabled)
        append(", isAuthorizationEnabled=")
        append(isAuthorizationEnabled)
        append(")")
    }

}
