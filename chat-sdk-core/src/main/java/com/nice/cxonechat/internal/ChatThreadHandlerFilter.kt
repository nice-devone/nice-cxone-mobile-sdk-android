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

package com.nice.cxonechat.internal

import com.nice.cxonechat.Cancellable
import com.nice.cxonechat.ChatThreadHandler
import com.nice.cxonechat.ChatThreadHandler.OnThreadUpdatedListener
import com.nice.cxonechat.internal.copy.ChatThreadCopyable.Companion.asCopyable
import com.nice.cxonechat.internal.model.MessageText
import com.nice.cxonechat.internal.model.network.Parameters
import com.nice.cxonechat.log.LoggerScope
import com.nice.cxonechat.log.scope
import com.nice.cxonechat.log.verbose
import com.nice.cxonechat.thread.ChatThread

/**
 * ChatThreadHandler that filters out unsupported messages
 * from the chat thread.
 *
 * @param origin The original decorated `ChatThreadHandler` instance.
 * @param chat Parameters used for logging and additional configuration.
 */
internal class ChatThreadHandlerFilter(
    private val origin: ChatThreadHandler,
    chat: ChatWithParameters,
) : ChatThreadHandler by origin,
    LoggerScope by LoggerScope("ChatThreadHandlerFilter", chat.entrails.logger) {

    override fun get(): ChatThread = scope("get") { origin.get().applyFilter() }

    override fun get(listener: OnThreadUpdatedListener): Cancellable = scope("get(listener)") {
        origin.get {
            listener.onUpdated(it.applyFilter())
        }
    }

    /**
     * Extension function to filter out messages, which shouldn't be accessible outside of the SDK, from a `ChatThread`.
     * Filtered out messages:
     *   - Unsupported message answers sent on behalf of the user
     *   Unsupported messages are identified by checking if the `parameters` of the message if they contain the
     *   `isUnsupportedMessageTypeAnswer` flag.
     *
     * @receiver The original `ChatThread` instance.
     * @return A new `ChatThread` instance with unsupported messages removed.
     */
    private fun ChatThread.applyFilter(): ChatThread = scope("filterThread") {
        asCopyable().copy(
            messages = messages.filterNot { message ->
                val parameters: Parameters.Object? = (message as? MessageText)?.parameters as? Parameters.Object
                // Filter out unsupported messages type answers
                parameters?.isUnsupportedMessageTypeAnswer == true
            }
        ).also { filtered ->
            val diff = messages.size - filtered.messages.size
            if (diff > 0) {
                verbose("Messages removed: $diff")
            }
        }
    }
}
