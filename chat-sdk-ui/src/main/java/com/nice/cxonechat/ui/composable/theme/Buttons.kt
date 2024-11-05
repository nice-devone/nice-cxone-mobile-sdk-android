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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons.AutoMirrored.Outlined
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
internal fun ChatTheme.buttonColors(isDefault: Boolean): ButtonColors {
    val background = if (isDefault) colorScheme.primary else colorScheme.background
    return ButtonDefaults.buttonColors(
        containerColor = background,
        contentColor = contentColorFor(background)
    )
}

@Composable
internal fun ChatTheme.OutlinedButton(
    text: String,
    modifier: Modifier = Modifier,
    isDefault: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        colors = buttonColors(isDefault),
    ) {
        Text(text)
    }
}

@Composable
internal fun ChatTheme.ButtonRow(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = space.large),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(space = space.medium),
            verticalAlignment = Alignment.CenterVertically,
            content = content,
        )
    }
}

@Composable
internal fun ChatTheme.SelectableIconButton(
    icon: ImageVector,
    description: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    backgroundModifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val selectedColor = if (selected) colorScheme.tertiary else colorScheme.secondary
    val coloredBackgroundModifier = backgroundModifier.background(
        color = selectedColor,
        shape = RoundedCornerShape(8.dp)
    )
    IconButton(
        onClick = onClick,
        modifier = modifier.then(coloredBackgroundModifier)
    ) {
        Icon(
            icon,
            tint = contentColorFor(selectedColor),
            modifier = Modifier.padding(4.dp),
            contentDescription = description
        )
    }
}

@Preview
@Composable
internal fun PreviewButtons() {
    ChatTheme {
        Surface {
            Column {
                ChatTheme.ButtonRow {
                    ChatTheme.OutlinedButton("Default", isDefault = true, onClick = { })
                    ChatTheme.OutlinedButton("Normal", onClick = { })
                }
                val selected = remember { mutableStateOf(false) }
                ChatTheme.SelectableIconButton(
                    icon = Outlined.Send,
                    description = "",
                    selected = selected.value
                ) {
                    selected.value = !selected.value
                }
            }
        }
    }
}
