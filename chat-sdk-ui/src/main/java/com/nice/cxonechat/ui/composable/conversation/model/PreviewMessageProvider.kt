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

package com.nice.cxonechat.ui.composable.conversation.model

import androidx.compose.runtime.Stable
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.nice.cxonechat.message.Action
import com.nice.cxonechat.message.Attachment
import com.nice.cxonechat.message.MessageDirection
import com.nice.cxonechat.message.MessageDirection.ToClient
import com.nice.cxonechat.message.MessageStatus
import com.nice.cxonechat.message.MessageStatus.Sent
import com.nice.cxonechat.ui.composable.conversation.model.Message.ListPicker
import com.nice.cxonechat.ui.composable.conversation.model.Message.QuickReply
import com.nice.cxonechat.ui.composable.conversation.model.Message.RichLink
import com.nice.cxonechat.ui.composable.conversation.model.Message.Text
import com.nice.cxonechat.ui.composable.conversation.model.Message.Unsupported
import com.nice.cxonechat.ui.util.DateProvider
import java.util.Date
import java.util.UUID
import com.nice.cxonechat.message.Action.ReplyButton as SdkReplyButton
import com.nice.cxonechat.message.Media as SdkMedia
import com.nice.cxonechat.message.Message as SdkMessage
import com.nice.cxonechat.message.Message.ListPicker as SdkListPicker
import com.nice.cxonechat.message.Message.QuickReplies as SdkQuickReply
import com.nice.cxonechat.message.Message.RichLink as SdkRichLink
import com.nice.cxonechat.message.Message.Text as SdkText
import com.nice.cxonechat.message.MessageAuthor as SdkAuthor
import com.nice.cxonechat.message.MessageMetadata as SdkMetadata

@Suppress("LongParameterList")
internal class PreviewMessageProvider: PreviewParameterProvider<Message> {
    val messages: Sequence<SdkMessage>
        get() = sequenceOf(
            Text("Text 1"),
            ListPicker(),
            QuickReply(),
            RichLink(),
        )

    override val values = messages.map { it.asMessage() }

    private fun SdkMessage.asMessage() = when(this) {
        is SdkText -> Text(this)
        is SdkListPicker -> ListPicker(this) {}
        is SdkQuickReply -> QuickReply(this) {}
        is SdkRichLink -> RichLink(this)
        else -> Unsupported(this)
    }

    internal data class Metadata(
        override val readAt: Date? = null,
        override val status: MessageStatus = Sent,
        override val seenAt: Date? = null,
    ) : SdkMetadata

    internal data class Author(
        override val id: String = "",
        override val firstName: String = "firstName",
        override val lastName: String = "lastName",
        override val imageUrl: String? = null
    ) : SdkAuthor()

    internal data class Media(
        override val fileName: String,
        override val url: String,
        override val mimeType: String,
    ) : SdkMedia

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
            media = Media(mediaFileName, mediaUrl, mediaMimeType),
            postback = postback,
            description = description,
        )
    }

    companion object {
        private var nextDate = Date().time

        init {
            DateProvider.now = {
                Date(nextDate.also { nextDate += 1 })
            }
        }

        fun MessageDirection.toAuthor(id: String = "", imageUrl: String? = null) = Author(
            id = id,
            firstName = if (this === ToClient) "Agent" else "Customer",
            lastName = "Preview",
            imageUrl = imageUrl
        )
    }

    @Stable
    internal data class Text(
        override val text: String = "Text Message",
        override val attachments: Iterable<Attachment> = sequenceOf<Attachment>().asIterable(),
        override val id: UUID = UUID.randomUUID(),
        override val threadId: UUID = UUID.randomUUID(),
        override val createdAt: Date = DateProvider.now(),
        override val direction: MessageDirection = ToClient,
        override val author: SdkAuthor? = ToClient.toAuthor(),
        override val metadata: SdkMetadata = Metadata(),
        override val fallbackText: String? = null,
    ) : SdkText()

    @Stable
    internal data class ListPicker(
        override val title: String = "List Picker",
        override val text: String = "List Picker Description",
        override val actions: Iterable<Action> = listOf(
            ReplyButton("Some text"),
            ReplyButton("Random cat", mediaUrl = "https://http.cat/203")
        ),
        override val id: UUID = UUID.randomUUID(),
        override val threadId: UUID = UUID.randomUUID(),
        override val createdAt: Date = DateProvider.now(),
        override val direction: MessageDirection = ToClient,
        override val author: SdkAuthor? = ToClient.toAuthor(),
        override val metadata: SdkMetadata = Metadata(),
        override val attachments: Iterable<Attachment> = listOf(),
        override val fallbackText: String? = null,
    ) : SdkListPicker()

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
        override val author: SdkAuthor? = ToClient.toAuthor(),
        override val attachments: Iterable<Attachment> = emptyList(),
        override val fallbackText: String? = null,
    ) : SdkQuickReply()

    @Stable
    internal data class RichLink(
        override val title: String = "Rich Link",
        override val url: String = "https://nice.com",
        override val media: Media = Media(
            fileName = "https://thecatapi.com/api/images/get?format=src&type=jpeg",
            url = "image/jpeg",
            mimeType = "Preview Image",
        ),
        override val id: UUID = UUID.randomUUID(),
        override val threadId: UUID = UUID.randomUUID(),
        override val createdAt: Date = Date(),
        override val direction: MessageDirection = ToClient,
        override val metadata: SdkMetadata = Metadata(),
        override val author: SdkAuthor? = ToClient.toAuthor(),
        override val attachments: Iterable<Attachment> = emptyList(),
        override val fallbackText: String? = null,
    ) : SdkRichLink()
}
