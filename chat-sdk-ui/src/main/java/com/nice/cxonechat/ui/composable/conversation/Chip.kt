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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons.Outlined
import androidx.compose.material.icons.outlined.Downloading
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.nice.cxonechat.ui.composable.conversation.model.Action.ReplyButton
import com.nice.cxonechat.ui.composable.conversation.model.PreviewMessageProvider
import com.nice.cxonechat.ui.composable.generic.forwardingPainter
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import com.nice.cxonechat.ui.composable.theme.ChatTheme.chatShapes
import com.nice.cxonechat.ui.composable.theme.ChatTheme.colorScheme
import com.nice.cxonechat.ui.composable.theme.ChatTheme.space

@Composable
internal fun Chip(
    text: String,
    modifier: Modifier = Modifier,
    image: String? = null,
    description: String? = null,
    enabled: Boolean = true,
    selected: Boolean = false,
    onSelected: () -> Unit,
) {
    val color = colorScheme.primary
    val disabledColor = colorScheme.onSurface.copy(alpha = 0.38f)

    Surface(
        color = if (enabled || selected) color else disabledColor,
        shape = chatShapes.chip,
        modifier = modifier
            .selectable(
                selected = selected,
                enabled = enabled,
                role = Role.RadioButton,
                onClick = onSelected,
            )
            .semantics {
                description?.let { contentDescription = it }
            }
            .padding(2.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier
                .padding(space.chipPadding)
                .defaultMinSize(space.clickableSize)
        ) {
            if (image != null) {
                AsyncImage(
                    model = image,
                    placeholder = forwardingPainter(
                        painter = rememberVectorPainter(image = Outlined.Downloading),
                        colorFilter = ColorFilter.tint(LocalContentColor.current)
                    ),
                    contentDescription = null,
                    modifier = Modifier.size(space.chipIconSize)
                )
            }
            Text(
                text = text,
                modifier = modifier
                    .padding(horizontal = 4.dp)
                    .defaultMinSize(minHeight = space.chipIconSize)
            )
        }
    }
}

@Composable
internal fun Chip(
    action: ReplyButton,
    onSelected: (ReplyButton) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    selected: Boolean = false,
) {
    Chip(
        text = action.text,
        modifier = modifier,
        image = action.media?.url,
        description = action.description,
        enabled = enabled,
        selected = selected,
    ) {
        onSelected(action)
    }
}

@Preview
@Composable
private fun ChipPreview() {
    ChatTheme {
        Column {
            Chip(
                action = ReplyButton(
                    action = PreviewMessageProvider.ReplyButton("Text"),
                    sendMessage = {}
                ),
                onSelected = {},
                selected = false,
            )
            Chip(
                action = ReplyButton(
                    action = PreviewMessageProvider.ReplyButton("Random cat", "https://http.cat/203"),
                    sendMessage = {}
                ),
                onSelected = {},
                selected = false,
            )
        }
    }
}
