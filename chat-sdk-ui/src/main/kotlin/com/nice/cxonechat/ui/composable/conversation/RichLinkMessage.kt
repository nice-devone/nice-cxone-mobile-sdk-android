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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize.Max
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nice.cxonechat.message.Media
import com.nice.cxonechat.ui.R
import com.nice.cxonechat.ui.R.string
import com.nice.cxonechat.ui.composable.conversation.model.Message.RichLink
import com.nice.cxonechat.ui.composable.generic.AutoLinkedText
import com.nice.cxonechat.ui.composable.generic.PresetAsyncImage
import com.nice.cxonechat.ui.composable.theme.ChatColors.ColorPair
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import com.nice.cxonechat.ui.composable.theme.ChatTheme.chatTypography
import com.nice.cxonechat.ui.composable.theme.ChatTheme.colorScheme
import com.nice.cxonechat.ui.composable.theme.ChatTheme.space
import com.nice.cxonechat.ui.util.openWithAndroid
import com.nice.cxonechat.ui.util.preview.message.UiSdkRichLink

@Composable
internal fun RichLinkMessage(
    message: RichLink,
    textColor: ColorPair,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    BoxWithConstraints {
        val maxWidth = this.maxWidth.times(0.6f)
        val maxHeight = this.maxHeight.div(3)
        val url = message.url
        val onClickLabel = stringResource(string.on_click_label_url_link)
        Column(
            modifier = modifier
                .testTag("rich_link_message")
                .clickable(
                    onClickLabel = onClickLabel
                ) {
                    context.openWithAndroid(url = url, mimeType = null)
                }
                .width(Max)
                .widthIn(max = maxWidth)
                .semantics {
                    isTraversalGroup = true
                },
            horizontalAlignment = Alignment.Start,
        ) {
            RichLinkMedia(message.media, maxHeight, message.title)
            RichLinkTitle(message.title, textColor)
            RichLinkUrl(url)
        }
    }
}

@Composable
private fun ColumnScope.RichLinkMedia(media: Media, maxHeight: Dp, description: String) {
    val imageModel: String? = media.url.ifBlank { null }
    val imageContentDescription: String? = imageModel?.let {
        stringResource(string.content_description_rich_link_image, description)
    }
    PresetAsyncImage(
        model = imageModel,
        contentDescription = imageContentDescription,
        contentScale = ContentScale.FillWidth,
        isGroupAttachment = false,
        showLoadingBorder = true,
        modifier = Modifier
            .testTag("rich_link_image")
            .align(Alignment.CenterHorizontally)
            .heightIn(min = space.richLinkMediaMinSize, max = maxHeight)
            .widthIn(min = space.richLinkMediaMinSize)
            .fillMaxWidth()
    )
}

@Composable
private fun RichLinkTitle(title: String, textColor: ColorPair) {
    Text(
        text = title,
        style = chatTypography.chatCardTitle,
        color = textColor.foreground,
        modifier = Modifier.padding(start = space.semiLarge, top = space.medium, end = space.semiLarge)
    )
}

@Composable
private fun RichLinkUrl(url: String) {
    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier
            .padding(start = space.semiLarge, top = 3.dp, bottom = space.semiLarge, end = space.semiLarge)
    ) {
        val linkColor = colorScheme.primary
        AutoLinkedText(
            text = url,
            style = chatTypography.chatCardLink,
            linkColor = linkColor,
            linkDecoration = TextDecoration.None,
        )
        Icon(
            painterResource(R.drawable.ic_link),
            contentDescription = null, // Decorative icon
            tint = linkColor,
            modifier = Modifier
                .testTag("rich_link_icon")
                .padding(start = 2.dp, bottom = 2.dp)
                .size(width = 12.dp, height = 11.dp)
        )
    }
}

@PreviewLightDark
@Composable
private fun PreviewMessageRichLink() {
    ChatTheme {
        Box(
            Modifier
                .fillMaxHeight(0.8f)
        ) {
            Column {
                PreviewMessageItemBase(
                    message = RichLink(UiSdkRichLink()),
                )
            }
        }
    }
}
