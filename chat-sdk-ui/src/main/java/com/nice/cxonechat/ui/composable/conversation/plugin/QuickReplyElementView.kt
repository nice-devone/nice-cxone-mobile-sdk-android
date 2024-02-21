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

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.nice.cxonechat.ui.composable.conversation.PreviewMessageItemBase
import com.nice.cxonechat.ui.composable.conversation.model.PluginElement.QuickReplies

@Composable
internal fun QuickReplyElementView(quickReplies: QuickReplies, modifier: Modifier = Modifier) {
    PluginCard(modifier = modifier) {
        val text = quickReplies.text
        if (text?.text?.isNotEmpty() == true) {
            TextElementView(text = text)
        }

        for (button in quickReplies.buttons) {
            ButtonElementView(button = button)
        }
    }
}

@Composable
@Preview
private fun PreviewQuickReply() {
    PreviewMessageItemBase(
        message = MessagePreviewProvider().quickReply.asMessage("quick reply"),
        showSender = true,
    )
}
