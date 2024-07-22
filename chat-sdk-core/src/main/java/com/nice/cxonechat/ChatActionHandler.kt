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

import com.nice.cxonechat.analytics.ActionMetadata

/**
 * Handler allowing to listen to popups whenever they are available. You are
 * encouraged to create this instance as soon as possible. It immediately
 * after being created starts to listen to the popup events and keeps the latest
 * one until it's observed.
 */
@Public
interface ChatActionHandler : AutoCloseable {

    /**
     * Subscribes the client to the listener.
     * The listener may be invoked immediately if there's a waiting popup,
     * otherwise waits for any popup events from the server.
     *
     * To unregister this listener call [close]
     * @see close
     */
    fun onPopup(listener: OnPopupActionListener)

    /**
     * Listens for requests to show a popup from proactive
     * message action.
     */
    @Public
    fun interface OnPopupActionListener {

        /**
         * Request to show a popup with given [variables].
         *
         * @param variables are undefined in this context, consult your
         * representative for more information
         * @param metadata you will pass when invoking status changes
         * on events. Such as Success, Failure, Click or others.
         */
        fun onShowPopup(variables: Map<String, Any?>, metadata: ActionMetadata)
    }

    /**
     * Removes listeners and releases internal server listener. After calling
     * this method, this instance is dead and its features are undefined in
     * that case.
     */
    override fun close()
}
