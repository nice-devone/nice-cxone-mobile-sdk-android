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
import androidx.compose.foundation.layout.Column
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import com.nice.cxonechat.ui.composable.conversation.model.Message.RichLink
import com.nice.cxonechat.ui.composable.conversation.model.PreviewMessageProvider
import com.nice.cxonechat.ui.composable.generic.PresetAsyncImage
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import com.nice.cxonechat.ui.util.openWithAndroid

@Composable
internal fun RichLinkMessage(message: RichLink, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Column(
        modifier
            .clickable {
                context.openWithAndroid(url = message.url, mimeType = null)
            },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        PresetAsyncImage(
            model = message.media.url.ifBlank { null },
            contentDescription = message.media.fileName,
        )
        Text(
            text = message.title,
            style = ChatTheme.chatTypography.chatCardTitle,
        )
    }
}

@Preview
@Composable
private fun PreviewMessageRichLink() {
    PreviewMessageItemBase(
        message = RichLink(PreviewMessageProvider.RichLink()),
        showSender = true,
    )
}
