package com.nice.cxonechat.state

import com.nice.cxonechat.Public

/**
 * The various options for how a channel is configured.
 */
@Public
abstract class Configuration {

    /** Whether the channel supports multiple threads for the same user. */
    abstract val hasMultipleThreadsPerEndUser: Boolean

    /** Whether the channel supports proactive chat features. */
    abstract val isProactiveChatEnabled: Boolean

    /** Whether OAuth authorization is enabled for the channel. */
    abstract val isAuthorizationEnabled: Boolean

}
