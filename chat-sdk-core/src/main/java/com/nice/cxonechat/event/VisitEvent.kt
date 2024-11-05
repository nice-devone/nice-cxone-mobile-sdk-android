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

package com.nice.cxonechat.event

import com.nice.cxonechat.enums.VisitorEventType.VisitorVisit
import com.nice.cxonechat.state.Connection
import com.nice.cxonechat.storage.ValueStorage
import java.util.Date

/**
 * Event notifying the backend that user has entered the chat area
 * of the given application. Session length is capped at 30 minutes
 * thereafter you need to resend the visit event if the user
 * performs any action.
 */
internal class VisitEvent(
    internal val date: Date = Date()
) : ChatEvent<AnalyticsEvent>() {

    override fun getModel(
        connection: Connection,
        storage: ValueStorage,
    ) = AnalyticsEvent(
        VisitorVisit,
        storage = storage,
        date = date
    )

    override fun toString() = "VisitEvent()"
}
