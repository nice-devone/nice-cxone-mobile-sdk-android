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

package com.nice.cxonechat.ui.composable.conversation

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons.Default
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults.enterAlwaysScrollBehavior
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.LoremIpsum
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nice.cxonechat.thread.ChatThreadState
import com.nice.cxonechat.ui.R.string
import com.nice.cxonechat.ui.composable.conversation.model.ConversationTopBarState
import com.nice.cxonechat.ui.composable.generic.IconMenuItem
import com.nice.cxonechat.ui.composable.theme.BackButton
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import com.nice.cxonechat.ui.composable.theme.MediumTopBar
import com.nice.cxonechat.ui.composable.theme.Scaffold
import com.nice.cxonechat.ui.composable.theme.TextField
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ChatThreadTopBar(
    scrollBehavior: TopAppBarScrollBehavior?,
    conversationState: ConversationTopBarState,
    onEditThreadName: () -> Unit,
    onEditThreadValues: () -> Unit,
    onEndContact: () -> Unit,
    displayEndConversation: () -> Unit,
    navigationIcon: @Composable () -> Unit = {},
) {
    val threadName: ThreadName? by conversationState.threadName.collectAsStateWithLifecycle(null)
    ChatTheme.MediumTopBar(
        modifier = Modifier.testTag("chat_thread_top_bar"),
        title = threadName.orDefaultThreadName(),
        scrollBehavior = scrollBehavior,
        navigationIcon = navigationIcon,
        actions = {
            Actions(
                conversationState,
                onEditThreadName,
                onEditThreadValues,
                onEndContact,
                displayEndConversation
            )
        },
    )
}

@Composable
private fun Actions(
    conversationState: ConversationTopBarState,
    onEditThreadName: () -> Unit,
    onEditThreadValues: () -> Unit,
    onEndContact: () -> Unit,
    displayEndConversation: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val singleItem = remember(conversationState) {
        listOf(
            conversationState.hasQuestions,
            conversationState.isMultiThreaded,
            conversationState.isLiveChat,
        ).count { it }.let { it <= 1 }
    }
    AnimatedContent(singleItem) { state ->
        if (state) {
            SingleAction(
                conversationState,
                onEditThreadName,
                onEditThreadValues,
                onEndContact,
                displayEndConversation,
                modifier
            )
        } else {
            MultipleActions(
                conversationState,
                onEditThreadName,
                onEditThreadValues,
                onEndContact,
                displayEndConversation,
                modifier
            )
        }
    }
}

@Composable
private fun SingleAction(
    conversationState: ConversationTopBarState,
    onEditThreadName: () -> Unit,
    onEditThreadValues: () -> Unit,
    onEndContact: () -> Unit,
    displayEndConversation: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (conversationState.isMultiThreaded) {
        IconButton(
            modifier = Modifier
                .testTag("edit_thread_name_button")
                .then(modifier),
            onClick = remember { onEditThreadName }
        ) {
            ChatIcon()
        }
    } else if (conversationState.hasQuestions) {
        IconButton(
            modifier = Modifier
                .testTag("edit_thread_custom_values_button")
                .then(modifier),
            onClick = remember { onEditThreadValues }
        ) {
            EditIcon()
        }
    } else if (conversationState.isLiveChat) {
        if (conversationState.isArchived.collectAsState().value) {
            IconButton(
                modifier = Modifier
                    .testTag("show_end_conversation_dialog_button")
                    .then(modifier),
                onClick = remember { displayEndConversation }
            ) {
                MenuIcon()
            }
        } else {
            val threadState by conversationState.threadState.collectAsState()
            IconButton(
                modifier = Modifier
                    .testTag("end_conversation_button")
                    .then(modifier),
                onClick = onEndContact,
                enabled = threadState == ChatThreadState.Ready,
                colors = IconButtonDefaults.iconButtonColors(contentColor = ChatTheme.colorScheme.error)
            ) {
                EndConversationIconForMenu()
            }
        }
    }
}

@Composable
private fun ShowArchivedThreadMenu(displayEndConversation: () -> Unit) {
    IconMenuItem(
        text = string.livechat_conversation_options,
        onClick = remember { displayEndConversation },
        icon = {
            MenuIcon()
        },
        modifier = Modifier.testTag("show_archived_thread_menu_item"),
    )
}

