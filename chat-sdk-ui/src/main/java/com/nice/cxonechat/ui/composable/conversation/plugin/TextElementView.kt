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
import androidx.compose.ui.tooling.preview.PreviewParameter
import com.nice.cxonechat.ui.composable.conversation.MessageItem
import com.nice.cxonechat.ui.composable.conversation.PreviewMessageItemBase
import com.nice.cxonechat.ui.composable.conversation.model.Message
import com.nice.cxonechat.ui.composable.conversation.model.PluginElement.Text
import com.nice.cxonechat.ui.composable.conversation.model.PluginElement.Text.Format.Html
import com.nice.cxonechat.ui.composable.conversation.model.PluginElement.Text.Format.Markdown
import com.nice.cxonechat.ui.composable.conversation.model.PluginElement.Text.Format.Plain
import com.nice.cxonechat.ui.composable.generic.HtmlText
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import dev.jeziellago.compose.markdowntext.MarkdownText

@Composable
internal fun TextElementView(text: Text, modifier: Modifier = Modifier) {
    when (text.format) {
        Markdown -> MarkdownText(markdown = text.text, modifier = modifier, style = ChatTheme.typography.body1)
        Html -> HtmlText(html = text.text, modifier = modifier, textStyle = ChatTheme.typography.body1)
        Plain -> Text(text.text, modifier = modifier, style = ChatTheme.typography.body1)
    }
}

@Composable
@Preview
private fun PreviewText(@PreviewParameter(TextPreviewProvider::class) message: Message) {
    PreviewMessageItemBase {
        MessageItem(message = message)
    }
}

@Composable
@Preview
private fun PreviewAllText() {
    PreviewMessageItemBase {
        PluginPreviewProvider().text.forEach {
            MessageItem(message = it)
        }
    }
}

private class TextPreviewProvider: PluginPreviewProvider() {
    override val values: Sequence<Message>
        get() = text.asSequence()
}
