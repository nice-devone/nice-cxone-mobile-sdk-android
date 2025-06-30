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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
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
import com.nice.cxonechat.ui.composable.conversation.model.Message
import com.nice.cxonechat.ui.composable.conversation.model.PreviewMessageProvider
import com.nice.cxonechat.ui.composable.conversation.model.Section
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import com.nice.cxonechat.ui.composable.theme.ChatTheme.space
import com.nice.cxonechat.ui.domain.model.Person
import com.nice.cxonechat.ui.util.preview.message.toPerson

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
) {
    LaunchedEffect(agentIsTyping) {
        if (agentIsTyping) {
            scrollState.scrollToItem(0)
        }
    }
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
                val showStatus: Boolean = remember(message.status, groupState, isLastMessage, position) {
                    message.showStatus(groupState, isLastMessage)
                }
                if (showStatus) {
                    lastDisplayedStatuses[message.status] = position
                }

                MessageItem(
                    message = message,
                    showStatus = showStatus,
                    itemGroupState = groupState,
                    onAttachmentClicked = onAttachmentClicked,
                    onMoreClicked = onMoreClicked,
                    onShare = onShare,
                    modifier = Modifier
                        .testTag("message_item_$position")
                        .fillParentMaxWidth()
                        .animateItem(),
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

private fun Message.showStatus(
    groupState: MessageItemGroupState,
    isLastMessage: Boolean,
): Boolean = direction === ToAgent && (
        status === MessageStatus.FailedToDeliver || isLastMessage && groupState in setOf(LAST, SOLO)
        )

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
            )
        }
    }
}
