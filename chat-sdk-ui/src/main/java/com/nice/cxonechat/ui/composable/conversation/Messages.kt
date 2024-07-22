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

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.nice.cxonechat.message.Attachment
import com.nice.cxonechat.ui.R.string
import com.nice.cxonechat.ui.composable.conversation.ContentType.DateHeader
import com.nice.cxonechat.ui.composable.conversation.ContentType.Loading
import com.nice.cxonechat.ui.composable.conversation.model.PreviewMessageProvider
import com.nice.cxonechat.ui.composable.conversation.model.Section
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import com.nice.cxonechat.ui.composable.theme.ChatTheme.space
import com.nice.cxonechat.ui.util.isSameDay
import java.util.Date

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun ColumnScope.Messages(
    scrollState: LazyListState,
    groupedMessages: List<Section>,
    loadMore: () -> Unit,
    canLoadMore: Boolean,
    onAttachmentClicked: (Attachment) -> Unit,
    onMoreClicked: (List<Attachment>, String) -> Unit,
    onShare: (Collection<Attachment>) -> Unit,
) {
    LazyColumn(
        reverseLayout = true,
        state = scrollState,
        verticalArrangement = Arrangement.Top,
        modifier = Modifier
            .weight(1f)
            .fillMaxSize(),
        contentPadding = PaddingValues(space.medium),
    ) {
        groupedMessages.forEach { section ->
            itemsIndexed(
                items = section.messages,
                key = { _, message -> message.id },
                contentType = { _, message -> message.contentType }
            ) { i, message ->
                val isLast = i == section.messages.lastIndex
                val showSender = isLast || message.sender != section.messages[i + 1].sender
                MessageItem(
                    message = message,
                    showSender = showSender,
                    onAttachmentClicked = onAttachmentClicked,
                    onMoreClicked = onMoreClicked,
                    onShare = onShare,
                )
            }

            // Note that these will actually appear *above* the relevant messages because of `reverseLayout = true`
            when {
                // no header if only one day is displayed
                groupedMessages.size <= 1 -> Unit

                // Display "Today" over today's messages
                section.createdAt.isSameDay(Date()) ->
                    item(contentType = DateHeader) {
                        DayHeader(dayString = stringResource(string.today), Modifier.animateItemPlacement())
                    }

                // display appropriate date over other messages
                else ->
                    item(contentType = DateHeader) {
                        DayHeader(dayString = section.createdAtDate, Modifier.animateItemPlacement())
                    }
            }
        }

        if (canLoadMore) {
            item(contentType = Loading) {
                LoadMore(loadMore)
            }
        }
    }
}

@Preview
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
                onAttachmentClicked = {},
                onMoreClicked = { _, _ -> },
                onShare = {},
            )
        }
    }
}
