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

package com.nice.cxonechat.internal

import com.nice.cxonechat.internal.socket.SocketConnectionListener

/**
 * Handle Multi thread chat specific functionality.
 *
 * A chat in multithread mode is ready once connected.  The process of fetching the thread
 * list and metadata will be initiated once the client indicates an interest in threads by
 * calling [com.nice.cxonechat.Chat.threads]
 *
 * @param origin Existing implementation of [ChatWithParameters] used for delegation.
 */
internal class ChatMultiThread(
    private val origin: ChatWithParameters,
) : ChatWithParameters by origin {

    init {
        chatStateListener?.let { listener ->
            socketListener.addListener(
                SocketConnectionListener(listener = listener) {
                    listener.onConnected()
                    listener.onReady()
                }
            )
        }
    }
}
