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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.nice.cxonechat.message.Attachment
import com.nice.cxonechat.message.MessageDirection.ToClient
import com.nice.cxonechat.ui.R.string
import com.nice.cxonechat.ui.composable.conversation.model.Message.WithAttachments
import com.nice.cxonechat.ui.composable.conversation.model.PreviewMessageProvider
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import com.nice.cxonechat.ui.composable.theme.ChatTheme.space
import com.nice.cxonechat.ui.composable.theme.SelectionFrame
import java.lang.Integer.min

@Composable
internal fun AttachmentMessage(
    message: WithAttachments,
    modifier: Modifier,
    onAttachmentClicked: (Attachment) -> Unit,
    onMoreClicked: (List<Attachment>, String) -> Unit,
    onShare: (Collection<Attachment>) -> Unit,
) {
    val attachments = message.attachments.toList()
    val iconCount = space.smallAttachmentCount

    Column(
        modifier = modifier.padding(0.dp),
        verticalArrangement = Arrangement.spacedBy(space.small)
    ) {
        if (attachments.isEmpty()) {
            // This really can't happen since the decision to get to AttachmentMessage
            // was predicated on attachments.count > 0
            Text(
                text = message.text.ifBlank { "Missing attachments in attachment message." },
                style = ChatTheme.chatTypography.chatMessage,
            )
        } else {
            if (message.text.isNotBlank()) {
                Text(message.text, style = ChatTheme.chatTypography.chatMessage)
            }

            LazyRow(horizontalArrangement = Arrangement.spacedBy(space.small)) {
                items(
                    min(iconCount, attachments.count()),
                    key = { attachments[it].url }
                ) { index ->
                    AttachmentItem(
                        index = index,
                        message = message.text,
                        attachments = attachments,
                        onAttachmentClicked = onAttachmentClicked,
                        onMoreClicked = onMoreClicked,
                        onShare = onShare
                    )
                }
            }
        }
    }
}

@Composable
private fun AttachmentItem(
    index: Int,
    message: String,
    attachments: List<Attachment>,
    onAttachmentClicked: (Attachment) -> Unit,
    onMoreClicked: (List<Attachment>, String) -> Unit,
    onShare: (Collection<Attachment>) -> Unit,
) {
    val count = attachments.size
    val iconCount = space.smallAttachmentCount

    if (index == iconCount - 1 && count > iconCount) {
        MoreIcon(
            count = count - iconCount + 1,
            onClicked = { onMoreClicked(attachments, message) },
        )
    } else {
        val attachment = attachments[index]

        AttachmentIcon(
            attachment = attachment,
            modifier = Modifier.size(space.smallAttachmentSize),
            onClick = onAttachmentClicked,
            onLongClick = {
                if (attachments.size > 1) {
                    onMoreClicked(attachments, message)
                } else {
                    onShare(listOf(it))
                }
            },
        )
    }
}

@Composable
private fun MoreIcon(
    count: Int,
    modifier: Modifier = Modifier,
    onClicked: () -> Unit,
) {
    ChatTheme.SelectionFrame(
        modifier = Modifier
            .size(space.smallAttachmentSize)
            .then(modifier)
            .clickable(onClick = onClicked)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                stringResource(string.extra_attachments_count, count),
                style = ChatTheme.typography.titleMedium
            )
        }
    }
}

@Preview
@Composable
private fun PreviewMoreIcon() {
    ChatTheme {
        MoreIcon(count = 4, onClicked = {})
    }
}

private data class CountProvider(
    override val values: Sequence<Int> = (0..5).asSequence()
) : PreviewParameterProvider<Int>

@Preview
@Composable
private fun PreviewAttachmentMessage(
    @PreviewParameter(CountProvider::class) count: Int
) {
    val message = WithAttachments(
        message = PreviewMessageProvider.Text(
            "Preview video",
            direction = ToClient,
            attachments = PreviewAttachments.with(count)
        )
    )

    PreviewMessageItemBase(
        message = message,
        showSender = true
    )
}
