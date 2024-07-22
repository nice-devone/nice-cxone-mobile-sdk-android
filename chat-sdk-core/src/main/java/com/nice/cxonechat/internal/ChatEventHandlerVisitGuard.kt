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

import com.nice.cxonechat.ChatEventHandler
import com.nice.cxonechat.ChatEventHandler.OnEventErrorListener
import com.nice.cxonechat.ChatEventHandler.OnEventSentListener
import com.nice.cxonechat.event.ChatEvent
import com.nice.cxonechat.event.PageViewEvent
import com.nice.cxonechat.event.VisitEvent
import com.nice.cxonechat.storage.ValueStorage.VisitDetails
import com.nice.cxonechat.util.UUIDProvider
import java.util.Date

internal class ChatEventHandlerVisitGuard(
    private val origin: ChatEventHandler,
    private val chat: ChatWithParameters,
) : ChatEventHandler by origin {
    override fun trigger(event: ChatEvent, listener: OnEventSentListener?, errorListener: OnEventErrorListener?) {
        if (event is PageViewEvent) {
            validateVisit(event.date)
        }
        origin.trigger(event, listener, errorListener)
    }

    private fun validateVisit(date: Date) {
        // On page view events we want to check and insure that the current
        // visit is valid, if not we want to generate a new visit id.
        val details = chat.storage.visitDetails
        val expires = Date(date.time + 30 * 60 * 1000)

        // if the existing valid date is null or before now, we need a new visit
        if (details?.validUntil?.before(date) != false) {
            chat.storage.visitDetails = VisitDetails(UUIDProvider.next(), expires)

            // generate a new visit event with the new visit
            origin.trigger(VisitEvent(date))
        } else {
            // extend validity of the current visit (even if we created a new one so
            // the time stamp is actually correct).
            chat.storage.visitDetails = details.copy(
                validUntil = expires
            )
        }
    }
}
