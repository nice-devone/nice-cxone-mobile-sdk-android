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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Scaffold
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.nice.cxonechat.message.Message
import com.nice.cxonechat.message.MessageDirection.ToClient
import com.nice.cxonechat.ui.R.drawable
import com.nice.cxonechat.ui.R.string
import com.nice.cxonechat.ui.composable.conversation.model.ConversationUiState
import com.nice.cxonechat.ui.composable.conversation.model.Section
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import com.nice.cxonechat.ui.composable.theme.Scaffold
import com.nice.cxonechat.ui.composable.theme.TopBar
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date

/**
 * Displays [ConversationUiState] messages an input for sending of new messages to the conversation using
 * the [ConversationUiState.sendMessage].
 *
 * @param conversationState State of the conversation and means how to send new messages.
 * @param audioRecordingState State of the audio recording and means how to trigger it.
 * @param onAttachmentTypeSelection Action invoked when a user has selected what type of file they want to send as attachment.
 * @param onEditThreadName Callback to trigger edit thread name dialog.
 * @param onEditThreadValues Callback to trigger edit thread values dialog.
 * @param modifier Optional [Modifier] for [Scaffold] surrounding the conversation view.
 */
@Composable
internal fun ChatConversation(
    conversationState: ConversationUiState,
    audioRecordingState: AudioRecordingUiState,
    onAttachmentTypeSelection: (mimeType: String) -> Unit,
    onEditThreadName: () -> Unit,
    onEditThreadValues: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val messages = conversationState.messages(context).collectAsState(initial = emptyList()).value

    LaunchedEffect(messages) {
        if (scrollState.firstVisibleItemIndex <= 1) { // Only autoscroll if user is on last message
            delay(250)
            scrollState.scrollToItem(0)
        }
    }

    ChatTheme.Scaffold(
        topBar = {
            ChatThreadTopBar(
                conversationState = conversationState,
                onEditThreadName = onEditThreadName,
                onEditThreadValues = onEditThreadValues,
            )
        }
    ) {
        Column(
            modifier.fillMaxSize(),
        ) {
            MessageListView(
                messages,
                conversation = conversationState,
                scrollState = scrollState,
                modifier = Modifier.weight(1f)
            )
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
        }
    }
}

@Composable
private fun ChatThreadTopBar(
    conversationState: ConversationUiState,
    onEditThreadName: () -> Unit,
    onEditThreadValues: () -> Unit,
) {
    ChatTheme.TopBar(
        title = conversationState.threadName.collectAsState(null).value?.ifBlank { null }
            ?: stringResource(id = string.thread_list_title),
        actions = {
            if (conversationState.isMultiThreaded) {
                IconButton(onClick = onEditThreadName) {
                    Icon(
                        painter = painterResource(id = drawable.ic_baseline_chat_24),
                        contentDescription = stringResource(id = string.change_thread_name)
                    )
                }
            }
            if (conversationState.hasQuestions) {
                IconButton(onClick = onEditThreadValues) {
                    Icon(
                        painter = painterResource(id = drawable.ic_baseline_edit),
                        contentDescription = stringResource(id = string.change_details_label)
                    )
                }
            }
        }
    )
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

    Surface(modifier) {
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

@Preview(showBackground = true)
@Composable
private fun PreviewChat() {
    ChatTheme(true) {
        val scrollState = rememberLazyListState()
        val firstDate = simpleDate(2022, 2, 12)
        val secondDate = simpleDate(2023, 1, 13)
        val messages = listOf(
            previewTextMessage("Hello", createdAt = firstDate),
            previewTextMessage("Hello again", createdAt = firstDate),
            previewTextMessage("Is anyone there?", createdAt = firstDate),
            previewTextMessage("Hi, how are you?", direction = ToClient, createdAt = secondDate),
            previewTextMessage("Hi, how are you, again?", direction = ToClient, createdAt = secondDate),
        ).sortedByDescending(Message::createdAt)
        val conversation = previewUiState(messages)
        val context = LocalContext.current
        MessageListView(
            messages = conversation.messages(context).collectAsState(initial = emptyList()).value,
            scrollState = scrollState,
            conversation = conversation
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewChatMessageInput() {
    val messages = listOf(previewTextMessage("Hello"))
    ChatConversation(
        conversationState = previewUiState(messages),
        audioRecordingState = previewAudioState(),
        onAttachmentTypeSelection = {},
        onEditThreadName = {},
        onEditThreadValues = {},
    )
}

private fun simpleDate(
    year: Int,
    month: Int,
    date: Int,
): Date {
    val cal = Calendar.getInstance()
    cal.set(year, month, date)
    return cal.time
}
