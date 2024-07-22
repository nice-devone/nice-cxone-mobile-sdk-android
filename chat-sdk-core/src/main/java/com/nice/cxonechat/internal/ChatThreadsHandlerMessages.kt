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

import com.nice.cxonechat.Cancellable
import com.nice.cxonechat.ChatThreadHandler
import com.nice.cxonechat.ChatThreadsHandler
import com.nice.cxonechat.ChatThreadsHandler.OnThreadsUpdatedListener
import com.nice.cxonechat.internal.copy.ChatThreadCopyable.Companion.asCopyable
import com.nice.cxonechat.message.Message
import com.nice.cxonechat.thread.ChatThread
import java.util.UUID

internal class ChatThreadsHandlerMessages(
    private val origin: ChatThreadsHandler,
) : ChatThreadsHandler by origin {

    private val messages = mutableMapOf<UUID, List<Message>>()
    private val cancellable = CompoundCancellable()

    override fun threads(listener: OnThreadsUpdatedListener): Cancellable {
        return cancellable withPrimary origin.threads { threads ->
            val items = threads.map { thread ->
                when (thread.messages.isEmpty()) {
                    true -> thread.asCopyable().copy(messages = messages[thread.id].orEmpty())
                    else -> thread
                }
            }
            listener.onThreadsUpdated(items)
        }
    }

    override fun thread(thread: ChatThread): ChatThreadHandler {
        val cancellableEffect = origin.thread(thread.asCopyable().copy()).get {
            messages[it.id] = it.messages
        }
        cancellable.withEffect(thread.id, cancellableEffect)
        return origin.thread(thread)
    }

    private class CompoundCancellable {

        private val primaries = mutableListOf<Cancellable>()
        private val effects = mutableMapOf<UUID, Cancellable>()

        infix fun withPrimary(cancellable: Cancellable): Cancellable {
            primaries += cancellable
            return Cancellable {
                primaries -= cancellable
                if (primaries.isEmpty()) {
                    for ((_, effect) in effects) {
                        effect.cancel()
                    }
                }
            }
        }

        fun withEffect(id: UUID, cancellable: Cancellable) {
            effects[id]?.cancel()
            effects[id] = cancellable
        }
    }
}
