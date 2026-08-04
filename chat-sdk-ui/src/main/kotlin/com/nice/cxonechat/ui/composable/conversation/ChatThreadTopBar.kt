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
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons.Default
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults.enterAlwaysScrollBehavior
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
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
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.LoremIpsum
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nice.cxonechat.thread.ChatThreadState
import com.nice.cxonechat.ui.R.string
import com.nice.cxonechat.ui.composable.conversation.model.ConversationTopBarState
import com.nice.cxonechat.ui.composable.theme.BackButton
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import com.nice.cxonechat.ui.composable.theme.ChatTheme.chatColors
import com.nice.cxonechat.ui.composable.theme.ChatTheme.chatShapes
import com.nice.cxonechat.ui.composable.theme.ChatTheme.space
import com.nice.cxonechat.ui.composable.theme.MediumTopBar
import com.nice.cxonechat.ui.composable.theme.Scaffold
import com.nice.cxonechat.ui.composable.theme.TextField
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ChatThreadTopBar(
    scrollBehavior: TopAppBarScrollBehavior?,
    conversationState: ConversationTopBarState,
    onEditThreadName: () -> Unit,
    onEditThreadValues: () -> Unit,
    onEndContact: () -> Unit,
    displayEndConversation: () -> Unit,
    onSendTranscript: () -> Unit,
    navigationIcon: @Composable () -> Unit = {},
) {
    val threadName: ThreadName? by conversationState.threadName.collectAsStateWithLifecycle(null)
    val agentName: String? by conversationState.agentName.collectAsStateWithLifecycle(null)
    val hasAiMessages by conversationState.hasAiMessages.collectAsStateWithLifecycle()
    val aiSubtitle: @Composable () -> Unit = {
        AnimatedVisibility(
            visible = hasAiMessages,
            enter = fadeIn() + expandVertically(expandFrom = Alignment.Top),
            exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Top),
        ) {
            AiAuthorshipBadge(modifier = Modifier.padding(bottom = space.large))
        }
    }
    val hasAgent: Boolean by conversationState.hasAgent.collectAsStateWithLifecycle(false)
    val agentNameValue = agentName?.ifBlank { null }
        ?: if (hasAgent) stringResource(string.agent_default_name) else null
    val resolvedTitle = threadName?.ifBlank { null }
        ?: agentNameValue
        ?: stringResource(string.no_agent_assigned)
    val threadDescription = when {
        conversationState.isMultiThreaded && agentNameValue != null ->
            stringResource(string.content_description_conversation_with_agent, resolvedTitle, agentNameValue)

        !conversationState.isLiveChat ->
            stringResource(string.content_description_conversation_name, resolvedTitle)

        else -> resolvedTitle
    }
    val stateDescriptionString = if (hasAgent) {
        stringResource(string.state_description_agent_assigned)
    } else {
        stringResource(string.state_description_no_agent_assigned)
    }
    val topBarContentDescription = if (!conversationState.isLiveChat) {
        "$stateDescriptionString $threadDescription"
    } else {
        threadDescription
    }
    ChatTheme.MediumTopBar(
        modifier = Modifier.testTag("chat_thread_top_bar"),
        titleModifier = Modifier.semantics {
            liveRegion = LiveRegionMode.Polite
            contentDescription = topBarContentDescription
        },
        title = resolvedTitle,
        subtitle = aiSubtitle,
        scrollBehavior = scrollBehavior,
        navigationIcon = navigationIcon,
        actions = {
            Actions(
                conversationState = conversationState,
                onEditThreadName = onEditThreadName,
                onEditThreadValues = onEditThreadValues,
                onEndContact = onEndContact,
                displayEndConversation = displayEndConversation,
                onSendTranscript = onSendTranscript
            )
        },
    )
}

@Composable
private fun AiAuthorshipBadge(modifier: Modifier = Modifier) {
    val label = stringResource(string.ai_assistant_label)
    Box(
        modifier = modifier
            .background(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(space.small),
            )
            .padding(horizontal = space.small)
            .semantics(mergeDescendants = true) { contentDescription = label },
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.semantics { hideFromAccessibility() },
        )
    }
}

