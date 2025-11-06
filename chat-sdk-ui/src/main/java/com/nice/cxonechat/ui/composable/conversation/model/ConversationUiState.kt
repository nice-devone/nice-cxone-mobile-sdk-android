/*
 * Copyright (c) 2021-2025. NICE Ltd. All rights reserved.
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

import android.content.Context
import androidx.annotation.OptIn
import androidx.compose.runtime.Stable
import androidx.core.net.toUri
import androidx.emoji2.text.EmojiCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import com.nice.cxonechat.message.Attachment
import com.nice.cxonechat.message.OutboundMessage
import com.nice.cxonechat.ui.composable.conversation.model.Message.AudioAttachment
import com.nice.cxonechat.ui.composable.conversation.model.Message.ListPicker
import com.nice.cxonechat.ui.composable.conversation.model.Message.QuickReply
import com.nice.cxonechat.ui.composable.conversation.model.Message.RichLink
import com.nice.cxonechat.ui.composable.conversation.model.Message.Text
import com.nice.cxonechat.ui.composable.conversation.model.Message.Unsupported
import com.nice.cxonechat.ui.composable.conversation.model.Message.WithAttachments
import com.nice.cxonechat.ui.domain.model.Person
import com.nice.cxonechat.ui.services.PlayerDownloadService
import com.nice.cxonechat.ui.util.emojiCount
import com.nice.cxonechat.ui.util.preview.message.SdkListPicker
import com.nice.cxonechat.ui.util.preview.message.SdkMessage
import com.nice.cxonechat.ui.util.preview.message.SdkQuickReply
import com.nice.cxonechat.ui.util.preview.message.SdkReplyButton
import com.nice.cxonechat.ui.util.preview.message.SdkRichLink
import com.nice.cxonechat.ui.util.preview.message.SdkText
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

/**
 * Captures the state of active conversation and also handling of various actions possible within
 * the conversation.
 *
 * @param sdkMessages Flow of messages for the active conversation, it is expected that the flow will be updated if
 * [sendMessage] is invoked.
 * @property agentTyping details of agent currently typing, if any.
 * @property pendingAttachments Flow of attachments that are ready to be sent.
 * @property positionInQueue Flow of current position in queue.
 * @property sendMessage An action which will be invoked if the user wants to post a new message to the conversation, or
 * if he has interacted with an element which generates a message.
 * @property loadMore An action which will be called when more messages can be displayed/loaded.
 * @property canLoadMore Flow indicating if there are more messages to load.
 * @property isAgentTyping Flow indicating if there is an agent typing.
 * @property onStartTyping An action which will be called when the user has started to type a text message.
 * @property onStopTyping An action which will be called (with some delay) when the user has stopped typing a text message.
 * @property onAttachmentClicked An action which handles when users clicks on an Attachment
 * @property onMoreClicked An action to take when the more button is clicked in an attachment preview.
 * @property onShare Action to take when share is selected via long press or attachment selection dialog.
 * @property isArchived Flow indicating if the thread was archived.
 * @property isLiveChat true iff the channel is configured as live chat.
 * @property onRemovePendingAttachment An action to remove a pending attachment.
 * @property onReplyButtonClicked An action to take when a reply button is clicked.
 * @param backgroundDispatcher Optional dispatcher used for mapping of incoming messages off the main thread,
 * intended for testing.
 */
