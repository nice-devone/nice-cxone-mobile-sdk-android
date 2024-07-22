/*
 * Copyright (c) 2021-2024. NICE Ltd. All rights reserved.
 *
 * Licensed under the NICE License;
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    https://github.com/nice-devone/nice-cxone-mobile-sdk-android/blob/main/LICENSE
 *
 * TO THE EXTENT PERMITTED BY APPLICABLE LAW, THE CXONE MOBILE SDK IS PROVIDED ON
 * AN “AS IS” BASIS. NICE HEREBY DISCLAIMS ALL WARRANTIES AND CONDITIONS, EXPRESS
 * OR IMPLIED, INCLUDING (WITHOUT LIMITATION) WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE, NON-INFRINGEMENT, AND TITLE.
 */

package com.nice.cxonechat

import androidx.annotation.CheckResult
import com.nice.cxonechat.thread.ChatThread

/**
 * Instance of a thread handler. This instance will contain the most up-to-date
 * [thread][ChatThread], even newer than the thread it was created from.
 *
 * This object will not transfer any changes to the parent object and all updates
 * are contained within this class. It's up to the client to keep the instance
 * alive as long as they need it.
 *
 * It has no side effects attached to it.
 */
@Public
interface ChatThreadHandler {

    /**
     * Returns the most up-to-date [thread][ChatThread] with updated properties.
     * This value is internally stored and is safe to call in main-thread context.
     *
     * Effects and listeners update the instance if registered or called, otherwise
     * the method returns the **default** value it was created from.
     */
    fun get(): ChatThread

    /**
     * Registers a [listener] which can be cancelled any time. The thread registers
     * for listeners to thread recovered events (triggered by [refresh]), message
     * events (triggered by [messages] and its methods) and metadata events.
     *
     * It will only call updates on the thread matching ids to the thread the
     * handler instance was created from.
     */
    @CheckResult
    fun get(listener: OnThreadUpdatedListener): Cancellable

    /**
     * Notifies the server asynchronously that client wants to change the thread
     * name to the supplied [name]. The change can fail, though if it doesn't [get]
     * methods will return updated thread.
     */
    fun setName(name: String)

    /**
     * Notifies the server asynchronously that client wants the thread to be
     * refreshed. The request can be performed even if the client doesn't
     * expect new data. In which case the [get] returns updated value anyway.
     *
     * Please note that this method and consequent behavior is defined only
     * if [get] callback is registered. In all other cases this method does
     * nothing.
     */
    fun refresh()

    /**
     * Requests that the server archive this thread.
     *
     * @param onComplete Callback to be performed when the request has completed.
     * This routine will be invoked with a success value indicating whether or
     * not the thread has been successfully archived.
     */
    fun archive(onComplete: (Boolean) -> Unit = {})

    /**
     * Returns new instance of message handler for this [ChatThread].
     * @see ChatThreadMessageHandler
     */
    fun messages(): ChatThreadMessageHandler

    /**
     * Returns new instance of thread event handler for this [ChatThread].
     * @see ChatThreadEventHandler
     */
    fun events(): ChatThreadEventHandler

    /**
     * Returns new instance of field handler for this [ChatThread].
     * @see ChatFieldHandler
     */
    fun customFields(): ChatFieldHandler

    /**
     * Terminate the contact.
     *
     * @throws InvalidStateException if the current channel is not a live chat.
     */
    fun endContact()

    /**
     * Listener allowing to receive changes to a chat thread.
     * @see get
     */
    @Public
    fun interface OnThreadUpdatedListener {
        /**
         * Method invoked whenever thread changes. Please note that the
         * [equality][ChatThread.equals] may be identical for subsequent calls.
         *
         * Equality is also defined by all properties inside the object. If you
         * need to know that the thread is the same, use [id][ChatThread.id].
         */
        fun onUpdated(thread: ChatThread)
    }
}
