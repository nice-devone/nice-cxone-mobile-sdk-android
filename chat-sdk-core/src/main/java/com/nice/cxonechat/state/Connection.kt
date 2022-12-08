package com.nice.cxonechat.state

import com.nice.cxonechat.Public
import java.util.UUID

@Public
abstract class Connection {
    /**
     * Id of the brand currently active in the instance of a chat.
     * It's originally defined as 4 digit number, but can have
     * different forms or sizes.
     * */
    abstract val brandId: Int

    /**
     * Id of an channel currently connected to this instance.
     * */
    abstract val channelId: String

    /**
     * First name of a customer connected to this instance.
     * It can be empty if the customer is not yet authorized.
     * */
    abstract val firstName: String

    /**
     * Last name of a customer connected to this instance.
     * It can be empty if the customer is not yet authorized.
     * */
    abstract val lastName: String

    /**
     * Id of an consumer connected to this instance. It's
     * automatically generated and not empty once connected
     * to the supporting socket for the first time.
     *
     * It's also unchanged as long as the app data is intact.
     * */
    abstract val consumerId: UUID?

    /**
     * Environment through which is this instance connected.
     * */
    abstract val environment: Environment
}
