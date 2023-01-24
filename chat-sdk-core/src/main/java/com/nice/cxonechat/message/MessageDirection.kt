package com.nice.cxonechat.message

import com.nice.cxonechat.Public

/**
 * Direction of a given message. App is regarded as *your* point of view.
 */
@Public
enum class MessageDirection {
    /**
     * **Client**
     * - Is sending a message.
     *
     * **Agent**
     * - Is receiving a message.
     * */
    ToAgent,

    /**
     * **Client**
     * - Is receiving a message.
     *
     * **Agent**
     * - Is sending a message.
     * */
    ToClient
}