@Suppress(
    "LongParameterList", // POJO class
)
@Stable
internal data class ConversationUiState(
    private val sdkMessages: Flow<List<SdkMessage>>,
    internal val agentTyping: Flow<Person?>,
    internal val pendingAttachments: StateFlow<List<Attachment>>,
    internal val positionInQueue: Flow<Int?>,
    internal val sendMessage: (OutboundMessage) -> Unit,
    internal val loadMore: () -> Unit,
    internal val canLoadMore: StateFlow<Boolean>,
    internal val isAgentTyping: StateFlow<Boolean>,
    internal val onStartTyping: () -> Unit,
    internal val onStopTyping: () -> Unit,
    internal val onAttachmentClicked: (Attachment) -> Unit,
    internal val onMoreClicked: (List<Attachment>) -> Unit,
    internal val onShare: (Collection<Attachment>) -> Unit,
    internal val isArchived: StateFlow<Boolean>,
    internal val isLiveChat: Boolean,
    internal val onRemovePendingAttachment: (Attachment) -> Unit,
    internal val onReplyButtonClicked: (SdkReplyButton) -> Unit = {},
    private val backgroundDispatcher: CoroutineDispatcher = Dispatchers.Default,
) {

    @Stable
    internal fun messages(context: Context): Flow<List<Section>> = sdkMessages
        .map { messageList ->
            messageList
                .flatMap { message -> message.toUiMessage() }
                .onEach {
                    if (it is AudioAttachment) {
                        startDownload(context, it.attachment.url)
                    }
                }
                .groupMessages(context)
                .entries
                .map(::Section)
        }
        .flowOn(backgroundDispatcher)

    @Stable
    private fun SdkMessage.toUiMessage(): Iterable<Message> = when (this) {
        is SdkText -> uiTextMessage()
        is SdkRichLink -> listOf(RichLink(this))
        is SdkListPicker -> listOf(ListPicker(this, onReplyButtonClicked))
        is SdkQuickReply -> listOf(QuickReply(this, onReplyButtonClicked))
        else -> listOf(Unsupported(this))
    }

    private fun SdkText.uiTextMessage(): List<Message> = if (attachments.firstOrNull() == null) {
        listOf(if (isEmojiMessage(this)) Message.EmojiText(this) else Text(this))
    } else {
        val attachmentGroups = this.attachments.groupBy {
            it.mimeType.orEmpty().startsWith("audio/")
        }.flatMap { (key, value) ->
            if (!key) {
                listOf(WithAttachments(this, value))
            } else {
                value.map { AudioAttachment(this, it) }
            }
        }.sortedBy {
            if (it is AudioAttachment) -1 else 0
        }
        listOfNotNull(
            Text(message = this).takeUnless { text.isEmpty() },
        ).plus(attachmentGroups)
    }

    private companion object {
        private const val TWO_MINUTES = 120_000L
        private const val EMOJI_TEXT_MAX_LENGTH = 3

        fun isEmojiMessage(message: SdkText): Boolean {
            val emoji = runCatching { EmojiCompat.get() }.getOrNull()
            val messageText = message.text
            return emoji != null &&
                    messageText.isNotBlank() &&
                    emoji.emojiCount(messageText, EMOJI_TEXT_MAX_LENGTH) in 1..EMOJI_TEXT_MAX_LENGTH
        }

        fun List<Message>.groupMessages(context: Context): Map<String, List<Message>> {
            if (isEmpty()) return emptyMap()

            val initial = mutableListOf(mutableListOf(first()))
            return drop(1)
                .fold(initial) { groups: MutableList<MutableList<Message>>, curr: Message ->
                    val lastGroup = groups.last()
                    val prev = lastGroup.last()
                    if (prev.createdAt.time - curr.createdAt.time <= TWO_MINUTES) {
                        lastGroup.add(curr)
                    } else {
                        groups.add(mutableListOf(curr))
                    }
                    groups
                }
                .associateBy { group -> group.minBy(Message::createdAt).createdAtDate(context) }
        }
    }
}

@OptIn(UnstableApi::class)
private fun startDownload(context: Context, url: String) {
    val mediaItem = MediaItem.fromUri(url.toUri())
    val downloadRequest = DownloadRequest.Builder(mediaItem.mediaId, mediaItem.localConfiguration!!.uri)
        .build()
    DownloadService.sendAddDownload(
        context,
        PlayerDownloadService::class.java,
        downloadRequest,
        false,
    )
}
