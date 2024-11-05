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

package com.nice.cxonechat.ui.composable

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.lifecycle.Lifecycle.State
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.nice.cxonechat.message.Attachment
import com.nice.cxonechat.ui.SelectAttachmentActivityLauncher
import com.nice.cxonechat.ui.composable.conversation.AudioRecordingUiState
import com.nice.cxonechat.ui.composable.conversation.ChatConversation
import com.nice.cxonechat.ui.composable.conversation.DialogView
import com.nice.cxonechat.ui.composable.conversation.model.ConversationUiState
import com.nice.cxonechat.ui.main.AudioRecordingViewModel
import com.nice.cxonechat.ui.main.ChatThreadViewModel
import com.nice.cxonechat.ui.main.ChatViewModel
import com.nice.cxonechat.ui.util.repeatOnOwnerLifecycle
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import java.util.UUID

@Composable
@OptIn(ExperimentalComposeUiApi::class)
internal fun ThreadContentView(
    onAttachmentClicked: (Attachment) -> Unit,
    onShare: (Collection<Attachment>) -> Unit,
    closeChat: () -> Unit,
    onDismissRecording: () -> Unit,
    onTriggerRecording: suspend () -> Boolean,
    chatThreadViewModel: ChatThreadViewModel,
    chatViewModel: ChatViewModel,
    audioViewModel: AudioRecordingViewModel,
    snackbarHostState: SnackbarHostState,
    activityLauncher: SelectAttachmentActivityLauncher,
) {
    val owner = LocalLifecycleOwner.current
    LaunchedEffect(chatThreadViewModel, owner) {
        refreshThreadOnResume(chatThreadViewModel, owner)
    }
    ChatConversation(
        conversationState = uiState(chatThreadViewModel, onShare, onAttachmentClicked),
        audioRecordingState = audioState(
            chatThreadViewModel,
            audioViewModel,
            onDismissRecording,
            onTriggerRecording,
        ),
        onAttachmentTypeSelection = {
            activityLauncher.getDocument(it.toTypedArray())
        },
        modifier = Modifier.semantics {
            testTagsAsResourceId = true // Enabled for UI test automation
        }
    )
    DialogView(
        onAttachmentClicked = onAttachmentClicked,
        onShare = onShare,
        closeChat = closeChat,
        threadViewModel = chatThreadViewModel,
        chatModel = chatViewModel,
    )
    CustomPopUpView(snackbarHostState, chatThreadViewModel)
}

private fun refreshThreadOnResume(chatThreadViewModel: ChatThreadViewModel, lifecycleOwner: LifecycleOwner) {
    val threadIdFlow = chatThreadViewModel.chatThreadHandler.map { it.get().id }
    val refreshFlow = lifecycleOwner.lifecycle.currentStateFlow
        .combine(threadIdFlow) { state, id -> state to id }
        .distinctUntilChanged { old, new -> old.first === new.first && old.second == new.second }
        .filter { (state, id) -> state === State.RESUMED && id !== NIL_UUID }
        .distinctUntilChanged()
    lifecycleOwner.repeatOnOwnerLifecycle {
        refreshFlow.collect {
            chatThreadViewModel.refresh()
        }
    }
}

private fun uiState(
    chatThreadViewModel: ChatThreadViewModel,
    onShare: (Collection<Attachment>) -> Unit,
    onAttachmentClicked: (Attachment) -> Unit,
) = ConversationUiState(
    sdkMessages = chatThreadViewModel.messages,
    typingIndicator = chatThreadViewModel.agentState,
    positionInQueue = chatThreadViewModel.positionInQueue,
    sendMessage = chatThreadViewModel::sendMessage,
    loadMore = chatThreadViewModel::loadMore,
    canLoadMore = chatThreadViewModel.canLoadMore,
    onStartTyping = {
        chatThreadViewModel.reportThreadRead()
        chatThreadViewModel.reportTypingStarted()
    },
    onStopTyping = chatThreadViewModel::reportTypingEnd,
    onAttachmentClicked = onAttachmentClicked,
    onMoreClicked = chatThreadViewModel::selectAttachments,
    onShare = onShare,
    isArchived = chatThreadViewModel.isArchived,
    isLiveChat = chatThreadViewModel.isLiveChat,
)

private fun audioState(
    chatThreadViewModel: ChatThreadViewModel,
    audioViewModel: AudioRecordingViewModel,
    onDismiss: () -> Unit,
    onTriggerRecording: suspend () -> Boolean,
) = AudioRecordingUiState(
    uriFlow = audioViewModel.recordedUriFlow,
    isRecordingFlow = audioViewModel.recordingFlow,
    onDismiss = onDismiss,
    onApprove = chatThreadViewModel::sendAttachment,
    onAudioRecordToggle = onTriggerRecording,
)

private val NIL_UUID = UUID(0, 0)
