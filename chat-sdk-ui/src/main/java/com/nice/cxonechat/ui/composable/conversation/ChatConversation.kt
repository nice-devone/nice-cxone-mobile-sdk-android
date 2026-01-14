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

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons.Filled
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nice.cxonechat.ui.AttachmentType
import com.nice.cxonechat.ui.R.string
import com.nice.cxonechat.ui.composable.conversation.model.ConversationUiState
import com.nice.cxonechat.ui.composable.conversation.model.PreviewMessageProvider
import com.nice.cxonechat.ui.composable.conversation.model.Section
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import com.nice.cxonechat.ui.composable.theme.ChatTheme.space
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
 * @param onError Action invoked when an error occurs. The string parameter is a user friendly error message.
 * @param showMessageProcessing Whether to show the message processing indicator.
 * @param snackBarHostState The state of the snackbar host to show snackbars for errors and other information.
 */
@Composable
internal fun ChatConversation(
    conversationState: ConversationUiState,
    audioRecordingState: AudioRecordingUiState,
    onAttachmentTypeSelection: (attachmentType: AttachmentType) -> Unit,
    modifier: Modifier = Modifier,
    onError: (String) -> Unit,
    showMessageProcessing: Boolean,
    snackBarHostState: SnackbarHostState,
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
        modifier = Modifier
            .background(color = ChatTheme.chatColors.token.background.default)
            .testTag("chat_conversation_column")
            .then(modifier)
            .fillMaxSize(),
    ) {
        MessageListView(
            messages,
            conversation = conversationState,
            scrollState = scrollState,
            modifier = Modifier
                .testTag("message_list_view")
                .weight(1f),
            snackBarHostState = snackBarHostState
        )
        UserInputView(
            conversationState = conversationState,
            scope = scope,
            scrollState = scrollState,
            audioRecordingState = audioRecordingState,
            onAttachmentTypeSelection = onAttachmentTypeSelection,
            showMessageProcessing = showMessageProcessing,
            onError = onError
        )
    }
}

@Composable
private fun UserInputView(
    conversationState: ConversationUiState,
    scope: CoroutineScope,
    scrollState: LazyListState,
    audioRecordingState: AudioRecordingUiState,
    modifier: Modifier = Modifier,
    onAttachmentTypeSelection: (attachmentType: AttachmentType) -> Unit,
    onError: (String) -> Unit,
    showMessageProcessing: Boolean,
) {
    if (!conversationState.isArchived.collectAsState().value) {
        UserInput(
            conversationUiState = conversationState,
            audioRecordingUiState = audioRecordingState,
            onAttachmentTypeSelection = onAttachmentTypeSelection,
            resetScroll = {
                scope.launch {
                    scrollState.scrollToItem(0)
                }
            },
            modifier = Modifier
                .testTag("user_input")
                .then(modifier),
            showMessageProcessing = showMessageProcessing,
            onError = onError
        )
    } else {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .testTag("archived_info_box")
                .then(modifier)
                .fillMaxWidth()
                .background(ChatTheme.chatColors.token.background.surface.variant)
        ) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(4.dp)
                    .testTag("archived_info_row")
            ) {
                Icon(
                    imageVector = Filled.Archive,
                    contentDescription = null,
                    tint = ChatTheme.chatColors.token.content.primary,
                    modifier = Modifier
                        .testTag("archive_icon")
                        .padding(end = space.small)
                )
                Text(
                    text = stringResource(
                        if (conversationState.isLiveChat) string.label_livechat_thread_archived else string.label_thread_archived
                    ),
                    color = ChatTheme.chatColors.token.content.primary
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
    snackBarHostState: SnackbarHostState,
) {
    val canLoadMore = conversation.canLoadMore.collectAsState().value
    val agentDetails = conversation.agentTyping.collectAsState(null).value
    val agentIsTyping = conversation.isAgentTyping.collectAsState().value
    val positionInQueue by conversation.positionInQueue.collectAsState(initial = null)
    val showPositionInQueue by remember {
        derivedStateOf {
            positionInQueue != null && agentDetails == null
        }
    }
    Box(
        modifier = Modifier
            .testTag("message_list_view_box")
            .then(modifier)
    ) {
        Column(
            modifier = Modifier.testTag("message_list_column")
        ) {
            AnimatedVisibility(showPositionInQueue, Modifier.align(CenterHorizontally)) {
                positionInQueue?.let { position ->
                    PositionInQueue(position = position)
                }
            }
            Messages(
                scrollState = scrollState,
                groupedMessages = messages,
                loadMore = conversation.loadMore,
                canLoadMore = canLoadMore,
                agentIsTyping = agentIsTyping,
                agentDetails = agentDetails,
                onAttachmentClicked = conversation.onAttachmentClicked,
                onMoreClicked = conversation.onMoreClicked,
                onShare = conversation.onShare,
                snackBarHostState = snackBarHostState,
            )
        }
    }
}

@Preview(device = "spec:width=673dp,height=841dp")
@Composable
private fun PreviewChat() {
    ChatTheme {
        val scrollState = rememberLazyListState()
        val messages = PreviewMessageProvider().messages.toList()
        val conversation = previewUiState(messages, positionInQueue = 4)
        val context = LocalContext.current
        MessageListView(
            messages = conversation.messages(context).collectAsState(initial = emptyList()).value,
            scrollState = scrollState,
            conversation = conversation,
            snackBarHostState = SnackbarHostState(),
        )
    }
}

@PreviewLightDark
@Composable
private fun PreviewChatMessageInput() {
    val messages = PreviewMessageProvider().messages.toList()
    ChatTheme {
        Surface {
            ChatConversation(
                conversationState = previewUiState(messages, positionInQueue = 4),
                audioRecordingState = previewAudioState(),
                onAttachmentTypeSelection = {},
                showMessageProcessing = false,
                onError = {},
                snackBarHostState = SnackbarHostState(),
            )
        }
    }
}
