/*
 * Copyright (c) 2021-2023. NICE Ltd. All rights reserved.
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

import com.nice.cxonechat.ChatEventHandler
import com.nice.cxonechat.ChatEventHandler.OnEventSentListener
import com.nice.cxonechat.event.ChatEvent
import com.nice.cxonechat.event.PageViewEndedEvent
import com.nice.cxonechat.event.PageViewEvent
import com.nice.cxonechat.event.TimeSpentOnPageEvent
import java.lang.Long.max

internal class ChatEventHandlerTimeOnPage(
    private val origin: ChatEventHandler,
    private val chat: ChatWithParameters,
) : ChatWithParameters by chat, ChatEventHandler {
    override fun trigger(event: ChatEvent, listener: OnEventSentListener?) {
        when (event) {
            is PageViewEvent -> onPageViewed(event, listener)
            is PageViewEndedEvent -> onPageEnded(event, listener)
            else -> origin.trigger(event, listener)
        }
    }

    private fun onPageViewed(event: PageViewEvent, listener: OnEventSentListener?) {
        // Ignore a duplicate event
        if (event.uri == lastPageViewed?.uri && event.title == lastPageViewed?.title) {
            listener?.onSent()
            return
        }

        lastPageViewed?.let { last ->
            onPageEnded(PageViewEndedEvent(last.title, last.uri, event.date), listener)
        }
        lastPageViewed = event

        origin.trigger(event, listener)
    }

    private fun onPageEnded(event: PageViewEndedEvent, listener: OnEventSentListener?) {
        val last = lastPageViewed

        if (last != null &&
            last.uri == event.uri &&
            last.title == event.title
        ) {
            origin.trigger(
                TimeSpentOnPageEvent(
                    uri = event.uri,
                    title = event.title,
                    timeSpentOnPage = max(1, (event.date.time - last.date.time) / 1000),
                    date = event.date
                ),
                listener
            )
        } else {
            listener?.onSent()
        }

        lastPageViewed = null
    }
}
