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

package com.nice.cxonechat.event.thread

import com.nice.cxonechat.internal.model.ActionInternal
import com.nice.cxonechat.message.OutboundMessage
import com.nice.cxonechat.state.Connection
import com.nice.cxonechat.thread.ChatThread

/**
 * Represents an event triggered by a reply button in the chat thread.
 * This event is used to send a message when the user clicks on a reply button.
 * Unlike other [ChatThreadEvent]s this class returns an [OutboundMessage] as the model.
 *
 * @property replyButtonEvent The action associated with the reply button.
 */
internal data class ReplyButtonEvent(
    internal val replyButtonEvent: ActionInternal.ReplyButton,
) : ChatThreadEvent() {

    override fun getModel(thread: ChatThread, connection: Connection) = OutboundMessage(
        message = replyButtonEvent.text,
        postback = replyButtonEvent.postback,
    )
}
