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

package com.nice.cxonechat.ui.util.preview.message

import androidx.compose.runtime.Stable
import com.nice.cxonechat.message.Action
import com.nice.cxonechat.message.Attachment
import com.nice.cxonechat.message.MessageAuthor
import com.nice.cxonechat.message.MessageDirection
import com.nice.cxonechat.message.MessageDirection.ToClient
import com.nice.cxonechat.message.MessageMetadata
import com.nice.cxonechat.message.MessageStatus
import com.nice.cxonechat.message.MessageStatus.Sent
import com.nice.cxonechat.ui.composable.conversation.model.Message
import com.nice.cxonechat.ui.util.DateProvider
import java.util.Date
import java.util.UUID

internal fun SdkMessage.asMessage() = when (this) {
    is SdkText -> Message.Text(this)
    is SdkListPicker -> Message.ListPicker(this) {}
    is SdkQuickReply -> Message.QuickReply(this) {}
    is SdkRichLink -> Message.RichLink(this)
    else -> Message.Unsupported(this)
}

internal typealias SdkMessage = com.nice.cxonechat.message.Message
internal typealias SdkMessageUnsupported = com.nice.cxonechat.message.Message.Unsupported

internal data class UiSdkUnsupportedMessage(
    override val id: UUID = UUID.randomUUID(),
    override val threadId: UUID = UUID.randomUUID(),
    override val createdAt: Date = DateProvider.now(),
    override val direction: MessageDirection = ToClient,
    override val author: MessageAuthor? = ToClient.toPerson(),
    override val metadata: MessageMetadata = Metadata(),
    override val attachments: Iterable<Attachment> = emptyList(),
    override val fallbackText: String = "Unsupported message content",
    override val text: String = "$fallbackText:\nPLUGIN — SATISFACTION_SURVEY",
) : SdkMessageUnsupported()

@Stable
internal data class Text(
    override val text: String = "Text Message",
    override val attachments: Iterable<Attachment> = sequenceOf<Attachment>().asIterable(),
    override val id: UUID = UUID.randomUUID(),
    override val threadId: UUID = UUID.randomUUID(),
    override val createdAt: Date = DateProvider.now(),
    override val direction: MessageDirection = ToClient,
    override val author: SdkAuthor? = direction.toPerson(),
    override val metadata: SdkMetadata = Metadata(),
    override val fallbackText: String? = null,
) : SdkText()

internal typealias SdkText = com.nice.cxonechat.message.Message.Text
internal typealias UiSdkText = Text

@Stable
internal data class ListPicker(
    override val title: String = "List Picker",
    override val text: String = "List Picker Description",
    override val actions: Iterable<Action> = listOf(
        ReplyButton("Reply without media"),
        ReplyButton("Reply with a cat image", mediaUrl = "https://http.cat/203", mediaMimeType = "image/jpeg")
    ),
    override val id: UUID = UUID.randomUUID(),
    override val threadId: UUID = UUID.randomUUID(),
    override val createdAt: Date = DateProvider.now(),
    override val direction: MessageDirection = ToClient,
    override val author: SdkAuthor? = ToClient.toPerson(),
    override val metadata: SdkMetadata = Metadata(),
    override val attachments: Iterable<Attachment> = listOf(),
    override val fallbackText: String? = null,
) : SdkListPicker()

internal typealias SdkListPicker = com.nice.cxonechat.message.Message.ListPicker
internal typealias UiSdkListPicker = ListPicker

@Stable
internal data class QuickReply(
    override val title: String = "This is a quick reply card",
    override val actions: Iterable<Action> = listOf(
        ReplyButton("Some text"),
        ReplyButton("Random cat", "https://http.cat/203")
    ),
    override val id: UUID = UUID.randomUUID(),
    override val threadId: UUID = UUID.randomUUID(),
    override val createdAt: Date = Date(),
    override val direction: MessageDirection = ToClient,
    override val metadata: SdkMetadata = Metadata(),
    override val author: SdkAuthor? = ToClient.toPerson(),
    override val attachments: Iterable<Attachment> = emptyList(),
    override val fallbackText: String? = null,
) : SdkQuickReply()

internal typealias SdkQuickReply = com.nice.cxonechat.message.Message.QuickReplies
internal typealias UiSdkQuickReply = QuickReply

@Stable
internal data class RichLink(
    override val title: String = "Rich Link",
    override val url: String = "https://nice.com",
    override val media: SdkMedia = Media(
        url = "https://thecatapi.com/api/images/get?format=src&type=jpeg",
        mimeType = "image/jpeg",
        fileName = "Preview Image",
    ),
    override val id: UUID = UUID.randomUUID(),
    override val threadId: UUID = UUID.randomUUID(),
    override val createdAt: Date = Date(),
    override val direction: MessageDirection = ToClient,
    override val metadata: SdkMetadata = Metadata(),
    override val author: SdkAuthor? = ToClient.toPerson(),
    override val attachments: Iterable<Attachment> = emptyList(),
    override val fallbackText: String? = null,
) : SdkRichLink()

internal typealias SdkRichLink = com.nice.cxonechat.message.Message.RichLink
internal typealias UiSdkRichLink = RichLink

internal data class Metadata(
    override val readAt: Date? = null,
    override val status: MessageStatus = Sent,
    override val seenAt: Date? = null,
    override val seenByCustomerAt: Date? = null,
) : SdkMetadata

internal typealias SdkMetadata = MessageMetadata
internal typealias UiSdkMetadata = Metadata

internal data class Media(
    override val fileName: String,
    override val url: String,
    override val mimeType: String,
) : SdkMedia

internal typealias SdkMedia = com.nice.cxonechat.message.Media

internal data class ReplyButton(
    override val text: String,
    override val media: Media? = null,
    override val postback: String? = null,
    override val description: String? = null,
) : SdkReplyButton {
    constructor(
        text: String,
        mediaUrl: String,
        mediaFileName: String = "filename",
        mediaMimeType: String = "unknown/unknown",
        postback: String? = null,
        description: String? = null,
    ) : this(
        text = text,
        media = Media(fileName = mediaFileName, url = mediaUrl, mimeType = mediaMimeType),
        postback = postback,
        description = description,
    )
}

internal typealias SdkReplyButton = Action.ReplyButton
internal typealias UiSdkReplyButton = ReplyButton
internal typealias SdkAttachment = Attachment
internal typealias SdkAction = Action
internal typealias SdkAuthor = MessageAuthor
