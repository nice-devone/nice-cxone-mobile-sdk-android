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

package com.nice.cxonechat.ui.composable

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow.Companion.Ellipsis
import androidx.compose.ui.tooling.preview.Preview
import com.nice.cxonechat.message.MessageDirection.ToClient
import com.nice.cxonechat.ui.composable.conversation.orDefaultThreadName
import com.nice.cxonechat.ui.composable.generic.AgentAvatar
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import com.nice.cxonechat.ui.composable.theme.ChatTheme.chatTypography
import com.nice.cxonechat.ui.composable.theme.ChatTheme.colorScheme
import com.nice.cxonechat.ui.composable.theme.ChatTheme.space
import com.nice.cxonechat.ui.domain.model.Thread
import com.nice.cxonechat.ui.util.toShortTimeString

@Composable
internal fun ChatThreadView(thread: Thread, onThreadSelected: (Thread) -> Unit) {
    val firstMessage = thread.messages.firstOrNull()
    val isUnread = firstMessage?.metadata?.seenByCustomerAt == null &&
            firstMessage?.direction == ToClient &&
            thread.chatThread.canAddMoreMessages

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onThreadSelected(thread) }
            .background(ChatTheme.chatColors.token.background.default)
            .testTag("chat_thread_view_${thread.id}"),
    ) {
        AgentAvatar(
            url = thread.agent?.imageUrl,
            modifier = Modifier.padding(start = space.large, top = space.semiLarge)
        )
        Spacer(modifier = Modifier.width(space.large))
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = space.semiLarge)
        ) {
            ChatThreadHeader(thread, isUnread)
            ChatThreadLastMessage(thread, isUnread)
        }
    }
}

@Composable
private fun ChatThreadHeader(thread: Thread, isUnread: Boolean) {
    Row(verticalAlignment = CenterVertically) {
        if (isUnread) {
            UnreadIndicator()
            Spacer(modifier = Modifier.width(space.small))
        }
        Text(
            text = thread.name.orDefaultThreadName(),
            modifier = Modifier.testTag("conversation_name"),
            color = ChatTheme.chatColors.token.content.primary,
            style = if (isUnread) chatTypography.threadListNameUnread else chatTypography.threadListName,
        )
        Spacer(modifier = Modifier.weight(1f))
        val context = LocalContext.current
        val lastMessageTime = remember(thread, context) {
            thread.lastMessageTime?.let { context.toShortTimeString(it) }.orEmpty()
        }
        Text(
            text = lastMessageTime,
            modifier = Modifier.testTag("last_message_time"),
            style = if (isUnread) chatTypography.threadListLastMessageTimeUnread else chatTypography.threadListLastMessageTime,
            color = colorScheme.tertiary
        )
        DetailsChevron()
    }
}

@Composable
private fun UnreadIndicator() {
    Box(
        modifier = Modifier
            .size(space.medium)
            .testTag("unread_indicator")
            .background(color = colorScheme.primary, shape = CircleShape)
    )
}

@Composable
private fun ChatThreadLastMessage(thread: Thread, isUnread: Boolean) {
    Text(
        text = thread.lastMessage,
        modifier = Modifier.padding(end = space.xxl),
        style = if (isUnread) chatTypography.threadListLastMessageUnread else chatTypography.threadListLastMessage,
        maxLines = 2,
        overflow = Ellipsis,
        color = colorScheme.tertiary,
    )
}

@Composable
@Preview
@NonRestartableComposable
private fun DetailsChevron(modifier: Modifier = Modifier) {
    Image(
        painter = rememberVectorPainter(image = Icons.Default.ChevronRight),
        contentDescription = "open conversation details",
        modifier = modifier.padding(end = space.medium),
        colorFilter = ColorFilter.tint(colorScheme.primary)
    )
}

@Composable
@Preview
private fun ChatThreadViewPreview() {
    ChatTheme {
        ChatThreadView(PreviewThread.nextThread()) { }
    }
}