@Composable
private fun EditThreadValuesMenu(onEditThreadValues: () -> Unit) {
    IconMenuItem(
        text = string.change_details_label,
        onClick = remember { onEditThreadValues },
        icon = {
            EditIcon()
        },
        modifier = Modifier.testTag("edit_thread_custom_values_menu_item"),
    )
}

@Composable
private fun ChangeThreadNameMenu(onEditThreadName: () -> Unit) {
    IconMenuItem(
        text = string.change_thread_name,
        onClick = remember { onEditThreadName },
        icon = {
            ChatIcon()
        },
        modifier = Modifier.testTag("change_thread_name_menu_item"),
    )
}

@Composable
private fun EndConversationMenu(
    threadState: State<ChatThreadState>,
    onClick: () -> Unit,
) {
    IconMenuItem(
        text = string.action_end_conversation,
        onClick = remember { onClick },
        enabled = threadState.value == ChatThreadState.Ready,
        {
            EndConversationIconForMenu()
        },
        modifier = Modifier.testTag("end_conversation_menu_item"),
    )
}

@Composable
private fun MultipleActions(
    conversationState: ConversationTopBarState,
    onEditThreadName: () -> Unit,
    onEditThreadValues: () -> Unit,
    onEndContact: () -> Unit,
    displayEndConversation: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val dismiss: () -> Unit = remember { { expanded = false } }
    IconButton(
        modifier = Modifier
            .testTag("chat_thread_top_bar_menu_button")
            .then(modifier),
        onClick = { expanded = true },
    ) {
        Icon(Default.MoreVert, contentDescription = stringResource(string.livechat_conversation_options))
    }
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = { expanded = false },
        modifier = Modifier.testTag("chat_thread_top_bar_menu"),
    ) {
        if (conversationState.isMultiThreaded) {
            ChangeThreadNameMenu {
                onEditThreadName()
                dismiss()
            }
        }
        if (conversationState.hasQuestions) {
            EditThreadValuesMenu {
                onEditThreadValues()
                dismiss()
            }
        }
        if (conversationState.isLiveChat) {
            if (conversationState.isArchived.collectAsState().value) {
                ShowArchivedThreadMenu {
                    displayEndConversation()
                    dismiss()
                }
            } else {
                EndConversationMenu(conversationState.threadState.collectAsState()) {
                    onEndContact()
                    dismiss()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@PreviewLightDark
@Suppress(
    "LongMethod"
)
private fun PreviewChatThreadTopBar(
    @PreviewParameter(LoremIpsum::class) content: String,
) {
    val threadNameState = rememberTextFieldState("Sample thread name")
    val isArchivedFlow: MutableStateFlow<Boolean> = remember { MutableStateFlow(false) }
    val isArchived by isArchivedFlow.collectAsState()
    val conversationStateFlow = remember { MutableStateFlow(ChatThreadState.Pending) }
    val conversationState by conversationStateFlow.collectAsState()
    val isMultiThreaded = remember { mutableStateOf(true) }
    val hasQuestions = remember { mutableStateOf(true) }
    val isLiveChat = remember { mutableStateOf(true) }
    val scrollBehavior = enterAlwaysScrollBehavior()
    ChatTheme {
        ChatTheme.Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = {
                ChatThreadTopBar(
                    scrollBehavior = scrollBehavior,
                    conversationState = ConversationTopBarState(
                        threadName = snapshotFlow { threadNameState.text as String },
                        isMultiThreaded = isMultiThreaded.value,
                        hasQuestions = hasQuestions.value,
                        isLiveChat = isLiveChat.value,
                        isArchived = isArchivedFlow.asStateFlow(),
                        threadState = conversationStateFlow,
                    ),
                    onEditThreadName = {},
                    onEditThreadValues = {},
                    onEndContact = {},
                    displayEndConversation = {},
                    navigationIcon = { BackButton {} }
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .padding(paddingValues)
                    .then(Modifier.padding(8.dp))
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.Start,
            ) {
                ChatTheme.TextField(
                    label = "Thread name",
                    value = threadNameState,
                    modifier = Modifier.fillMaxWidth(),
                )
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
                HorizontalDivider()
                Text(content)
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
