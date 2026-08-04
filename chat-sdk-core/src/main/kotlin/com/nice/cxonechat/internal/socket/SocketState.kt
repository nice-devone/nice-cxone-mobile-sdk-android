/*
 * Copyright (c) 2021-2026. NICE Ltd. All rights reserved.
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

package com.nice.cxonechat.internal.socket

/**
 * Enum representing the various states of a WebSocket connection.
 */
internal enum class SocketState {
    /**
     * Initial state before any connection attempt has been made.
     */
    INITIAL,

    /**
     * Indicates that the WebSocket is in the process of establishing a connection.
     */
    CONNECTING,

    /**
     * Indicates that the WebSocket connection has been successfully established.
     * The connection ready for communication, but wasn't marked as fully open yet.
     */
    CONNECTED,

    /**
     * Indicates that the WebSocket connection is fully open and ready for communication.
     * Messages can be sent and received over the connection.
     */
    OPEN,

    /**
     * Indicates that the WebSocket connection is in the process of closing.
     * This state is transient and will be followed by the CLOSED state.
     */
    CLOSING,

    /**
     * Indicates that the WebSocket connection has been closed.
     * This is a terminal state, and the connection cannot be reopened.
     * A new connection must be established to resume communication.
     */
    CLOSED
}
