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
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.nice.cxonechat.message.Attachment
import com.nice.cxonechat.ui.composable.conversation.attachments.AttachmentFramedSmallPreview
import com.nice.cxonechat.ui.composable.icons.ChatIcons
import com.nice.cxonechat.ui.composable.icons.filled.Cancel
import com.nice.cxonechat.ui.composable.icons.filled.CancelDark
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import com.nice.cxonechat.ui.composable.theme.ChatTheme.colorScheme
import com.nice.cxonechat.ui.composable.theme.ChatTheme.space
import com.nice.cxonechat.ui.composable.theme.ChatTheme.typography

@Composable
internal fun AttachmentPreviewBar(
    attachments: List<Attachment>,
    onAttachmentClick: (Attachment) -> Unit,
    onAttachmentRemoved: (Attachment) -> Unit,
) {
    AnimatedVisibility(
        modifier = Modifier.testTag("attachment_preview_bar"),
        visible = attachments.isNotEmpty()
    ) {
        HorizontalDivider(modifier = Modifier.padding(horizontal = space.medium))
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = space.medium, vertical = 11.dp),
        ) {
            itemsIndexed(attachments, key = { i, item -> item.hashCode() }) { i, attachment ->
                val padding = space.small
                val modifier = if (i == 0) {
                    Modifier.padding(end = padding)
                } else {
                    Modifier.padding(horizontal = padding)
                }.testTag("attachment_preview_item_$i")
                AttachmentPreviewItem(attachment, onAttachmentClick, onAttachmentRemoved, modifier)
            }
        }
    }
}

@Composable
private fun AttachmentPreviewItem(
    attachment: Attachment,
    onAttachmentClick: (Attachment) -> Unit,
    onAttachmentRemoved: (Attachment) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        contentAlignment = Alignment.TopEnd,
        modifier = modifier,
    ) {
        Item(onAttachmentClick, attachment)
        CancelIcon(onAttachmentRemoved, attachment)
    }
}

@Composable
private fun Item(onAttachmentClick: (Attachment) -> Unit, attachment: Attachment) {
    val friendlyName = attachment.friendlyName
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .testTag("preview_item_$friendlyName")
            .clickable(onClick = remember { { onAttachmentClick(attachment) } })
    ) {
        AttachmentFramedSmallPreview(
            modifier = Modifier
                .padding(bottom = space.xSmall)
                .size(space.attachmentUploadPreviewSize),
            attachment = attachment,
        )
        Text(
            text = friendlyName,
            style = typography.bodySmall,
            overflow = TextOverflow.MiddleEllipsis,
            maxLines = 1,
            modifier = Modifier.widthIn(max = space.attachmentUploadPreviewSize.width - space.xSmall)
        )
    }
}

@Composable
private fun CancelIcon(onAttachmentRemoved: (Attachment) -> Unit, attachment: Attachment) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .testTag("preview_item_cancel_${attachment.friendlyName}")
            .clickable(onClick = remember { { onAttachmentRemoved(attachment) } })
            .size(space.attachmentUploadRemoveClickableSize)
            .offset(x = 11.dp, y = (-11).dp)
    ) {
        Image(
            imageVector = if (isSystemInDarkTheme()) ChatIcons.CancelDark else ChatIcons.Cancel,
            contentDescription = "Remove prepared attachment ${attachment.friendlyName}",
        )
    }
}

@PreviewLightDark
@Composable
private fun AttachmentPreviewBarPreview() {
    var showAttachment: Boolean by remember { mutableStateOf(true) }
    val attachments = remember { PreviewAttachments.choices.toMutableStateList() }
    ChatTheme {
        Surface(color = colorScheme.background) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(space.medium),
                    modifier = Modifier.padding(space.medium)
                ) {
                    Switch(showAttachment, onCheckedChange = {
                        showAttachment = it
                        if (it) {
                            attachments.addAll(PreviewAttachments.choices)
                        } else {
                            attachments.clear()
                        }
                    })
                    Text("Show attachment preview", style = typography.labelLarge)
                }
                AttachmentPreviewBar(
                    attachments = attachments,
                    onAttachmentClick = {},
                    onAttachmentRemoved = {
                        attachments.remove(it)
                        if (attachments.isEmpty()) {
                            showAttachment = false
                        }
                    }
                )
            }
        }
    }
}
