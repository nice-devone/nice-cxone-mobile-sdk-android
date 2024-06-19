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

package com.nice.cxonechat.internal.socket

import com.nice.cxonechat.ChatStateListener
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener

internal class SocketConnectionListener(
    val onUnexpectedDisconnect: () -> Unit = {},
    val onConnected: () -> Unit,
) : WebSocketListener() {

    constructor(
        listener: ChatStateListener? = null,
        onConnected: () -> Unit,
    ) : this(
        onConnected = onConnected,
        onUnexpectedDisconnect = listener?.let { it::onUnexpectedDisconnect } ?: {}
    )

    override fun onOpen(webSocket: WebSocket, response: Response): Unit = onConnected()

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?): Unit = onUnexpectedDisconnect()
}
