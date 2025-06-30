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

package com.nice.cxonechat.ui.composable

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle.State
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.nice.cxonechat.message.Attachment
import com.nice.cxonechat.thread.ChatThreadState
import com.nice.cxonechat.ui.SelectAttachmentActivityLauncher
import com.nice.cxonechat.ui.composable.conversation.AudioRecordingUiState
import com.nice.cxonechat.ui.composable.conversation.ChatConversation
import com.nice.cxonechat.ui.composable.conversation.ThreadDialogView
import com.nice.cxonechat.ui.composable.conversation.model.ConversationUiState
import com.nice.cxonechat.ui.util.repeatOnOwnerLifecycle
import com.nice.cxonechat.ui.viewmodel.AudioRecordingViewModel
import com.nice.cxonechat.ui.viewmodel.ChatThreadViewModel
import com.nice.cxonechat.ui.viewmodel.ChatViewModel
import com.nice.cxonechat.ui.viewmodel.ChatViewModel.DialogState.Preparing
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import java.util.UUID

@Composable
internal fun ThreadContentView(
    onAttachmentClicked: (Attachment) -> Unit,
    onShare: (Collection<Attachment>) -> Unit,
    closeChat: () -> Unit,
    onDismissRecording: () -> Unit,
    onTriggerRecording: suspend () -> Boolean,
    chatThreadViewModel: ChatThreadViewModel,
    chatViewModel: ChatViewModel,
    audioViewModel: AudioRecordingViewModel,
    activityLauncher: SelectAttachmentActivityLauncher,
) {
    val owner = LocalLifecycleOwner.current
    LaunchedEffect(chatThreadViewModel, owner) {
        refreshThreadOnResume(chatThreadViewModel, owner)
    }
    val dialogState = chatViewModel.dialogShown.collectAsState()
    val threadState by chatThreadViewModel.threadStateFlow.collectAsState()
    val messages by chatThreadViewModel.messages.collectAsState(emptyList())
    LaunchedEffect(threadState, messages) {
        if (threadState.ordinal >= ChatThreadState.Loaded.ordinal) chatThreadViewModel.reportThreadRead()
    }
    ChatConversation(
        conversationState = uiState(chatThreadViewModel, onShare, onAttachmentClicked),
        audioRecordingState = audioState(
            chatThreadViewModel,
            audioViewModel,
            onDismissRecording,
            onTriggerRecording,
        ),
        onAttachmentTypeSelection = remember {
            {
                activityLauncher.getAttachment(it)
            }
        },
        modifier = Modifier
            .blur(if (dialogState.value == Preparing) 4.dp else 0.dp)
            .semantics {
            testTagsAsResourceId = true // Enabled for UI test automation
        }
    )
    ThreadDialogView(
        onAttachmentClicked = onAttachmentClicked,
        onShare = onShare,
        closeChat = closeChat,
        threadViewModel = chatThreadViewModel,
        chatModel = chatViewModel,
    )
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
    agentTyping = chatThreadViewModel.currentAgent,
    positionInQueue = chatThreadViewModel.positionInQueue,
    sendMessage = chatThreadViewModel::sendMessageWithAttachments,
    loadMore = chatThreadViewModel::loadMore,
    canLoadMore = chatThreadViewModel.canLoadMore,
    isAgentTyping = chatThreadViewModel.isAgentTyping,
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
    pendingAttachments = chatThreadViewModel.pendingAttachments,
    onRemovePendingAttachment = chatThreadViewModel::removePendingAttachment,
)

private fun audioState(
    chatThreadViewModel: ChatThreadViewModel,
    audioViewModel: AudioRecordingViewModel,
    onDismiss: () -> Unit,
    onTriggerRecording: suspend () -> Boolean,
) = AudioRecordingUiState(
    isRecordingAllowedFlow = audioViewModel.isRecordingAllowed,
    uriFlow = audioViewModel.recordedUriFlow,
    isRecordingFlow = audioViewModel.recordingFlow,
    onDismiss = onDismiss,
    onApprove = chatThreadViewModel::sendAttachment,
    onAudioRecordToggle = onTriggerRecording,
    durationFlow = audioViewModel.recordDurationFlow,
)

private val NIL_UUID = UUID(0, 0)
