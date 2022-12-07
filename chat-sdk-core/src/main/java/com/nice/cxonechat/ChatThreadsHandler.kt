package com.nice.cxonechat

import androidx.annotation.CheckResult
import com.nice.cxonechat.thread.ChatThread

/**
 * Instance that allows modification, manipulation and observing of [threads][ChatThread].
 *
 * Some methods of this class depend on configuration sent from the server.
 * If the configuration changes remotely, you need to call [ChatBuilder.build] again
 * and use the newly created instance. Saved configuration will never change at run-time.
 *
 * There are no effects attached to initialization of this class.
 * */
@Public
interface ChatThreadsHandler {

    /**
     * Sends a request to refresh the thread-list. It's important that you register
     * a callback with [threads], which returns the newly refreshed values.
     *
     * Client needs to register only one [OnThreadsUpdatedListener] per chat instance.
     * All subsequent [refresh] calls will notify listeners registered in this instance.
     * */
    fun refresh()

    /**
     * Creates a new thread -if permitted by configuration- and returns a handler for it.
     * [threads] should return this new instance even if it's not created on the server
     * yet.
     *
     * Whenever configuration doesn't permit creating new threads, this method throws
     * in response. In cases where configuration permit only singular thread, the method
     * requires client to first call [threads] with a listener. This is to ensure proper
     * validation of creating threads. _Please note that you have to perform this action
     * on every new [ChatThreadsHandler] as it exclusively remembers its own state._
     *
     * The [ChatThreadsHandler] instance remembers at most one thread that contains no
     * messages (ie. is not created on the server).
     * */
    @Throws(Exception::class)
    fun create(): ChatThreadHandler = create(emptyMap())

    /**
     * Creates a new thread -if permitted by configuration- and returns a handler for it.
     * [threads] should return this new instance even if it's not created on the server
     * yet.
     *
     * Whenever configuration doesn't permit creating new threads, this method throws
     * in response.
     * In cases where configuration permits only singular thread, the method
     * requires a client to first call [threads] with a listener.
     * This is to ensure proper validation of creating threads.
     *
     * _Please note that you have to perform this action on every new [ChatThreadsHandler]
     * as it exclusively remembers its own state._
     *
     * The [ChatThreadsHandler] instance remembers at most one thread that contains no
     * messages (ie. is not created on the server).
     *
     * @param customFields An initial map of custom-field key-values specific to this new thread.
     * These custom-fields can be used for personalization during thread creation
     * (e.g.: for a welcome message) and will be sent with a first outbound message.
     * Possible source is from a pre-chat survey.
     * */
    @Throws(Exception::class)
    fun create(customFields: Map<String, String>): ChatThreadHandler

    /**
     * Registers a listeners on this instance that returns new value every time client
     * calls [refresh] or server imperatively forces a refresh remotely.
     *
     * It has side-effects attached to it, see referenced methods.
     * @see [create]
     * @see [refresh]
     * */
    @CheckResult
    fun threads(listener: OnThreadsUpdatedListener): Cancellable

    /**
     * Creates new thread handler with given thread. [thread] is used as a template and
     * this particular instance will not be updated in response to changes in the
     * handler.
     *
     * @param thread recovered from [threads] callback
     * @see ChatThreadHandler
     * */
    fun thread(thread: ChatThread): ChatThreadHandler

    /**
     * Listener allowing user to subscribe on thread list changes.
     * */
    @Public
    fun interface OnThreadsUpdatedListener {
        /**
         * Notifies the listener about changes to the thread list.
         * */
        fun onThreadsUpdated(threads: List<ChatThread>)
    }

}
