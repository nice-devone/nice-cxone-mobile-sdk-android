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

package com.nice.cxonechat.event.thread

import com.nice.cxonechat.internal.model.AttachmentModel
import com.nice.cxonechat.internal.model.CustomFieldModel
import com.nice.cxonechat.internal.model.network.ActionMessage
import com.nice.cxonechat.state.Connection
import com.nice.cxonechat.thread.ChatThread
import java.util.UUID

internal class MessageEvent(
    private val message: String,
    private val attachments: Iterable<AttachmentModel>,
    private val fields: List<CustomFieldModel>,
    private val authToken: String?,
    private val postback: String?,
) : ChatThreadEvent() {

    val messageId: UUID = UUID.randomUUID()

    override fun getModel(
        thread: ChatThread,
        connection: Connection,
    ) = ActionMessage(
        connection = connection,
        thread = thread,
        id = messageId,
        message = message,
        attachments = attachments,
        fields = fields,
        token = authToken,
        postback = postback
    )
}
