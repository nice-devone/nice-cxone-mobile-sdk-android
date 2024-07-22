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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.nice.cxonechat.ui.composable.conversation.model.Action
import com.nice.cxonechat.ui.composable.conversation.model.Action.ReplyButton
import com.nice.cxonechat.ui.composable.conversation.model.PreviewMessageProvider
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import com.nice.cxonechat.ui.composable.theme.ChatTheme.chatColors

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ChipGroup(
    actions: Iterable<Action>,
    modifier: Modifier = Modifier,
    selection: Action? = null,
    onSelect: (Action) -> Unit = {},
) {
    FlowRow(
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

private val actions = listOf(
        ReplyButton(
            action = PreviewMessageProvider.ReplyButton("Some text"),
            sendMessage = { }
        ),
        ReplyButton(
            action = PreviewMessageProvider.ReplyButton("Random cat", "https://http.cat/203"),
            sendMessage = { }
        )
    )

@Preview
@Composable
private fun SelectableChipGroupPreview() {
    ChatTheme {
        var selected: Action? by remember { mutableStateOf(null) }

        Column {
            ChipGroup(
                actions = actions,
                selection = selected,
            ) {
                selected = it
            }
            Button(onClick = { selected = null }) {
                Text(text = "Reset selected")
            }
            Row {
                Text(
                    text = "Last selected: ${(selected as? ReplyButton)?.text.orEmpty()}",
                    color = chatColors.agent.foreground
                )
            }
        }
    }
}

@Preview
@Composable
private fun ReusableChipGroupPreview() {
    ChatTheme {
        var selected: Action? by remember { mutableStateOf(null) }

        Column {
            ChipGroup(
                actions = actions,
            ) {
                selected = it
            }
            Button(onClick = { selected = null }) {
                Text(text = "Reset selected")
            }
            Row {
                Text(
                    text = "Last selected: ${(selected as? ReplyButton)?.text.orEmpty()}",
                    color = chatColors.agent.foreground
                )
            }
        }
    }
}
