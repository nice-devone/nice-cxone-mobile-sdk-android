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

import com.nice.cxonechat.enums.VisitorEventType.Conversion
import com.nice.cxonechat.event.AnalyticsEvent.Data.ConversionData
import com.nice.cxonechat.state.Connection
import com.nice.cxonechat.storage.ValueStorage
import java.util.Date
import com.nice.cxonechat.internal.model.network.Conversion as ConversionModel

/**
 * Event notifying the backend that a conversion has been made.
 *
 * Conversions are understood as a completed activities that are important
 * to your business.
 */
internal class ConversionEvent(
    private val type: String,
    private val value: Number,
    private val date: Date = Date()
) : ChatEvent<AnalyticsEvent>() {
    override fun getModel(
        connection: Connection,
        storage: ValueStorage,
    ): AnalyticsEvent {
        val conversion = ConversionModel(
            type = type,
            value = value.toLong(),
            timestamp = date
        )
        return AnalyticsEvent(
            Conversion,
            storage,
            date,
            ConversionData(conversion),
        )
    }

    override fun toString() = "ConversionEvent(type='$type', value=$value)"
}
