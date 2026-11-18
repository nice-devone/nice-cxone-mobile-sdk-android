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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.nice.cxonechat.ui.composable.conversation.model.Action.ReplyButton
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import com.nice.cxonechat.ui.composable.theme.ChatTheme.chatShapes
import com.nice.cxonechat.ui.composable.theme.ChatTheme.chatTypography
import com.nice.cxonechat.ui.composable.theme.ChatTheme.space
import com.nice.cxonechat.ui.util.preview.message.UiSdkReplyButton

@Composable
internal fun Chip(
    text: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    enabled: Boolean = true,
    selected: Boolean = false,
    onSelected: () -> Unit,
) {
    Surface(
        color = ChatTheme.chatColors.token.background.surface.emphasis,
        shape = chatShapes.chip,
        modifier = modifier
            .selectable(
                selected = selected,
                enabled = enabled,
                role = Role.RadioButton,
                onClick = onSelected,
            )
            .semantics(mergeDescendants = true) {
                description?.let { contentDescription = it }
            }
            .defaultMinSize(space.chipMinSize, space.chipMinSize)
            .minimumInteractiveComponentSize()
            .chipWidth(text)
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(space.chipPadding)
        ) {
            Text(
                text = text,
                color = ChatTheme.chatColors.token.brand.primary,
                style = ChatTheme.chatTypography.chipText,
            )
        }
    }
}

/**
 * A chip whose text does not fit [Space.maxButtonWidth] on one line fills the row instead of wrapping
 * to a new line while staying pinned to that width, matching the Figma design (node 63819-71662): a long
 * option is drawn as a full-width row, not a narrow, multi-line chip. Short text keeps the normal
 * hug-content chip capped at [Space.maxButtonWidth].
 *
 * Decided by measuring the text's own single-line width rather than a character-count guess, since the
 * rendered width of a given string depends on the font and the characters themselves.
 */
@Composable
private fun Modifier.chipWidth(text: String): Modifier {
    val style = chatTypography.chipText
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val chipPadding = space.chipPadding
    val maxButtonWidth = space.maxButtonWidth
    val fillsRow = remember(text, style, density, layoutDirection, chipPadding, maxButtonWidth) {
        val horizontalPadding = chipPadding.calculateLeftPadding(layoutDirection) +
            chipPadding.calculateRightPadding(layoutDirection)
        val availableTextWidthPx = with(density) { (maxButtonWidth - horizontalPadding).roundToPx() }
        val singleLineWidthPx = textMeasurer.measure(text = text, style = style, maxLines = 1, softWrap = false).size.width
        singleLineWidthPx > availableTextWidthPx
    }
    return if (fillsRow) fillMaxWidth() else sizeIn(maxWidth = maxButtonWidth)
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
        description = action.description,
        enabled = enabled,
        selected = selected,
    ) {
        onSelected(action)
    }
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
                        onActionClicked = {}
                    ),
                    onSelected = {},
                    selected = false,
                )
                Chip(
                    action = ReplyButton(
                        action = UiSdkReplyButton("Random cat", "https://http.cat/203"),
                        onActionClicked = {}
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
                        onActionClicked = {}
                    ),
                    onSelected = {},
                    selected = false,
                )
            }
        }
    }
}
