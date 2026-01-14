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
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.nice.cxonechat.ui.composable.conversation.model.Action
import com.nice.cxonechat.ui.composable.conversation.model.Action.ReplyButton
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import com.nice.cxonechat.ui.composable.theme.ChatTheme.chatColors
import com.nice.cxonechat.ui.composable.theme.LocalSpace
import com.nice.cxonechat.ui.util.preview.message.UiSdkReplyButton

@Composable
internal fun ChipGroup(
    actions: Iterable<Action>,
    modifier: Modifier = Modifier,
    selection: Action? = null,
    onSelect: (Action) -> Unit = {},
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(LocalSpace.current.medium),
        verticalArrangement = Arrangement.spacedBy(LocalSpace.current.medium),
        modifier = modifier.selectableGroup(),
    ) {
        for (action in actions) {
            if (action is ReplyButton) {
                Chip(
                    action = action,
                    onSelected = {
                        onSelect(it)
                        it.onClick()
                    },
                    enabled = selection == null,
                )
            }
        }
    }
}

@Suppress(
    "MaxLineLength",
    "ArgumentListWrapping",
)
private fun actions(count: Int = 2): List<ReplyButton> = buildList(capacity = count) {
    for (i in 0 until count - 1) {
        add(
            ReplyButton(
                action = UiSdkReplyButton("Chip $i"),
                onActionClicked = { }
            )
        )
    }
    if (this.lastIndex % 2 == 0) {
        add(
            ReplyButton(
                action = UiSdkReplyButton("Some very very very loooooooong text, maybe too long for normal use, but you never know, right?"),
                onActionClicked = { }
            )
        )
    } else {
        add(
            ReplyButton(
                action = UiSdkReplyButton("Random cat", "https://http.cat/203"),
                onActionClicked = { }
            )
        )
    }
}

@PreviewLightDark
@Composable
private fun SelectableChipGroupPreview() {
    var selected: Action? by remember { mutableStateOf(null) }
    ChatTheme {
        Surface {
            Column {
                ChipGroup(
                    actions = actions(6),
                    selection = selected,
                ) {
                    selected = it
                }
                OutlinedButton(onClick = { selected = null }) {
                    Text(text = "Reset selected")
                }
                Row {
                    Text(
                        text = "Last selected: ${(selected as? ReplyButton)?.text.orEmpty()}",
                        color = chatColors.token.brand.primary
                    )
                }
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun ReusableChipGroupPreview() {
    var selected: Action? by remember { mutableStateOf(null) }
    ChatTheme {
        Surface {
            Column {
                ChipGroup(
                    actions = actions(),
                ) {
                    selected = it
                }
                OutlinedButton(onClick = { selected = null }) {
                    Text(text = "Reset selected")
                }
                Row {
                    Text(
                        text = "Last selected: ${(selected as? ReplyButton)?.text.orEmpty()}",
                        color = chatColors.token.brand.primary
                    )
                }
            }
        }
    }
}
