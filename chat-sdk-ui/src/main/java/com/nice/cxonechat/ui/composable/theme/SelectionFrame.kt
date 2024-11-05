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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons.Outlined
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.nice.cxonechat.ui.composable.theme.ChatTheme.space

@Composable
internal fun ChatTheme.SelectionFrame(
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    content: @Composable () -> Unit,
) {
    val strokeWidth = if (selected) space.selectedFrameWidth else space.unselectedFrameWidth

    Surface(
        modifier = modifier,
        shape = chatShapes.selectionFrame,
        shadowElevation = strokeWidth,
        tonalElevation = strokeWidth,
        content = content,
        border = BorderStroke(
            strokeWidth,
            if (selected) MaterialTheme.colorScheme.primary else LocalContentColor.current
        )
    )
}

@Composable
@Preview
private fun PreviewSelectionFrame() {
    var selected by remember { mutableStateOf(false) }
    ChatTheme {
        Column(
            modifier = Modifier.padding(space.small),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ChatTheme.SelectionFrame(
                modifier = Modifier.padding(space.medium),
                selected = selected
            ) {
                Icon(Outlined.Favorite, contentDescription = null, modifier = Modifier.padding(space.medium))
            }
            Switch(selected, onCheckedChange = { selected = it })
        }
    }
}
