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

import com.nice.cxonechat.enums.EventAction
import com.nice.cxonechat.enums.EventAction.ChatWindowEvent
import com.nice.cxonechat.enums.EventType.EndContact
import com.nice.cxonechat.state.Connection
import com.nice.cxonechat.thread.ChatThread
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
internal data class ActionEndContact(
    @SerialName("action")
    val action: EventAction = ChatWindowEvent,
    @SerialName("eventId")
    @Contextual
    val eventId: UUID = UUID.randomUUID(),
    @SerialName("payload")
    val payload: LegacyPayload<EndContactPayload>,
) {
    constructor(
        connection: Connection,
        thread: ChatThread
    ) : this(
        payload = LegacyPayload(
            eventType = EndContact,
            connection = connection,
            data = EndContactPayload(thread)
        )
    )
}
