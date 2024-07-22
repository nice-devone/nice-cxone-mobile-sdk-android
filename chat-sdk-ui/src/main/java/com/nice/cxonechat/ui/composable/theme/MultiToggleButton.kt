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

package com.nice.cxonechat.ui.composable.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.Divider
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
internal fun ChatTheme.MultiToggleButton(
    currentSelection: String,
    toggleStates: List<String>,
    modifier: Modifier = Modifier,
    onToggleChange: (String) -> Unit
) {
    val selectedTint = colors.primary
    val unselectedTint = Color.Unspecified

    Surface(
        modifier = modifier,
        shape = shapes.medium,
        border = BorderStroke(1.dp, Color.LightGray)
    ) {
        Row(
            modifier = Modifier
                .height(IntrinsicSize.Min)
        ) {
            toggleStates.forEachIndexed { index, label ->
                val isSelected = currentSelection.lowercase() == label.lowercase()
                val backgroundTint = if (isSelected) selectedTint else unselectedTint
                val textColor = if (isSelected) Color.White else Color.Unspecified

                if (index != 0) {
                    Divider(
                        color = Color.LightGray,
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(1.dp)
                    )
                }

                ToggleButton(
                    label = label,
                    isSelected = isSelected,
                    color = textColor,
                    backgroundColor = backgroundTint,
                    onToggleChange = onToggleChange
                )
            }
        }
    }
}

@Composable
private fun ToggleButton(
    label: String,
    isSelected: Boolean,
    color: Color,
    backgroundColor: Color,
    onToggleChange: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .background(backgroundColor)
            .padding(vertical = 6.dp, horizontal = 8.dp)
            .toggleable(
                value = isSelected,
                enabled = true,
                onValueChange = { selected ->
                    if (selected) {
                        onToggleChange(label)
                    }
                }
            )
    ) {
        Text(label, color = color, modifier = Modifier.padding(4.dp))
    }
}

@Preview
@Composable
private fun MultiToggleButtonPreview() {
    val options = listOf("First", "Second", "Third")

    ChatTheme {
        Column(
            Modifier
                .fillMaxHeight(1f)
                .fillMaxWidth(1f)
        ) {
            var selection by remember { mutableStateOf(options.first()) }

            Row {
                Text(selection)
            }
            Row(horizontalArrangement = Arrangement.Center) {
                ChatTheme.MultiToggleButton(currentSelection = selection, toggleStates = options) {
                    selection = it
                }
            }
        }
    }
}
