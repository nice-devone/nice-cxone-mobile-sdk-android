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
import com.nice.cxonechat.enums.EventAction.ChatWindowEvent
import com.nice.cxonechat.enums.EventType
import com.nice.cxonechat.enums.EventType.SenderTypingEnded
import com.nice.cxonechat.enums.EventType.SenderTypingStarted
import com.nice.cxonechat.internal.model.Thread
import com.nice.cxonechat.state.Connection
import com.nice.cxonechat.thread.ChatThread
import com.nice.cxonechat.util.UUIDProvider
import java.util.UUID

internal data class ActionCustomerTyping(
    @SerializedName("action")
    val action: EventAction = ChatWindowEvent,
    @SerializedName("eventId")
    val eventId: UUID = UUIDProvider.next(),
    @SerializedName("payload")
    val payload: Payload<Data>,
) {

    constructor(
        connection: Connection,
        thread: ChatThread,
        type: EventType,
    ) : this(
        payload = Payload(
            eventType = type,
            connection = connection,
            data = Data(
                thread = Thread(thread)
            )
        )
    )

    data class Data(
        @SerializedName("thread")
        val thread: Thread,
    )

    companion object {

        fun started(
            connection: Connection,
            thread: ChatThread,
        ) = ActionCustomerTyping(
            connection = connection,
            thread = thread,
            type = SenderTypingStarted
        )

        fun ended(
            connection: Connection,
            thread: ChatThread,
        ) = ActionCustomerTyping(
            connection = connection,
            thread = thread,
            type = SenderTypingEnded
        )
    }
}
