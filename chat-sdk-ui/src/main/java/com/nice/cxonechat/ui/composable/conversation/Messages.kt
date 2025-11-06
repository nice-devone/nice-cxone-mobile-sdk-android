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

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import com.nice.cxonechat.message.Attachment
import com.nice.cxonechat.message.MessageDirection
import com.nice.cxonechat.message.MessageDirection.ToAgent
import com.nice.cxonechat.message.MessageStatus
import com.nice.cxonechat.ui.composable.conversation.ContentType.DateHeader
import com.nice.cxonechat.ui.composable.conversation.ContentType.Loading
import com.nice.cxonechat.ui.composable.conversation.ContentType.Typing
import com.nice.cxonechat.ui.composable.conversation.MessageItemGroupState.LAST
import com.nice.cxonechat.ui.composable.conversation.MessageItemGroupState.SOLO
import com.nice.cxonechat.ui.composable.conversation.MessageItemGroupState.SOLO_GROUPED
import com.nice.cxonechat.ui.composable.conversation.model.Message
import com.nice.cxonechat.ui.composable.conversation.model.PreviewMessageProvider
import com.nice.cxonechat.ui.composable.conversation.model.Section
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import com.nice.cxonechat.ui.composable.theme.ChatTheme.space
import com.nice.cxonechat.ui.domain.model.Person
import com.nice.cxonechat.ui.util.preview.message.toPerson
import java.util.UUID

@Suppress("LongMethod")
@Composable
internal fun ColumnScope.Messages(
    scrollState: LazyListState,
    groupedMessages: List<Section>,
    loadMore: () -> Unit,
    canLoadMore: Boolean,
    agentIsTyping: Boolean,
    agentDetails: Person?,
    modifier: Modifier = Modifier,
    onAttachmentClicked: (Attachment) -> Unit,
    onMoreClicked: (List<Attachment>) -> Unit,
    onShare: (Collection<Attachment>) -> Unit,
    snackBarHostState: SnackbarHostState,
) {
    LaunchedEffect(agentIsTyping) {
        if (agentIsTyping) {
            scrollState.scrollToItem(0)
        }
    }
    val messageQuickReplyState = rememberMessageQuickReplyState(groupedMessages)
    val messageListPickerState = rememberMessageListPickerState(groupedMessages)
    val lastDisplayedStatuses: MutableMap<MessageStatus, Position> = remember { mutableMapOf() }
    LazyColumn(
        reverseLayout = true,
        state = scrollState,
        verticalArrangement = Arrangement.Top,
        modifier = Modifier
            .testTag("messages")
            .then(modifier)
            .weight(1f)
            .padding(horizontal = space.medium)
            .fillMaxSize(),
        contentPadding = PaddingValues(space.medium),
    ) {
        if (agentIsTyping && agentDetails != null) {
            item(contentType = Typing) {
                TypingIndicatorMessage(
                    agent = agentDetails,
                    modifier = Modifier.padding(top = space.small)
                )
            }
        }

        groupedMessages.forEachIndexed { sectionIndex, section ->
            itemsIndexed(
                items = section.messages,
                key = { i, message -> "${message.id}_$i" }, // ID may not be unique because of message splitting based on content
                contentType = { _, message -> message.contentType }
            ) { i, message ->
                val isLast = i == section.messages.lastIndex
                val sender = message.sender
                val groupState = remember(section, i, sender, isLast) { getGroupState(section, i) }
                val position = remember(sectionIndex, i) { Position(sectionIndex, i) }
                val isLastMessage = remember(position, message.status) {
                    message.direction === ToAgent &&
                            lastDisplayedStatuses.filterKeys {
                                it !== MessageStatus.FailedToDeliver && // Failed message status is always shown
                                        it >= message.status // Show last Higher status if current lower
                            }.none { position > it.value }
                }
                val isLastMessageInChat = groupedMessages.first().messages.first().id == message.id
                val messageStatusState = getMessageStatusState(
                    message,
                    isLastMessageInChat,
                    messageQuickReplyState,
                    messageListPickerState
                )
                val showStatus: DisplayStatus = remember(message.status, groupState, isLastMessage, position) {
                    message.showStatus(groupState, isLastMessage)
                }
                if (showStatus === DisplayStatus.DISPLAY) {
                    lastDisplayedStatuses[message.status] = position
                }

                MessageItem(
                    message = message,
                    showStatus = showStatus,
                    itemGroupState = groupState,
                    onAttachmentClicked = onAttachmentClicked,
                    onMoreClicked = onMoreClicked,
                    onShare = onShare,
                    messageStatusState = messageStatusState,
                    onQuickReplyOptionSelected = { newValue ->
                        messageQuickReplyState[message.id] = newValue
                    },
                    onListPickerSelected = { newValue ->
                        messageListPickerState[message.id] = newValue
                    },
                    modifier = Modifier
                        .testTag("message_item_$position")
                        .fillParentMaxWidth()
                        .animateItem(),
                    snackBarHostState = snackBarHostState,
                )
            }

            // Note that these will actually appear *above* the relevant messages because of `reverseLayout = true`
            // Display appropriate date over other messages
            item(contentType = DateHeader) {
                MessageGroupHeader(dayString = section.createdAtDate, Modifier.animateItem())
            }
        }

        if (canLoadMore) {
            item(contentType = Loading) {
                LoadMore(loadMore = loadMore)
            }
        }
    }
}

