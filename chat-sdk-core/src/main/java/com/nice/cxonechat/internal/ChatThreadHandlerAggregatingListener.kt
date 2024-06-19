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
import com.nice.cxonechat.ChatThreadHandler.OnThreadUpdatedListener
import com.nice.cxonechat.thread.ChatThread
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Class responsible for aggregating multiple [OnThreadUpdatedListener]s into one.
 */
internal class ChatThreadHandlerAggregatingListener(
    private val origin: ChatThreadHandler,
) : ChatThreadHandler by origin, OnThreadUpdatedListener {

    private val listeners = CopyOnWriteArrayList<OnThreadUpdatedListener>()

    @Volatile
    private var cancellable: Cancellable? = null

    override fun get(listener: OnThreadUpdatedListener): Cancellable {
        synchronized(this) {
            listeners.add(listener)
            if (cancellable == null) {
                cancellable = origin.get(this)
            }
        }
        return Cancellable {
            synchronized(this) {
                listeners.remove(listener)
                if (listeners.isEmpty()) {
                    cancellable?.cancel()
                    cancellable = null
                }
            }
        }
    }

    override fun onUpdated(thread: ChatThread) {
        for (listener in listeners) {
            listener.onUpdated(thread)
        }
    }
}
