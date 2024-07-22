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

import com.google.gson.annotations.SerializedName
import com.nice.cxonechat.enums.VisitorEventType
import com.nice.cxonechat.storage.ValueStorage
import java.util.Date
import java.util.UUID

internal data class AnalyticsEvent(
    @SerializedName("id")
    val eventId: UUID,
    @SerializedName("type")
    val type: VisitorEventType,
    @SerializedName("visitId")
    val visitId: UUID,
    @SerializedName("destination")
    val destinationId: Destination,
    @SerializedName("createdAtWithMilliseconds")
    val createdAt: Date,
    @SerializedName("data")
    val data: Any
) {
    data class Destination(
        @SerializedName("id")
        val destinationId: UUID
    )

    constructor(type: VisitorEventType, storage: ValueStorage, date: Date = Date(), data: Any = mapOf<String, String>()) : this(
        UUID.randomUUID(),
        type,
        storage.visitId,
        Destination(storage.destinationId),
        date,
        data
    )
}
