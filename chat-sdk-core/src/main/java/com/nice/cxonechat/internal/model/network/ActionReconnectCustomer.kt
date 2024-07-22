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
import com.nice.cxonechat.enums.EventAction
import com.nice.cxonechat.enums.EventType.ReconnectCustomer
import com.nice.cxonechat.state.Connection
import com.nice.cxonechat.util.UUIDProvider
import java.util.UUID

internal data class ActionReconnectCustomer(
    @SerializedName("action")
    val action: EventAction = EventAction.ChatWindowEvent,
    @SerializedName("eventId")
    val eventId: UUID = UUIDProvider.next(),
    @SerializedName("payload")
    val payload: LegacyPayload<AccessPayload>,
) {

    constructor(
        connection: Connection,
        visitor: UUID,
        token: String?,
    ) : this(
        payload = LegacyPayload(
            eventType = ReconnectCustomer,
            connection = connection,
            data = AccessPayload(token),
            visitor = visitor
        )
    )
}
