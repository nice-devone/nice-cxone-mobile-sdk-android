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

package com.nice.cxonechat.ui.composable.conversation.plugin

import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.nice.cxonechat.ui.composable.conversation.PreviewMessageItemBase
import com.nice.cxonechat.ui.composable.conversation.model.PluginElement.Subtitle
import com.nice.cxonechat.ui.composable.theme.ChatTheme

@Composable
internal fun SubtitleElementView(subtitle: Subtitle, modifier: Modifier = Modifier) {
    Text(subtitle.text, modifier = modifier, style = ChatTheme.chatTypography.chatCardSubtitle)
}

@Composable
@Preview
private fun PreviewSubtitle() {
    PreviewMessageItemBase(
        message = MessagePreviewProvider().subtitle.asMessage("subtitle"),
        showSender = true,
    )
}
