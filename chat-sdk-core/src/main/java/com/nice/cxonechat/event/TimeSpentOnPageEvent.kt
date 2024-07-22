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

import com.nice.cxonechat.enums.VisitorEventType.TimeSpentOnPage
import com.nice.cxonechat.internal.model.network.TimeSpentOnPageModel
import com.nice.cxonechat.state.Connection
import com.nice.cxonechat.storage.ValueStorage
import java.util.Date

/**
 * Event notifying the backend that user has clicked a url in chat or visited other unspecified
 * url withing the chat platform.
 */
internal class TimeSpentOnPageEvent(
    private val title: String,
    private val uri: String,
    private val date: Date = Date(),
    private val timeSpentOnPage: Long
) : ChatEvent() {
    override fun getModel(
        connection: Connection,
        storage: ValueStorage,
    ): Any {
        val model = TimeSpentOnPageModel(
            url = uri,
            title = title,
            timeSpentOnPage = timeSpentOnPage
        )
        return AnalyticsEvent(
            TimeSpentOnPage,
            storage,
            date,
            model
        )
    }

    override fun toString() = "TimeSpentOnPage(title='$title', uri='$uri', timeOnPage=$timeSpentOnPage)"
}
