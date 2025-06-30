/*
 * Copyright (c) 2021-2025. NICE Ltd. All rights reserved.
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
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewLightDark
import coil3.compose.AsyncImage
import com.nice.cxonechat.ui.composable.conversation.model.Action.ReplyButton
import com.nice.cxonechat.ui.composable.generic.forwardingPainter
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import com.nice.cxonechat.ui.composable.theme.ChatTheme.chatShapes
import com.nice.cxonechat.ui.composable.theme.ChatTheme.colorScheme
import com.nice.cxonechat.ui.composable.theme.ChatTheme.space
import com.nice.cxonechat.ui.util.preview.message.UiSdkReplyButton

@Composable
internal fun Chip(
    text: String,
    modifier: Modifier = Modifier,
    image: String? = null,
    description: String? = null,
    enabled: Boolean = true,
    selected: Boolean = false,
    colors: ChipColors = ChipDefaults.chipColors(),
    onSelected: () -> Unit,
) {
    Surface(
        color = colors.containerColor(enabled || selected),
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
            .defaultMinSize(space.chipMinSize, space.chipMinSize)
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(space.chipPadding)
        ) {
            if (image != null) {
                AsyncImage(
                    model = image,
                    placeholder = forwardingPainter(
                        painter = rememberVectorPainter(image = Outlined.Downloading),
                        colorFilter = ColorFilter.tint(LocalContentColor.current)
                    ),
                    contentDescription = null,
                    modifier = Modifier
                        .size(space.chipIconSize)
                        .padding(end = space.small)
                )
            }
            Text(
                text = text,
                style = ChatTheme.chatTypography.chipText,
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
    colors: ChipColors = ChipDefaults.chipColors(),
) {
    Chip(
        text = action.text,
        modifier = modifier,
        image = action.media?.url,
        description = action.description,
        enabled = enabled,
        selected = selected,
        colors = colors,
    ) {
        onSelected(action)
    }
}

internal object ChipDefaults {
    @Composable
    fun chipColors(
        containerColor: Color = colorScheme.primary,
        disabledContainerColor: Color = colorScheme.onSurface.copy(alpha = 0.38f),
    ) = ChipColors(
        containerColor = containerColor,
        disabledContainerColor = disabledContainerColor,
    )
}

@Immutable
internal data class ChipColors(
    private val containerColor: Color = Color.Unspecified,
    private val disabledContainerColor: Color = Color.Unspecified,
) {
    @Stable
    fun containerColor(enabled: Boolean): Color = if (enabled) containerColor else disabledContainerColor
}

@PreviewLightDark
@Composable
@Suppress("MaxLineLength")
private fun ChipPreview() {
    ChatTheme {
        Surface {
            Column(
                verticalArrangement = Arrangement.spacedBy(space.medium)
            ) {
                Chip(
                    action = ReplyButton(
                        action = UiSdkReplyButton("Yes"),
                        sendMessage = {}
                    ),
                    onSelected = {},
                    selected = false,
                )
                Chip(
                    action = ReplyButton(
                        action = UiSdkReplyButton("Random cat", "https://http.cat/203"),
                        sendMessage = {}
                    ),
                    onSelected = {},
                    selected = false,
                )
                Chip(
                    action = ReplyButton(
                        action = UiSdkReplyButton(
                            "Lorem ipsum dolor sit amet, consectetuer adipiscing elit. " +
                                    "Morbi commodo, ipsum sed pharetra gravida," +
                                    " orci magna rhoncus neque.",
                        ),
                        sendMessage = {}
                    ),
                    onSelected = {},
                    selected = false,
                )
            }
        }
    }
}
