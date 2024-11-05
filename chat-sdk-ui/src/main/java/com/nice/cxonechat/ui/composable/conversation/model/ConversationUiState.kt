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

import android.content.Context
import androidx.compose.runtime.Stable
import com.nice.cxonechat.message.Attachment
import com.nice.cxonechat.message.OutboundMessage
import com.nice.cxonechat.ui.composable.conversation.model.Message.ListPicker
import com.nice.cxonechat.ui.composable.conversation.model.Message.QuickReply
import com.nice.cxonechat.ui.composable.conversation.model.Message.RichLink
import com.nice.cxonechat.ui.composable.conversation.model.Message.Text
import com.nice.cxonechat.ui.composable.conversation.model.Message.Unsupported
import com.nice.cxonechat.ui.composable.conversation.model.Message.WithAttachments
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import com.nice.cxonechat.message.Message as SdkMessage

/**
 * Captures the state of active conversation and also handling of various actions possible within
 * the conversation.
 *
 * @param sdkMessages Flow of messages for the active conversation, it is expected that the flow will be updated if
 * [sendMessage] is invoked.
 * @property typingIndicator Flow indicating that the agent handling the conversation is typing.
 * @property positionInQueue Flow of current position in queue.
 * @property sendMessage An action which will be invoked if the user wants to post a new message to the conversation, or
 * if he has interacted with an element which generates a message.
 * @property loadMore An action which will be called when more messages can be displayed/loaded.
 * @property canLoadMore Flow indicating if there are more messages to load.
 * @property onStartTyping An action which will be called when the user has started to type a text message.
 * @property onStopTyping An action which will be called (with some delay) when the user has stopped typing a text message.
 * @property onAttachmentClicked An action which handles when users clicks on an Attachment
 * @property onMoreClicked An action to take when the more button is clicked in an attachment preview.
 * @property onShare Action to take when share is selected via long press or attachment selection dialog.
 * @property isArchived Flow indicating if the thread was archived.
 * @property isLiveChat true iff the channel is configured as live chat.
 * @param backgroundDispatcher Optional dispatcher used for mapping of incoming messages off the main thread,
 * intended for testing.
 */
@Suppress(
    "LongParameterList", // POJO class
)
@Stable
internal data class ConversationUiState(
    private val sdkMessages: Flow<List<SdkMessage>>,
    internal val typingIndicator: Flow<Boolean>,
    internal val positionInQueue: Flow<Int?>,
    internal val sendMessage: (OutboundMessage) -> Unit,
    internal val loadMore: () -> Unit,
    internal val canLoadMore: StateFlow<Boolean>,
    internal val onStartTyping: () -> Unit,
    internal val onStopTyping: () -> Unit,
    internal val onAttachmentClicked: (Attachment) -> Unit,
    internal val onMoreClicked: (List<Attachment>, String) -> Unit,
    internal val onShare: (Collection<Attachment>) -> Unit,
    internal val isArchived: StateFlow<Boolean>,
    internal val isLiveChat: Boolean,
    private val backgroundDispatcher: CoroutineDispatcher = Dispatchers.Default,
) {
    @Stable
    internal fun messages(context: Context): Flow<List<Section>> = sdkMessages
        .map { messageList ->
            messageList
                .map { message -> message.toUiMessage() }
                .groupBy { message -> message.createdAtDate(context) }
                .entries
                .map(::Section)
        }
        .flowOn(backgroundDispatcher)

    @Stable
    private fun SdkMessage.toUiMessage(): Message = when (this) {
        is SdkMessage.Text ->
            if (attachments.firstOrNull() == null) {
                Text(message = this)
            } else {
                WithAttachments(this)
            }

        is SdkMessage.RichLink -> RichLink(this)
        is SdkMessage.ListPicker -> ListPicker(this, sendMessage)
        is SdkMessage.QuickReplies -> QuickReply(this, sendMessage)
        else -> Unsupported(this)
    }
}