@Composable
private fun Actions(
    conversationState: ConversationTopBarState,
    onEditThreadName: () -> Unit,
    onEditThreadValues: () -> Unit,
    onEndContact: () -> Unit,
    displayEndConversation: () -> Unit,
    onSendTranscript: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isArchived by conversationState.isArchived.collectAsState()
    var expanded by remember { mutableStateOf(false) }
    val dismiss: () -> Unit = { expanded = false }
    IconButton(
        modifier = modifier.testTag("chat_thread_top_bar_menu_button"),
        onClick = { expanded = true },
    ) {
        Icon(Default.MoreVert, contentDescription = stringResource(string.livechat_conversation_options))
    }
    DropdownMenu(
        expanded = expanded,
        containerColor = chatColors.token.background.surface.subtle,
        onDismissRequest = { expanded = false },
        shape = chatShapes.menuActionsBoxShape,
        modifier = Modifier.testTag("chat_thread_top_bar_menu"),
        offset = DpOffset(x = -space.xl, y = 0.dp) // shift left by 24dp
    ) {
        if (conversationState.isMultiThreaded) {
            ChangeThreadNameMenu {
                onEditThreadName()
                dismiss()
            }
        }
        if (conversationState.hasQuestions && !isArchived) {
            EditThreadValuesMenu {
                onEditThreadValues()
                dismiss()
            }
        }
        if (conversationState.liveChatAllowTranscript) {
            SendTranscriptMenu {
                onSendTranscript()
                dismiss()
            }
        }
        if (conversationState.isLiveChat) {
            if (isArchived) {
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
    val agentNameState = rememberTextFieldState("John Smith")
    val isArchivedFlow: MutableStateFlow<Boolean> = remember { MutableStateFlow(false) }
    val isArchived by isArchivedFlow.collectAsState()
    val conversationStateFlow = remember { MutableStateFlow(ChatThreadState.Pending) }
    val conversationState by conversationStateFlow.collectAsState()
    val isMultiThreaded = remember { mutableStateOf(true) }
    val hasQuestions = remember { mutableStateOf(true) }
    val isLiveChat = remember { mutableStateOf(true) }
    val liveChatAllowTranscript = remember { mutableStateOf(true) }
    val hasAiMessagesFlow: MutableStateFlow<Boolean> = remember { MutableStateFlow(true) }
    val hasAiMessages by hasAiMessagesFlow.collectAsState()
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
                        isArchived = isArchivedFlow,
                        threadState = conversationStateFlow,
                        liveChatAllowTranscript = liveChatAllowTranscript.value,
                        agentName = snapshotFlow { agentNameState.text.toString().ifBlank { null } },
                        hasAiMessages = hasAiMessagesFlow,
                    ),
                    onEditThreadName = {},
                    onEditThreadValues = {},
                    onEndContact = {},
                    displayEndConversation = {},
                    onSendTranscript = {},
                    navigationIcon = { BackButton {} }
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .padding(paddingValues)
                    .then(Modifier.padding(8.dp))
                    .verticalScroll(rememberScrollState())
                    .semantics(true) {
                        hideFromAccessibility()
                    },
                horizontalAlignment = Alignment.Start,
            ) {
                ChatTheme.TextField(
                    label = "Thread name",
                    value = threadNameState,
                )
                ChatTheme.TextField(
                    label = "Agent name (blank = none)",
                    value = agentNameState,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(space.small),
                ) {
                    Switch(
                        checked = isArchived,
                        onCheckedChange = { isArchivedFlow.value = it },
                        modifier = Modifier.semantics(true) {
                            hideFromAccessibility()
                        }
                    )
                    Text(
                        "Archived",
                        modifier = Modifier.semantics(true) {
                            hideFromAccessibility()
                        },
                    )
                }
                PreviewSwitch(isMultiThreaded, "Multi-threaded")
                PreviewSwitch(hasQuestions, "Has questions")
                PreviewSwitch(isLiveChat, "Live chat")
                PreviewSwitch(liveChatAllowTranscript, "Live chat allow transcript")
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(space.small),
                ) {
                    Switch(
                        checked = hasAiMessages,
                        onCheckedChange = { hasAiMessagesFlow.value = it },
                        modifier = Modifier.semantics(true) {
                            hideFromAccessibility()
                        }
                    )
                    Text(
                        "AI messages",
                        modifier = Modifier.semantics(true) {
                            hideFromAccessibility()
                        },
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(space.small),
                ) {
                    Switch(
                        checked = conversationState == ChatThreadState.Ready,
                        onCheckedChange = {
                            conversationStateFlow.value = if (!it) ChatThreadState.Closed else ChatThreadState.Ready
                        },
                        modifier = Modifier.semantics(true) {
                            hideFromAccessibility()
                        }
                    )
                    Text(
                        "Chat Thread Ready",
                        modifier = Modifier.semantics(true) {
                            hideFromAccessibility()
                        },
                    )
                }
                HorizontalDivider()
                Text(
                    content,
                    Modifier.semantics(true) {
                        hideFromAccessibility()
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@PreviewLightDark
private fun PreviewAiBadgeTopBar() {
    ChatTheme {
        AiBadgeTopBarPreviewContent()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AiBadgeTopBarPreviewContent() {
    ChatTheme.Scaffold(
        topBar = {
            ChatThreadTopBar(
                scrollBehavior = null,
                conversationState = ConversationTopBarState(
                    threadName = flowOf("Support Chat"),
                    isMultiThreaded = false,
                    hasQuestions = false,
                    isLiveChat = true,
                    isArchived = MutableStateFlow(false),
                    threadState = MutableStateFlow(ChatThreadState.Pending),
                    liveChatAllowTranscript = false,
                    agentName = flowOf(null),
                    hasAiMessages = MutableStateFlow(true),
                ),
                onEditThreadName = {},
                onEditThreadValues = {},
                onEndContact = {},
                displayEndConversation = {},
                onSendTranscript = {},
                navigationIcon = { BackButton {} },
            )
        }
    ) { _ -> }
}

@Composable
private fun PreviewSwitch(checked: MutableState<Boolean>, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(space.small),
        modifier = Modifier.semantics(true) {
            hideFromAccessibility()
        }
    ) {
        Switch(
            checked = checked.value,
            onCheckedChange = { checked.value = it },
            modifier = Modifier.semantics(true) {
                hideFromAccessibility()
            }
        )
        Text(
            modifier = Modifier.semantics(true) {
                hideFromAccessibility()
            },
            text = label,
        )
    }
}
