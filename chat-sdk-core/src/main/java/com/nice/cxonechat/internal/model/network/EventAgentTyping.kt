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
import com.nice.cxonechat.enums.EventType.SenderTypingStarted
import com.nice.cxonechat.internal.model.AgentModel
import com.nice.cxonechat.internal.model.Thread
import com.nice.cxonechat.internal.socket.EventCallback.ReceivedEvent
import com.nice.cxonechat.thread.ChatThread

/** Event received when the agent begins typing or stops typing. */
internal data class EventAgentTyping(
    @SerializedName("data")
    val data: Data,
) {

    /**
     * Information about agent which has triggered the event.
     * Agent value is null if the event is triggered by the customer.
     */
    val agent get() = data.user?.toAgent()

    fun inThread(thread: ChatThread) =
        data.thread.idOnExternalPlatform == thread.id

    data class Data(
        @SerializedName("thread")
        val thread: Thread,
        @SerializedName("user")
        val user: AgentModel?,
    )

    companion object : ReceivedEvent<EventAgentTyping> {
        override val type = SenderTypingStarted
    }
}
