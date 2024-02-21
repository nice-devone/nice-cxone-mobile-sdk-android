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

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.nice.cxonechat.ui.composable.conversation.PreviewMessageItemBase
import com.nice.cxonechat.ui.composable.conversation.model.PluginElement.Menu
import com.nice.cxonechat.ui.composable.generic.ImageCarousel
import com.nice.cxonechat.ui.composable.theme.ChatTheme.space

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun MenuElementView(menu: Menu, modifier: Modifier = Modifier) {
    PluginCard(modifier = modifier) {
        val pairs = menu.files.map { it.url to it.name }
        if (pairs.isNotEmpty()) {
            ImageCarousel(
                images = pairs,
                modifier = Modifier
                    .fillMaxWidth(0.80f)
                    .height(space.menuElementHeight),
                autoScrollDuration = 0,
            )
        }

        menu.titles.firstOrNull()?.let { TitleElementView(title = it) }
        menu.subtitles.firstOrNull()?.let { SubtitleElementView(subtitle = it) }
        menu.texts.forEach { TextElementView(it) }
        menu.buttons.forEach { ButtonElementView(it) }
    }
}

@Composable
@Preview
private fun PreviewMenu() {
    PreviewMessageItemBase(
        message = MessagePreviewProvider().menu.asMessage("menu"),
        showSender = true,
    )
}
