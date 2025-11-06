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

package com.nice.cxonechat.ui.composable.theme

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.PreviewLightDark

@Composable
internal fun ChatTheme.SingleChoiceSegmentedButton(
    currentSelection: String,
    toggleStates: List<String>,
    modifier: Modifier = Modifier,
    onToggleChange: (String) -> Unit,
) {
    SingleChoiceSegmentedButtonRow(modifier) {
        toggleStates.forEachIndexed { index, label ->
            val isSelected = currentSelection.lowercase() == label.lowercase()
            SegmentedButton(
                shape = SegmentedButtonDefaults.itemShape(index, count = toggleStates.size),
                onClick = { onToggleChange(label) },
                selected = isSelected,
                colors = SegmentedButtonDefaults.colors(
                    activeContainerColor = colorScheme.primary,
                    activeContentColor = colorScheme.onPrimary,
                    activeBorderColor = colorScheme.primary,
                    inactiveContentColor = chatColors.token.content.secondary,
                    inactiveBorderColor = chatColors.token.border.default,
                ),
                modifier = Modifier.testTag("active_thread_toggle_button_$index"),
            ) {
                Text(text = label, style = chatTypography.segmentedButtonText)
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun MultiToggleButtonPreview() {
    val options = listOf("First", "Second", "Third")
    ChatTheme {
        Surface(color = ChatTheme.chatColors.token.background.default) {
            Column {
                var selection by remember { mutableStateOf(options.first()) }

                Row {
                    Text(selection)
                }
                Row(horizontalArrangement = Arrangement.Center) {
                    ChatTheme.SingleChoiceSegmentedButton(currentSelection = selection, toggleStates = options) {
                        selection = it
                    }
                }
            }
        }
    }
}
