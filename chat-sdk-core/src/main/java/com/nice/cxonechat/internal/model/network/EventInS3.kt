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

package com.nice.cxonechat.internal.model.network

import com.google.gson.annotations.SerializedName
import com.nice.cxonechat.enums.EventType
import com.nice.cxonechat.internal.socket.EventCallback.ReceivedEvent
import com.nice.cxonechat.util.IsoDate
import java.util.UUID

internal data class EventInS3(
    @SerializedName("eventId")
    val eventId: UUID,
    @SerializedName("createdAt")
    val createdAt: IsoDate,
    @SerializedName("data")
    val data: Data,
) {
    internal data class Data(
        @SerializedName("s3Object")
        val s3Object: S3Object,

        @SerializedName("originEvent")
        val originEvent: OriginEvent
    )

    internal data class S3Object(
        @SerializedName("url")
        val url: String
    )

    internal data class OriginEvent(
        @SerializedName("eventType")
        val eventType: EventType
    )

    companion object : ReceivedEvent<EventInS3> {
        override val type = EventType.EventInS3
    }
}
