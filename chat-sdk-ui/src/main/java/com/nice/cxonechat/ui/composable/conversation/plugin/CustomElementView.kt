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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nice.cxonechat.ui.R.string
import com.nice.cxonechat.ui.composable.conversation.PreviewMessageItemBase
import com.nice.cxonechat.ui.composable.conversation.model.PluginElement.Custom
import com.nice.cxonechat.ui.composable.theme.ChatTheme

@Composable
internal fun CustomElementView(custom: Custom, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val color = when(custom.variables["color"]) {
        "green" -> Color.Green
        "blue" -> Color.Blue
        else -> Color.Black
    }

    Column(
        modifier = modifier
            .background(color)
            .defaultMinSize(100.dp, 100.dp)
    ) {
        if (custom.fallbackText?.isNotEmpty() == true) {
            Text(
                context.getString(string.message_plugin_custom_fallback_text_label, custom.fallbackText),
                modifier = Modifier.padding(ChatTheme.space.small)
            )
        }
    }
}

@Composable
@Preview
private fun PreviewCustom() {
    PreviewMessageItemBase(
        message = MessagePreviewProvider().custom.asMessage("custom"),
        showSender = true,
    )
}
