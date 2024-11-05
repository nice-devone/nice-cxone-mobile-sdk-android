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

package com.nice.cxonechat.ui.composable.conversation

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons.Filled
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nice.cxonechat.ui.R.string
import com.nice.cxonechat.ui.composable.conversation.model.ConversationUiState
import com.nice.cxonechat.ui.composable.conversation.model.PreviewMessageProvider
import com.nice.cxonechat.ui.composable.conversation.model.Section
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import com.nice.cxonechat.ui.composable.theme.Scaffold
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Displays [ConversationUiState] messages an input for sending of new messages to the conversation using
 * the [ConversationUiState.sendMessage].
 *
 * @param conversationState State of the conversation and means how to send new messages.
 * @param audioRecordingState State of the audio recording and means how to trigger it.
 * @param onAttachmentTypeSelection Action invoked when a user has selected what type of file they want to send as attachment.
 * @param modifier Optional [Modifier] for [Scaffold] surrounding the conversation view.
 */
@Composable
internal fun ChatConversation(
    conversationState: ConversationUiState,
    audioRecordingState: AudioRecordingUiState,
    onAttachmentTypeSelection: (mimeType: Collection<String>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val messages = conversationState.messages(context).collectAsStateWithLifecycle(initialValue = emptyList()).value

    LaunchedEffect(messages) {
        if (scrollState.firstVisibleItemIndex <= 1) { // Only autoscroll if user is on last message
            delay(250)
            scrollState.scrollToItem(0)
        }
    }

    Column(
        modifier.fillMaxSize(),
    ) {
        MessageListView(
            messages,
            conversation = conversationState,
            scrollState = scrollState,
            modifier = Modifier.weight(1f)
        )
        UserInputView(
            conversationState = conversationState,
            scope = scope,
            scrollState = scrollState,
            audioRecordingState = audioRecordingState,
            onAttachmentTypeSelection = onAttachmentTypeSelection,
        )
    }
}

@Composable
private fun UserInputView(
    conversationState: ConversationUiState,
    scope: CoroutineScope,
    scrollState: LazyListState,
    audioRecordingState: AudioRecordingUiState,
    onAttachmentTypeSelection: (mimeTypes: Collection<String>) -> Unit,
) {
    if (!conversationState.isArchived.collectAsState().value) {
        UserInput(
            conversationUiState = conversationState,
            resetScroll = {
                scope.launch {
                    scrollState.scrollToItem(0)
                }
            },
            modifier = Modifier
                .navigationBarsPadding()
                .imePadding(),
            audioRecordingUiState = audioRecordingState,
            onAttachmentTypeSelection = onAttachmentTypeSelection,
        )
    } else {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .background(ChatTheme.chatColors.chatInfoLabel.background)
        ) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(4.dp)
            ) {
                Icon(
                    imageVector = Filled.Archive,
                    contentDescription = null,
                    tint = ChatTheme.chatColors.chatInfoLabel.foreground
                )
                Text(
                    text = stringResource(
                        if (conversationState.isLiveChat) string.label_livechat_thread_archived else string.label_thread_archived
                    ),
                    color = ChatTheme.chatColors.chatInfoLabel.foreground
                )
            }
        }
    }
}

@Composable
internal fun MessageListView(
    messages: List<Section>,
    conversation: ConversationUiState,
    scrollState: LazyListState,
    modifier: Modifier = Modifier,
) {
    val isTyping = conversation.typingIndicator.collectAsState(initial = false).value
    val canLoadMore = conversation.canLoadMore.collectAsState().value

    Box(modifier) {
        Column {
            Messages(
                scrollState = scrollState,
                groupedMessages = messages,
                loadMore = conversation.loadMore,
                canLoadMore = canLoadMore,
                onAttachmentClicked = conversation.onAttachmentClicked,
                onMoreClicked = conversation.onMoreClicked,
                onShare = conversation.onShare,
            )
            TypingIndicator(isTyping)
        }

        conversation.positionInQueue.collectAsState(initial = null).value?.let {
            PositionInQueue(
                position = it,
                modifier = Modifier
                    .fillMaxWidth(1f)
                    .padding(top = 8.dp)
            )
        }
    }
}

@Composable
private fun ColumnScope.TypingIndicator(isTyping: Boolean) {
    if (isTyping) {
        Text(
            modifier = Modifier
                .padding(ChatTheme.space.small)
                .align(Alignment.End),
            text = stringResource(string.text_agent_typing)
        )
    }
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
private fun PreviewChat() {
    ChatTheme(true) {
        val scrollState = rememberLazyListState()
        val messages = PreviewMessageProvider().messages.toList()
        val conversation = previewUiState(messages, positionInQueue = 4)
        val context = LocalContext.current
        MessageListView(
            messages = conversation.messages(context).collectAsState(initial = emptyList()).value,
            scrollState = scrollState,
            conversation = conversation
        )
    }
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewChatMessageInput() {
    val messages = PreviewMessageProvider().messages.toList()
    ChatTheme(darkTheme = true) {
        ChatConversation(
            conversationState = previewUiState(messages, positionInQueue = 4),
            audioRecordingState = previewAudioState(),
            onAttachmentTypeSelection = {},
        )
    }
}
