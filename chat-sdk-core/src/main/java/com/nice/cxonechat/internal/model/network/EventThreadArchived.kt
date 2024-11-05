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
import com.nice.cxonechat.internal.socket.EventCallback.EventWithId
import com.nice.cxonechat.internal.socket.EventCallback.ReceivedEvent
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
internal data class EventThreadArchived(
    @SerialName("eventId")
    @Contextual
    override val eventId: UUID,
    @SerialName("postback")
    val postback: Postback,
) : EventWithId {
    @Serializable
    data class Postback(
        @SerialName("eventType")
        val eventType: String
    )

    companion object : ReceivedEvent<EventThreadArchived> {
        override val type = EventType.ThreadArchived
    }
}
