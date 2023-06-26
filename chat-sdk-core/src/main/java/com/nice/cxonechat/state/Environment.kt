package com.nice.cxonechat.state

import com.nice.cxonechat.Public

/**
 * Current SDK environment providing backend (remote) configuration.
 * This can differ from region to region and permits connecting to your
 * own running copy of the service.
 * */
@Public
abstract class Environment {
    /**
     * Name of the environment. It's defined only for semantic reasons.
     */
    abstract val name: String

    /**
     * Physical location of the environment that we'll connect to.
     */
    abstract val location: String

    /**
     * Url used for fetching base configuration, such as: multi/single
     * thread, auth.
     * */
    abstract val baseUrl: String

    /**
     * Socket url used for chat communication.
     * */
    abstract val socketUrl: String

    /**
     * Origin header required for live chat.
     * */
    abstract val originHeader: String

    /**
     * Chat sub-url. Usually defined as `/chat` suffix to [baseUrl]
     * */
    abstract val chatUrl: String
}
