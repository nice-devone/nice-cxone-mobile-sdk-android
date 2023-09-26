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

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.nice.cxonechat.ui.R.string
import com.nice.cxonechat.ui.composable.conversation.ContentType.DATE_HEADER
import com.nice.cxonechat.ui.composable.conversation.ContentType.LOADING
import com.nice.cxonechat.ui.composable.conversation.model.Message
import com.nice.cxonechat.ui.composable.conversation.model.Section
import com.nice.cxonechat.ui.composable.theme.ChatTheme.space
import com.nice.cxonechat.ui.util.isSameDay
import java.util.Date

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun ColumnScope.Messages(
    scrollState: LazyListState,
    groupedMessages: List<Section>,
    onClick: (Message) -> Unit,
    onMessageLongClick: (Message) -> Unit,
    loadMore: () -> Unit,
    canLoadMore: Boolean,
) {
    LazyColumn(
        reverseLayout = true,
        state = scrollState,
        verticalArrangement = Arrangement.Top,
        modifier = Modifier
            .weight(1f)
            .fillMaxSize(),
        contentPadding = PaddingValues(space.medium)
    ) {
        groupedMessages.forEach { section ->
            items(items = section.messages, key = Message::id, contentType = Message::contentType) { message ->
                MessageItem(
                    message = message,
                    onClick = onClick,
                    onMessageLongClick = onMessageLongClick,
                )
            }

            // Note that these will actually appear *above* the relevant messages because of `reverseLayout = true`
            when {
                // no header if only one day is displayed
                groupedMessages.count() <= 1 -> Unit

                // Display "Today" over today's messages
                section.createdAt.isSameDay(Date()) ->
                    item(contentType = DATE_HEADER) {
                        DayHeader(dayString = stringResource(string.today), Modifier.animateItemPlacement())
                    }

                // display appropriate date over other messages
                else ->
                    item(contentType = DATE_HEADER) {
                        DayHeader(dayString = section.createdAtDate, Modifier.animateItemPlacement())
                    }
            }
        }
        if (canLoadMore) {
            item(contentType = LOADING) {
                LoadMore(loadMore)
            }
        }
    }
}