private fun getMessageStatusState(
    message: Message,
    isLastMessageInChat: Boolean,
    quickReplyState: Map<UUID, Boolean>,
    listPickerState: Map<UUID, Boolean>
): MessageStatusState = when (message.contentType) {
    ContentType.QuickReply -> getQuickReplyState(isLastMessageInChat, quickReplyState[message.id] ?: true)
    ContentType.ListPicker -> getListPickerState(listPickerState[message.id] ?: true)
    else -> MessageStatusState.DISABLED
}

@Composable
private fun rememberMessageQuickReplyState(groupedMessages: List<Section>): MutableMap<UUID, Boolean> {
    val messageQuickReplyStateSaver = listSaver<MutableMap<UUID, Boolean>, Pair<UUID, Boolean>>(
        save = { it.entries.map { entry -> entry.toPair() } },
        restore = { pairs -> mutableStateMapOf<UUID, Boolean>().apply { pairs.forEach { put(it.first, it.second) } } }
    )
    val messageQuickReplyState = rememberSaveable(saver = messageQuickReplyStateSaver) { mutableStateMapOf() }
    LaunchedEffect(groupedMessages) {
        groupedMessages.forEach { section ->
            section.messages.filter { it.contentType == ContentType.QuickReply }.forEach { message ->
                if (messageQuickReplyState[message.id] == null) {
                    messageQuickReplyState[message.id] = true
                }
            }
        }
    }
    return messageQuickReplyState
}

@Composable
private fun rememberMessageListPickerState(groupedMessages: List<Section>): MutableMap<UUID, Boolean> {
    val listPickerStateSaver = listSaver<MutableMap<UUID, Boolean>, Pair<UUID, Boolean>>(
        save = { it.entries.map { entry -> entry.toPair() } },
        restore = { pairs -> mutableStateMapOf<UUID, Boolean>().apply { pairs.forEach { put(it.first, it.second) } } }
    )
    val listPickerState = rememberSaveable(saver = listPickerStateSaver) { mutableStateMapOf<UUID, Boolean>() }
    LaunchedEffect(groupedMessages) {
        groupedMessages.forEach { section ->
            section.messages.filter { it.contentType == ContentType.ListPicker }.forEach { message ->
                if (listPickerState[message.id] == null) {
                    listPickerState[message.id] = true
                }
            }
        }
    }
    return listPickerState
}

@Immutable
private data class Position(
    val sectionIndex: Int,
    val messageIndex: Int,
) : Comparable<Position> {
    override fun compareTo(other: Position): Int =
        when (val sectionCompare = sectionIndex.compareTo(other.sectionIndex)) {
            0 -> messageIndex.compareTo(other.messageIndex)
            else -> sectionCompare
        }
}

internal fun Message.showStatus(
    groupState: MessageItemGroupState,
    isLastMessage: Boolean,
): DisplayStatus = when {
    direction === MessageDirection.ToClient -> DisplayStatus.HIDE
    status === MessageStatus.FailedToDeliver -> DisplayStatus.DISPLAY
    isLastMessage && groupState in setOf(LAST, SOLO, SOLO_GROUPED) -> DisplayStatus.DISPLAY
    groupState in setOf(LAST, SOLO) -> DisplayStatus.SPACER
    else -> DisplayStatus.HIDE
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_NO or Configuration.UI_MODE_TYPE_NORMAL)
@Composable
private fun MessagesPreview() {
    val context = LocalContext.current
    val section = PreviewMessageProvider()
        .values
        .groupBy { it.createdAtDate(context) }
        .entries
        .map(::Section)
    val listState = rememberLazyListState()

    ChatTheme {
        Column {
            Messages(
                scrollState = listState,
                groupedMessages = section,
                loadMore = {},
                canLoadMore = false,
                agentIsTyping = true,
                agentDetails = MessageDirection.ToClient.toPerson(),
                onAttachmentClicked = {},
                onMoreClicked = { _ -> },
                onShare = {},
                snackBarHostState = SnackbarHostState(),
            )
        }
    }
}
