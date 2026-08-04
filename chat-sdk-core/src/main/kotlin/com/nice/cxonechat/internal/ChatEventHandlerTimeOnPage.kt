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

import com.nice.cxonechat.ChatEventHandler
import com.nice.cxonechat.event.ChatEvent
import com.nice.cxonechat.event.PageViewEndedEvent
import com.nice.cxonechat.event.PageViewEvent
import com.nice.cxonechat.event.TimeSpentOnPageEvent

internal class ChatEventHandlerTimeOnPage(
    private val origin: ChatEventHandler,
    private val chat: ChatWithParameters,
) : ChatWithParameters by chat, ChatEventHandler {

    override suspend fun trigger(event: ChatEvent<*>) {
        when (event) {
            is PageViewEvent -> onPageViewed(event)
            is PageViewEndedEvent -> onPageEnded(event)
            else -> origin.trigger(event)
        }
    }

    private suspend fun onPageViewed(event: PageViewEvent) {
        // Ignore a duplicate event
        if (event.uri == lastPageViewed?.uri && event.title == lastPageViewed?.title) {
            return
        }

        lastPageViewed?.let { last ->
            onPageEnded(PageViewEndedEvent(last.title, last.uri, event.date))
        }
        lastPageViewed = event

        origin.trigger(event)
    }

    private suspend fun onPageEnded(event: PageViewEndedEvent) {
        val last = lastPageViewed

        if (last != null &&
            last.uri == event.uri &&
            last.title == event.title
        ) {
            origin.trigger(
                TimeSpentOnPageEvent(
                    uri = event.uri,
                    title = event.title,
                    timeSpentOnPage = maxOf(1L, (event.date - last.date).inWholeSeconds),
                    date = event.date
                )
            )
        }

        lastPageViewed = null
    }
}
