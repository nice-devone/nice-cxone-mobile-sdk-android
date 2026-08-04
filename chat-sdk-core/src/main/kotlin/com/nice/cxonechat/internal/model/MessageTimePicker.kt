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

package com.nice.cxonechat.internal.model

import com.nice.cxonechat.internal.model.network.MessagePolyContent
import com.nice.cxonechat.message.Attachment
import com.nice.cxonechat.message.Message.TimePicker
import com.nice.cxonechat.message.MessageAuthor
import com.nice.cxonechat.message.MessageDirection
import com.nice.cxonechat.message.MessageMetadata
import com.nice.cxonechat.message.TimeSlot
import java.util.UUID
import kotlin.time.Instant

/**
 * Internal implementation of [TimePicker] that wraps a [MessageModel] whose content is
 * [MessagePolyContent.TimePicker].
 *
 * Delegates all property access to the underlying model and content, providing the
 * public-facing [TimePicker] API to SDK consumers.
 */
internal data class MessageTimePicker(
    private val model: MessageModel,
) : TimePicker() {
    private val content
        get() = model.messageContent as MessagePolyContent.TimePicker

    override val id: UUID
        get() = model.idOnExternalPlatform
    override val threadId: UUID
        get() = model.threadIdOnExternalPlatform
    override val createdAt: Instant
        get() = model.createdAt
    override val direction: MessageDirection
        get() = model.direction.toMessageDirection()
    override val metadata: MessageMetadata
        get() = model.metadata
    override val author: MessageAuthor?
        get() = model.author
    override val attachments: Iterable<Attachment>
        get() = model.attachments.map(AttachmentModel::toAttachment)
    override val title: String
        get() = content.payload.title.content
    override val popupTitle: String
        get() = content.payload.event.title.content
    override val timeSlots: Iterable<TimeSlot>
        get() = content.payload.event.timeSlots.map(::TimeSlotInternal)
    override val fallbackText: String
        get() = content.fallbackText
    override val isAiGenerated: Boolean
        get() = direction == MessageDirection.ToClient && model.isAiGenerated

    override fun toString() = buildString {
        append("Message.TimePicker(")
        append("id=$id, ")
        append("threadId=$threadId, ")
        append("createdAt=$createdAt, ")
        append("direction=$direction, ")
        append("metadata=$metadata, ")
        append("author=$author, ")
        append("attachments=${attachments.joinToString()}, ")
        append("title='$title', ")
        append("popupTitle='$popupTitle', ")
        append("timeSlots=${timeSlots.joinToString()}, ")
        append("fallbackText='$fallbackText'")
        append(")")
    }
}
