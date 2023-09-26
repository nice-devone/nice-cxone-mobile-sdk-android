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

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import com.nice.cxonechat.ui.composable.conversation.MessageItem
import com.nice.cxonechat.ui.composable.conversation.PreviewMessageItemBase
import com.nice.cxonechat.ui.composable.conversation.model.Message
import com.nice.cxonechat.ui.composable.conversation.model.PluginElement
import com.nice.cxonechat.ui.composable.conversation.model.PluginElement.Button
import com.nice.cxonechat.ui.composable.conversation.model.PluginElement.Custom
import com.nice.cxonechat.ui.composable.conversation.model.PluginElement.File
import com.nice.cxonechat.ui.composable.conversation.model.PluginElement.Gallery
import com.nice.cxonechat.ui.composable.conversation.model.PluginElement.Menu
import com.nice.cxonechat.ui.composable.conversation.model.PluginElement.QuickReplies
import com.nice.cxonechat.ui.composable.conversation.model.PluginElement.SatisfactionSurvey
import com.nice.cxonechat.ui.composable.conversation.model.PluginElement.Subtitle
import com.nice.cxonechat.ui.composable.conversation.model.PluginElement.Text
import com.nice.cxonechat.ui.composable.conversation.model.PluginElement.TextAndButtons
import com.nice.cxonechat.ui.composable.conversation.model.PluginElement.Title
import com.nice.cxonechat.ui.composable.theme.ChatTheme

@Composable
internal fun BindElement(
    element: PluginElement,
    modifier: Modifier = Modifier,
    fallbackBinding: @Composable (PluginElement) -> Unit
) = when (element) {
    is Button -> ButtonElementView(button = element, modifier = modifier)
    is Custom -> CustomElementView(custom = element, modifier = modifier)
    is File -> FileElementView(file = element, modifier = modifier)
    is Gallery -> GalleryElementView(gallery = element, modifier = modifier, fallbackBinding = fallbackBinding)
    is Menu -> MenuElementView(menu = element, modifier = modifier)
    is QuickReplies -> QuickReplyElementView(quickReplies = element, modifier = modifier)
    is SatisfactionSurvey -> SatisfactionSurveyElementView(satisfactionSurvey = element, modifier = modifier)
    is Subtitle -> SubtitleElementView(subtitle = element, modifier = modifier)
    is Text -> TextElementView(text = element, modifier = modifier)
    is TextAndButtons -> TextAndButtonsElementView(textAndButtons = element, modifier = modifier)
    is Title -> TitleElementView(title = element, modifier = modifier)
    else -> fallbackBinding(element)
}

@Composable
@Preview
private fun PluginMessagePreview(
    @PreviewParameter(PluginPreviewProvider::class)
    message: Message
) {
    PreviewMessageItemBase {
        MessageItem(message = message, modifier = Modifier.padding(ChatTheme.space.large))
    }
}
