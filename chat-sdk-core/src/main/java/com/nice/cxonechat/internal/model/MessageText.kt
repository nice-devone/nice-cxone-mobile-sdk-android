/*
 * Copyright (c) 2021-2023. NICE Ltd. All rights reserved.
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

package com.nice.cxonechat.internal.model

import com.nice.cxonechat.internal.model.network.MessagePolyContent
import com.nice.cxonechat.message.Attachment
import com.nice.cxonechat.message.Message.Text
import com.nice.cxonechat.message.MessageAuthor
import com.nice.cxonechat.message.MessageDirection
import com.nice.cxonechat.message.MessageMetadata
import java.util.Date
import java.util.UUID

internal data class MessageText(
    private val model: MessageModel,
) : Text() {

    private val content
        get() = model.messageContent as MessagePolyContent.Text

    override val id: UUID
        get() = model.idOnExternalPlatform
    override val threadId: UUID
        get() = model.threadIdOnExternalPlatform
    override val createdAt: Date
        get() = model.createdAt
    override val direction: MessageDirection
        get() = model.direction.toMessageDirection()
    override val metadata: MessageMetadata
        get() = model.userStatistics.toMessageMetadata()
    override val author: MessageAuthor?
        get() = model.author
    override val attachments: Iterable<Attachment>
        get() = model.attachments.map(AttachmentModel::toAttachment)
    override val text: String
        get() = content.payload.text
    override val fallbackText: String?
        get() = content.fallbackText

    override fun toString() = buildString {
        append("Message.Text(id=")
        append(id)
        append(", threadId=")
        append(threadId)
        append(", createdAt=")
        append(createdAt)
        append(", direction=")
        append(direction)
        append(", metadata=")
        append(metadata)
        append(", author=")
        append(author)
        append(", attachments=")
        append(attachments)
        append(", text='")
        append(text)
        append("')")
    }
}
