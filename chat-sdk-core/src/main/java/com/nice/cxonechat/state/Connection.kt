package com.nice.cxonechat.state

import com.nice.cxonechat.Public
import java.util.UUID

/**
 * Definition of a data object, which is holding information relevant to
 * the lifecycle of SDK connection to the backend.
 */
@Public
abstract class Connection {
    /**
     * The id of the brand currently active in the instance of a chat, defined as integer.
     */
    abstract val brandId: Int

    /**
     * The id of the channel currently connected to this instance.
     */
    abstract val channelId: String

    /**
     * First name of the customer connected to this instance.
     * It can be empty if the customer is not yet authorized.
     */
    abstract val firstName: String

    /**
     * The last name of the customer connected to this instance.
     * It can be empty if the customer is not yet authorized.
     */
    abstract val lastName: String

    /**
     * The id of the customer connected to this instance.
     * It's automatically generated and not empty once connected
     * to the supporting socket for the first time.
     *
     * It's also unchanged as long as the app data are intact.
     */
    abstract val customerId: UUID?

    /**
     * The environment through which this instance connected.
     */
    abstract val environment: Environment

    /**
     * The internal unique id of installation instance.
     */
    abstract val visitorId: UUID
}
