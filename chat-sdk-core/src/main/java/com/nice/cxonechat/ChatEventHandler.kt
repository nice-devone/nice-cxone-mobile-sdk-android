/*
 * Copyright (c) 2021-2023. NICE Ltd. All rights reserved.
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

import com.google.gson.JsonIOException
import com.nice.cxonechat.event.ChatEvent
import com.nice.cxonechat.exceptions.CXOneException
import com.nice.cxonechat.exceptions.MissingCustomerId

/**
 * Event handler allows for triggering events regarding the overall [Chat]
 * instance.
 * It has no side effects attached to it and can be created again on demand.
 */
@Public
interface ChatEventHandler {

    /**
     * Sends an [event] to server without further delays from background thread.
     * If sending of the event fails, the event is considered consumed anyway.
     *
     * If the event is sent to the server (not to be confused with processed
     * by the server), the [listener] is invoked (from background thread).
     *
     * @param event [ChatEvent] subclass which generates an event model.
     * @param listener nullable listener if the client wants to know when it was sent.
     * @param errorListener An optional listener for errors encountered when handling the event.
     */
    fun trigger(event: ChatEvent, listener: OnEventSentListener? = null, errorListener: OnEventErrorListener? = null)

    /**
     * Listener to be notified when the triggered event is considered sent.
     */
    @Public
    fun interface OnEventSentListener {
        /**
         * Notifies about event being sent to the server, or if the sending has failed and event is considered consumed.
         * Method will be invoked on main thread.
         */
        fun onSent()
    }

    /**
     * Listener which will be notified when the triggered event has failed with an error.
     */
    @Public
    fun interface OnEventErrorListener {

        /**
         * Notifies about event the reason why the event wasn't sent successfully to the server.
         *
         * @param exception The cause of failure. Possible causes are:
         * * [MissingCustomerId] in case of internal invalid state of the SDK.
         * * [JsonIOException] in case of internal SDK error during
         *  the events' serialization.
         */
        fun onError(exception: CXOneException)
    }
}
