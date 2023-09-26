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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.nice.cxonechat.message.Attachment
import com.nice.cxonechat.message.MessageAuthor
import com.nice.cxonechat.message.MessageDirection
import com.nice.cxonechat.message.MessageDirection.ToClient
import com.nice.cxonechat.message.MessageMetadata
import com.nice.cxonechat.ui.composable.conversation.model.Message.ListPicker
import com.nice.cxonechat.ui.composable.theme.ChatTheme.chatTypography
import com.nice.cxonechat.ui.composable.theme.ChatTheme.space
import java.util.Date
import java.util.UUID
import com.nice.cxonechat.message.Action as SdkAction
import com.nice.cxonechat.message.Message.ListPicker as SdkListPicker

@Composable
internal fun ListPickerMessage(message: ListPicker, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(message.title, style = chatTypography.chatCardTitle)
        Spacer(modifier = Modifier.size(space.medium))
        Text(message.text)
        Spacer(modifier = Modifier.size(space.small))
        ChipGroup(actions = message.actions)
    }
}

@Preview
@Composable
private fun ListPickerMessagePreview() {
    PreviewMessageItemBase {
        MessageItem(
            message = ListPicker(
                message = object : SdkListPicker() {
                    override val title: String = "This is a list picker card"
                    override val text: String = "We have provided list of random options, you should select one."
                    override val actions: Iterable<SdkAction> = listOf(
                        PreviewReplyButton("Some text"),
                        PreviewReplyButton("Random cat", "https://http.cat/203")
                    )
                    override val id: UUID = UUID.randomUUID()
                    override val threadId: UUID = UUID.randomUUID()
                    override val createdAt: Date = Date()
                    override val direction: MessageDirection = ToClient
                    override val metadata: MessageMetadata = object : MessageMetadata {
                        override val readAt: Date? = null
                    }
                    override val author: MessageAuthor = object : MessageAuthor() {
                        override val id: String = ""
                        override val firstName: String = "firstname"
                        override val lastName: String = "lastname"
                        override val imageUrl: String? = null
                    }
                    override val attachments: Iterable<Attachment> = emptyList()
                    override val fallbackText: String = "Fallback"
                },
                sendMessage = {}
            ),
            modifier = Modifier.padding(space.large)
        )
    }
}
