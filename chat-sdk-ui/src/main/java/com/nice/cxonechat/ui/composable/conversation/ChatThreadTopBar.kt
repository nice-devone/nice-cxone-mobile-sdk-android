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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons.Filled
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nice.cxonechat.thread.ChatThreadState
import com.nice.cxonechat.ui.R
import com.nice.cxonechat.ui.composable.conversation.model.ConversationTopBarState
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import com.nice.cxonechat.ui.composable.theme.Scaffold
import com.nice.cxonechat.ui.composable.theme.TextField
import com.nice.cxonechat.ui.composable.theme.TopBar
import kotlinx.coroutines.flow.MutableStateFlow

@Composable
internal fun ChatThreadTopBar(
    conversationState: ConversationTopBarState,
    onEditThreadName: () -> Unit,
    onEditThreadValues: () -> Unit,
    onEndContact: () -> Unit,
    displayEndConversation: () -> Unit,
) {
    ChatTheme.TopBar(
        title = conversationState.threadName
            .collectAsState(null)
            .value
            ?.ifBlank { null }
            ?: stringResource(id = R.string.thread_list_title),
        actions = {
            if (conversationState.isMultiThreaded) {
                IconButton(onClick = onEditThreadName) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_baseline_chat_24),
                        contentDescription = stringResource(id = R.string.change_thread_name)
                    )
                }
            }
            if (conversationState.hasQuestions) {
                IconButton(onClick = onEditThreadValues) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_baseline_edit),
                        contentDescription = stringResource(id = R.string.change_details_label)
                    )
                }
            }
            if (conversationState.isLiveChat) {
                if (conversationState.isArchived.collectAsState().value) {
                    IconButton(onClick = displayEndConversation) {
                        Icon(
                            painter = rememberVectorPainter(image = Filled.MoreVert),
                            contentDescription = stringResource(id = R.string.livechat_conversation_options)
                        )
                    }
                } else {
                    val threadState by conversationState.threadState.collectAsState()
                    IconButton(
                        onClick = onEndContact,
                        enabled = threadState == ChatThreadState.Ready,
                        colors = IconButtonDefaults.iconButtonColors().copy(contentColor = ChatTheme.colorScheme.error)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_baseline_cancel_24),
                            contentDescription = stringResource(id = R.string.action_end_conversation)
                        )
                    }
                }
            }
        }
    )
}

@Composable
@Preview
@Suppress(
    "LongMethod"
)
private fun PreviewChatThreadTopBar() {
    val threadNameFlow: MutableStateFlow<String?> = remember { MutableStateFlow(null) }
    val threadName by threadNameFlow.collectAsState()
    val isArchivedFlow: MutableStateFlow<Boolean> = remember { MutableStateFlow(false) }
    val isArchived by isArchivedFlow.collectAsState()
    val conversationStateFlow = remember { MutableStateFlow(ChatThreadState.Pending) }
    val conversationState by conversationStateFlow.collectAsState()
    val isMultiThreaded = remember { mutableStateOf(true) }
    val hasQuestions = remember { mutableStateOf(true) }
    val isLiveChat = remember { mutableStateOf(true) }
    ChatTheme {
        ChatTheme.Scaffold(
            topBar = {
                ChatThreadTopBar(
                    conversationState = ConversationTopBarState(
                        threadName = threadNameFlow,
                        isMultiThreaded = isMultiThreaded.value,
                        hasQuestions = hasQuestions.value,
                        isLiveChat = isLiveChat.value,
                        isArchived = isArchivedFlow,
                        threadState = conversationStateFlow,
                    ),
                    onEditThreadName = {},
                    onEditThreadValues = {},
                    onEndContact = {},
                    displayEndConversation = {}
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .padding(paddingValues)
                    .then(Modifier.padding(8.dp)),
                horizontalAlignment = Alignment.Start,
            ) {
                ChatTheme.TextField(
                    label = "Thread name",
                    value = threadName.orEmpty(),
                    modifier = Modifier.fillMaxWidth(),
                ) { name -> threadNameFlow.value = name }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(
                        checked = isArchived,
                        onCheckedChange = { isArchivedFlow.value = it }
                    )
                    Text("Archived")
                }
                PreviewSwitch(isMultiThreaded, "Multi-threaded")
                PreviewSwitch(hasQuestions, "Has questions")
                PreviewSwitch(isLiveChat, "Live chat")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(
                        checked = conversationState == ChatThreadState.Ready,
                        onCheckedChange = {
                            conversationStateFlow.value = if (!it) ChatThreadState.Closed else ChatThreadState.Ready
                        }
                    )
                    Text("Chat Thread Ready")
                }
            }
        }
    }
}

@Composable
private fun PreviewSwitch(checked: MutableState<Boolean>, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Switch(
            checked = checked.value,
            onCheckedChange = { checked.value = it }
        )
        Text(label)
    }
}
