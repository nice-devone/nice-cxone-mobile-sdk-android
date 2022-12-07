package com.nice.cxonechat

import com.nice.cxonechat.state.Configuration
import com.nice.cxonechat.state.Environment
import com.nice.cxonechat.thread.ChatThread

/**
 * Current chat instance. Client is not limited to a single instance of any given chat.
 * They can create (or rather [build][ChatBuilder.build]) as many instances as they like.
 * They are though advised that creating *CONFLICTING* (or identical) instances with the
 * same configuration may lead to unexpected consequences.
 *
 * After creating an instance via [build][ChatBuilder.build], the customer is automatically
 * authorized if the authorization is enabled and the customer was not previously authorized,
 * otherwise reconnects the customer.
 *
 * It also starts listening for the authorization and updates customer details after
 * customer authorization. The time it takes to reach the device is undefined.
 *
 * Moreover it starts listening to events that modify welcome messages. Therefore welcome
 * message is only available after we receive the given event and requires no actions from
 * client side. The time it takes to reach the device is undefined.
 * */
@Public
interface Chat : AutoCloseable {

    /**
     * Current environment provided by the [SocketFactoryConfiguration]. It will remain
     * unchanged for the duration of existence of this object.
     * */
    val environment: Environment

    /**
     * Current options of the channel as received from the backend.
     * It will remain unchanged while this [Chat] instance exists.
     */
    val configuration: Configuration

    /**
     * Sets device token (notification push token) to this instance and transmits it
     * to the server. It's not guaranteed that the token is delivered to the server
     * though. Therefore clients are strongly advised to call this method every time
     * they receive new token and reconnect to this instance.
     * */
    fun setDeviceToken(token: String?)

    /**
     * Signs out the user, clears all stored values and closes the connection. Client
     * doesn't need to call [close] explicitly.
     *
     * This instance is considered _dead_ after calling this method
     * */
    fun signOut()

    /**
     * Creates new instance to observe and interact with [Threads][ChatThread].
     * @see ChatThreadsHandler
     * */
    fun threads(): ChatThreadsHandler

    /**
     * Creates new instance to interact with this instance.
     * @see ChatEventHandler
     * */
    fun events(): ChatEventHandler

    /**
     * Creates new instance to modify global custom fields.
     * @see ChatFieldHandler
     * */
    fun customFields(): ChatFieldHandler

    /**
     * Returns new instance of actions handler for this [Chat].
     * @see ChatActionHandler
     * */
    fun actions(): ChatActionHandler

    /**
     * Closes the connection to the chat backend and removes all listeners, even those
     * the client forgot to unregister. The instance is considered dead after calling
     * this method.
     *
     * Interacting with any handlers or methods in this class can lead to unspecified,
     * untested and further unwanted behavior.
     * */
    override fun close()

}
