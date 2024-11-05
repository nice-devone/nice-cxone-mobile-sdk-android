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

import com.nice.cxonechat.enums.EventType
import com.nice.cxonechat.internal.socket.EventCallback.ReceivedEvent
import com.nice.cxonechat.util.IsoDate
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
internal data class EventInS3(
    @SerialName("eventId")
    @Contextual
    val eventId: UUID,
    @SerialName("createdAt")
    @Contextual
    val createdAt: IsoDate,
    @SerialName("data")
    @Contextual
    val data: Data,
) {
    @Serializable
    internal data class Data(
        @SerialName("s3Object")
        val s3Object: S3Object,

        @SerialName("originEvent")
        val originEvent: OriginEvent
    )

    @Serializable
    internal data class S3Object(
        @SerialName("url")
        val url: String
    )

    @Serializable
    internal data class OriginEvent(
        @SerialName("eventType")
        val eventType: EventType
    )

    companion object : ReceivedEvent<EventInS3> {
        override val type = EventType.EventInS3
    }
}
