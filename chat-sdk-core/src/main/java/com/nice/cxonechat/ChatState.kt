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

/**
 * Definition of possible Chat states from application point-of-view.
 */
@Public
enum class ChatState {
    /** Not yet configured enough to connect. */
    Initial,

    /**
     * In the process of preparing the chat by performing initial configuration
     * checks fetching the channel configuration.
     */
    Preparing,

    /**
     * The chat is "prepared" but no web socket is open.
     *
     * In the prepared state it is acceptable to generate analytics events via
     * [[ChatEventHandlerActions]] and to attempt to connect the web socket via
     * [Chat.connect], but chat functionality via [Chat.threads] is
     * unavailable.
     */
    Prepared,

    /** In the process of connecting the websocket. */
    Connecting,

    /**
     * A websocket connection has been established.
     *
     * In the `Connected` state it is acceptable to generate analytics events via
     * [[ChatEventHandlerActions]] or to access chat functionality available via
     * [Chat.threads].
     */
    Connected,

    /**
     * A chat state was recovered (if there was anything to recover).
     * If there were any thread/s recovered, then this fact should have signaled via listener.
     */
    Ready,

    /**
     * Chat services are currently offline.
     */
    Offline,

    /** the connection was involuntarily lost. */
    ConnectionLost,
}
