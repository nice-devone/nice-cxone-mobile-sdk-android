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

import com.nice.cxonechat.exceptions.RuntimeChatException

/**
 * Listener for [Chat] instance state changes.
 * The current main purpose of this listener is to provide callbacks which notify integrating application about
 * chat session connectivity changes.
 */
@Public
interface ChatStateListener {
    /**
     * Method is invoked when chat session is unexpectedly lost.
     * [Chat] instance should be considered unusable, until reconnect is performed.
     * If this method is invoked while the [Chat.reconnect] is being performed, then application should consider the
     * reconnection attempt as failed. It is recommended that only a limited number of reconnection attempts are
     * performed.
     * If the problems persist despite the fact that the device has internet connectivity,
     * then please collect logs (assuming the development mode is enabled) and contact your CXone representative
     * with request for support.
     * Integration should also check for possibly lost incoming messages & thread changes once reconnection is done.
     */
    fun onUnexpectedDisconnect()

    /**
     * Method is invoked when chat instance is connected and ready to send/receive messages, events or actions.
     * This happens once initial connection is established or after [Chat.reconnect] is called.
     */
    fun onConnected()

    /**
     * Method is invoked when chat instance has finished performing background tasks after connection was established.
     */
    fun onReady()

    /**
     * Method is invoked when [Chat] instance encounters possible exception in a background process.
     * Application should handle these exceptions according to the description of each [RuntimeChatException].
     * Some of these exceptions can indicate issues during transfer of messages while others may indicate that further
     * interactions with [Chat] will be ignored.
     */
    fun onChatRuntimeException(exception: RuntimeChatException)
}
