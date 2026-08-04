/*
 * Copyright (c) 2021-2026. NICE Ltd. All rights reserved.
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

import com.nice.cxonechat.enums.EventType.MessageSeenChanged
import com.nice.cxonechat.internal.model.MessageModel
import com.nice.cxonechat.internal.socket.EventCallback.ReceivedEvent
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents an event indicating that a message has been seen.
 *
 * @property data The data associated with the event, containing the message details.
 */
@Serializable
internal data class EventMessageSeen(
    @SerialName("data")
    val data: Data,
) {
    /**
     * Nested data class representing the details of the message seen event.
     *
     * @property message The message model associated with the event.
     */
    @Serializable
    data class Data(
        @SerialName("message")
        val message: MessageModel,
    )

    /**
     * Retrieves the message associated with the event.
     *
     * @return The message converted to a domain-specific model.
     */
    val message get() = data.message.toMessage()

    companion object : ReceivedEvent<EventMessageSeen> {
        /**
         * The type of the event, indicating a message seen change.
         */
        override val type = MessageSeenChanged
    }
}
