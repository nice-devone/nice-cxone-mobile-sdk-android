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

import com.nice.cxonechat.enums.VisitorEventType.ProactiveActionSuccess
import com.nice.cxonechat.event.AnalyticsEvent.Data.ProactiveActionData
import com.nice.cxonechat.internal.model.network.ProactiveActionInfo
import com.nice.cxonechat.state.Connection
import com.nice.cxonechat.storage.ValueStorage
import java.util.Date

/**
 * Event notifying the backend that a proactive action has succeeded.
 */
internal class ProactiveActionSuccessEvent(
    private val data: ProactiveActionInfo,
    private val date: Date = Date()
) : ChatEvent<AnalyticsEvent>() {

    override fun getModel(
        connection: Connection,
        storage: ValueStorage,
    ) = AnalyticsEvent(
        ProactiveActionSuccess,
        storage = storage,
        date = date,
        data = ProactiveActionData(data),
    )

    override fun toString() = "ProactiveActionSuccessEvent(data=$data)"
}
