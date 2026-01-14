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

package com.nice.cxonechat.ui.composable.conversation

import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.nice.cxonechat.message.Attachment
import com.nice.cxonechat.message.Message
import com.nice.cxonechat.message.MessageDirection.ToAgent
import com.nice.cxonechat.ui.composable.conversation.MessageItemGroupState.SOLO
import com.nice.cxonechat.ui.composable.conversation.MessageStatusState.SELECTABLE
import com.nice.cxonechat.ui.composable.conversation.model.ConversationUiState
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import com.nice.cxonechat.ui.composable.theme.ChatTheme.chatColors
import com.nice.cxonechat.ui.domain.model.Person
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlin.time.Duration
import com.nice.cxonechat.ui.composable.conversation.model.Message as UiMessage

// Shared preview methods

@Stable
internal fun previewAudioState(): AudioRecordingUiState {
    val isRecordingFlow = MutableStateFlow(true)
    val durationFlow = MutableStateFlow(Duration.ZERO)
    return AudioRecordingUiState(
        isRecordingAllowedFlow = MutableStateFlow(true),
        uriFlow = MutableStateFlow(Uri.EMPTY),
        onDismiss = { },
        onApprove = { },
        onAudioRecordToggle = {
            isRecordingFlow.value = !isRecordingFlow.value
            isRecordingFlow.value
        },
        isRecordingFlow = isRecordingFlow,
        durationFlow = durationFlow,
    )
}

internal object PreviewAttachments {
    const val JPEG_MIME_TYPE = "image/jpeg"
    val image = object : Attachment {
        override val url: String = "https://http.cat/203"
        override val friendlyName: String = "A with some very looong filename.jpeg"
        override val mimeType: String = JPEG_MIME_TYPE
    }

    val image2 = object : Attachment {
        override val url: String = "https://cataas.com/cat"
        override val friendlyName: String = "cat_no_content2.jpeg"
        override val mimeType: String = JPEG_MIME_TYPE
    }

    val movie = object : Attachment {
        override val url: String = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"
        override val friendlyName: String = "example.webm"
        override val mimeType: String = "video/mp4"
    }

    val sound = object : Attachment {
        override val url: String = "https://http.cat/204"
        override val friendlyName: String = "cat_no_content.mp3"
        override val mimeType: String = "audio/mp3"
    }

    val pdf = object : Attachment {
        override val url: String = "https://www.w3.org/WAI/ER/tests/xhtml/testfiles/resources/pdf/dummy.pdf"
        override val friendlyName: String = "dummy.pdf"
        override val mimeType: String = "application/pdf"
    }

    val txt = object : Attachment {
        override val url: String = "https://www.nice.com/robots.txt"
        override val friendlyName: String = "robots.txt"
        override val mimeType: String = "text/plain"
    }

    val evil = object : Attachment {
        override val url: String = "https://developer.mozilla.org/en-US/docs/Web/HTTP/Guides/MIME_types"
        override val friendlyName: String = "evil"
        override val mimeType: String? = null
    }

    val choices = listOf(image, image2, movie, sound, pdf, txt, evil)

    fun getAttachments(count: Int = 1): Sequence<Attachment> {
        var index = count
        return generateSequence {
            index--.takeIf { it >= 0 }?.let {
                SampleAttachment(
                    "https://cataas.com/cat/says/$it",
                    "random_cat_$it.jpeg",
                    JPEG_MIME_TYPE
                )
            }
        }
    }

    fun with(count: Int): Iterable<Attachment> = getAttachments(maxOf(0, count - choices.size)).toList() + choices.take(count)
}

private data class SampleAttachment(
    override val url: String,
    override val friendlyName: String,
    override val mimeType: String?,
) : Attachment

internal data class AttachmentProvider(
    override val values: Sequence<Attachment> = PreviewAttachments.choices.asSequence(),
) : PreviewParameterProvider<Attachment>

@Composable
internal fun PreviewMessageItemBase(
    message: UiMessage,
    itemGroupState: MessageItemGroupState = SOLO,
) {
    PreviewMessageItemBase {
        PreviewMessageItem(
            message = message,
            itemGroupState = itemGroupState,
        )
    }
}

@Composable
internal fun PreviewMessageItem(
    message: UiMessage,
    itemGroupState: MessageItemGroupState = SOLO,
    showStatus: DisplayStatus = if (message.direction === ToAgent) DisplayStatus.DISPLAY else DisplayStatus.HIDE,
    onAttachmentClicked: (Attachment) -> Unit = {},
    onMoreClicked: (List<Attachment>) -> Unit = { _ -> },
    onShare: (Collection<Attachment>) -> Unit = {},
) {
    MessageItem(
        message = message,
        showStatus = showStatus,
        itemGroupState = itemGroupState,
        onAttachmentClicked = onAttachmentClicked,
        onMoreClicked = onMoreClicked,
        onShare = onShare,
        modifier = Modifier.fillMaxWidth(),
        messageStatusState = SELECTABLE,
        onQuickReplyOptionSelected = {},
        snackBarHostState = SnackbarHostState(),
        onListPickerSelected = {},
    )
}

@Composable
internal fun PreviewMessageItemBase(
    content: @Composable ColumnScope.() -> Unit,
) {
    ChatTheme {
        Surface(
            modifier = Modifier.systemBarsPadding(),
            color = chatColors.token.background.default,
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 17.dp),
            ) {
                content()
            }
        }
    }
}

@Stable
@Suppress(
    "LongParameterList" // Preview function
)
internal fun previewUiState(
    messages: List<Message> = emptyList(),
    isArchived: Boolean = false,
    positionInQueue: Int? = null,
    isLiveChat: Boolean = true,
    pendingAttachments: List<Attachment> = emptyList(),
) = ConversationUiState(
    sdkMessages = MutableStateFlow(messages),
    agentTyping = MutableStateFlow(Person(firstName = "Some", lastName = "User")),
    positionInQueue = flowOf(positionInQueue),
    sendMessage = {},
    loadMore = {},
    canLoadMore = MutableStateFlow(true),
    isAgentTyping = MutableStateFlow(true),
    onStartTyping = {},
    onStopTyping = {},
    onAttachmentClicked = {},
    onMoreClicked = { _ -> },
    onShare = {},
    isArchived = MutableStateFlow(isArchived),
    isLiveChat = isLiveChat,
    pendingAttachments = MutableStateFlow(pendingAttachments),
    onRemovePendingAttachment = {}
)
