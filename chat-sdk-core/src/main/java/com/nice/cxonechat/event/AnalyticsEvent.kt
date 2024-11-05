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

import com.nice.cxonechat.analytics.ActionMetadata
import com.nice.cxonechat.enums.VisitorEventType
import com.nice.cxonechat.event.AnalyticsEvent.Data.ValueMapData
import com.nice.cxonechat.internal.model.network.Conversion
import com.nice.cxonechat.internal.model.network.ProactiveActionInfo
import com.nice.cxonechat.internal.model.network.TimeSpentOnPageModel
import com.nice.cxonechat.storage.ValueStorage
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.Date
import java.util.UUID
import com.nice.cxonechat.internal.model.network.PageViewData as PageViewDataModel

@Serializable
internal data class AnalyticsEvent(
    @SerialName("id")
    @Contextual
    val eventId: UUID,
    @SerialName("type")
    val type: VisitorEventType,
    @SerialName("visitId")
    @Contextual
    val visitId: UUID,
    @SerialName("destination")
    val destinationId: Destination,
    @SerialName("createdAtWithMilliseconds")
    @Contextual
    val createdAt: Date,
    @SerialName("data")
    val data: Data,
) {
    @Serializable
    data class Destination(
        @SerialName("id")
        @Contextual
        val destinationId: UUID,
    )

    constructor(
        type: VisitorEventType,
        storage: ValueStorage,
        date: Date = Date(),
        data: Data = ValueMapData(emptyMap()),
    ) : this(
        UUID.randomUUID(),
        type,
        storage.visitId,
        Destination(storage.destinationId),
        date,
        data
    )

    @Serializable
    sealed interface Data {
        @Serializable
        @JvmInline
        value class ProactiveActionData(val data: ProactiveActionInfo) : Data {
            companion object {
                operator fun invoke(data: ActionMetadata) = ProactiveActionData(ProactiveActionInfo(data))
            }
        }

        @Serializable
        @JvmInline
        value class ValueMapData(val data: Map<String, String>) : Data

        @Serializable
        @JvmInline
        value class ConversionData(val data: Conversion) : Data

        @Serializable
        @JvmInline
        value class PageViewData(val data: PageViewDataModel) : Data

        @Serializable
        @JvmInline
        value class TimeSpentOnPageData(val data: TimeSpentOnPageModel) : Data
    }
}
