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

import com.nice.cxonechat.ChatThreadEventHandler
import com.nice.cxonechat.ChatThreadEventHandler.OnEventErrorListener
import com.nice.cxonechat.ChatThreadEventHandler.OnEventSentListener
import com.nice.cxonechat.event.thread.ChatThreadEvent
import com.nice.cxonechat.exceptions.CXOneException
import com.nice.cxonechat.internal.socket.send
import com.nice.cxonechat.thread.ChatThread

internal class ChatThreadEventHandlerImpl(
    private val chat: ChatWithParameters,
    private val thread: ChatThread,
) : ChatThreadEventHandler {

    override fun trigger(event: ChatThreadEvent, listener: OnEventSentListener?, errorListener: OnEventErrorListener?) {
        val socket = chat.socket ?: return

        try {
            val model = event.getModel(thread, chat.connection)
            when (listener) {
                null -> socket.send(model)
                else -> socket.send(model, listener::onSent)
            }
        } catch(exc: CXOneException) {
            errorListener?.onError(exc)
        }
    }
}
