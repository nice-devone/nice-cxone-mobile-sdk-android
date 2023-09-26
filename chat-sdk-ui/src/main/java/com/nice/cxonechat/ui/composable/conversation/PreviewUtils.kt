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

package com.nice.cxonechat.ui.composable.conversation

import android.net.Uri
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import com.nice.cxonechat.message.Action
import com.nice.cxonechat.message.Attachment
import com.nice.cxonechat.message.Media
import com.nice.cxonechat.message.Message
import com.nice.cxonechat.message.MessageAuthor
import com.nice.cxonechat.message.MessageDirection
import com.nice.cxonechat.message.MessageDirection.ToAgent
import com.nice.cxonechat.message.MessageDirection.ToClient
import com.nice.cxonechat.message.MessageMetadata
import com.nice.cxonechat.ui.composable.conversation.model.ConversationUiState
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import java.util.Date
import java.util.UUID

// Shared preview methods

@Stable
internal fun previewTextMessage(
    text: String,
    direction: MessageDirection = ToAgent,
    createdAt: Date = Date(),
): Message.Text =
    PreviewTextMessage(
        direction = direction,
        author = if (direction == ToAgent) {
            PreviewAuthor(
                "Client",
                "Preview",
            )
        } else {
            PreviewAuthor(
                "Agent",
                "Preview",
            )
        },
        text = text,
        createdAt = createdAt,
    )

@Stable
internal data class PreviewTextMessage(
    override val direction: MessageDirection,
    override val author: MessageAuthor,
    override val text: String,
    override val fallbackText: String? = null,
    override val id: UUID = UUID.randomUUID(),
    override val threadId: UUID = UUID.randomUUID(),
    override val metadata: MessageMetadata = PreviewMetadata,
    override val createdAt: Date = Date(),
    override val attachments: Iterable<Attachment> = emptyList(),
) : Message.Text()

@Stable
internal data class PreviewRichLinkMessage(
    override val title: String,
    override val url: String,
    val mediaFileName: String,
    val mediaUrl: String,
    val mediaMimeType: String,
    override val direction: MessageDirection = ToClient,
    override val author: MessageAuthor = PreviewAuthor("FirstName", "LastName"),
    override val fallbackText: String? = null,
    override val id: UUID = UUID.randomUUID(),
    override val threadId: UUID = UUID.randomUUID(),
    override val metadata: MessageMetadata = PreviewMetadata,
    override val createdAt: Date = Date(),
    override val attachments: Iterable<Attachment> = emptyList(),
    override val media: Media = object : Media {
        override val fileName: String = mediaFileName
        override val url: String = mediaUrl
        override val mimeType: String = mediaMimeType
    },
) : Message.RichLink()

@Stable
internal data class PreviewAuthor(
    override val firstName: String,
    override val lastName: String,
    override val imageUrl: String? = null,
    override val id: String = UUID.randomUUID().toString(),
) : MessageAuthor()

@Stable
internal fun previewAudioState(): AudioRecordingUiState {
    val isRecordingFlow = MutableStateFlow(true)
    return AudioRecordingUiState(
        uriFlow = MutableStateFlow(Uri.EMPTY),
        onDismiss = { },
        onApprove = { },
        onAudioRecordToggle = {
            isRecordingFlow.value = !isRecordingFlow.value
            isRecordingFlow.value
        },
        isRecordingFlow = isRecordingFlow
    )
}

@Stable
internal data class PreviewReplyButton(
    private val previewText: String,
    private val mediaUrl: String? = null,
) : Action.ReplyButton {
    override val text = previewText
    override val postback: String? = null
    override val media: Media? = mediaUrl?.let {
        object : Media {
            override val fileName: String = "filename"
            override val url: String = it
            override val mimeType: String = "unknown/unknown"
        }
    }
    override val description: String? = null
}

@Immutable
internal object PreviewMetadata : MessageMetadata {
    override val readAt: Date? = null
}

@Composable
internal fun PreviewMessageItemBase(
    content: @Composable LazyItemScope.() -> Unit,
) {
    ChatTheme {
        Surface {
            LazyColumn {
                item {
                    content()
                }
            }
        }
    }
}

@Stable
internal fun previewUiState(messages: List<Message> = emptyList()) = ConversationUiState(
    threadName = flowOf("Preview Thread"),
    sdkMessages = MutableStateFlow(messages),
    typingIndicator = flowOf(true),
    sendMessage = {},
    loadMore = {},
    canLoadMore = MutableStateFlow(true),
    onStartTyping = {},
    onStopTyping = {},
)
