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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize.Min
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons.AutoMirrored.Outlined
import androidx.compose.material.icons.automirrored.outlined.ArrowBackIos
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
internal fun ChatTheme.IconMultiButton(
    buttons: Map<String, Painter?>,
    modifier: Modifier = Modifier,
    onClick: (String) -> Unit,
) {
    Surface(
        shape = shapes.medium,
        border = BorderStroke(1.dp, Color.LightGray),
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .height(Min)
        ) {
            val buttonIterable = remember(buttons::toList)
            buttonIterable.forEachIndexed { index, entry ->
                if (index != 0) {
                    Divider(
                        color = Color.LightGray,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                    )
                }

                IconButton(
                    label = entry.first,
                    icon = entry.second,
                    onClick = onClick
                )
            }
        }
    }
}

@Composable
private fun IconButton(
    label: String,
    icon: Painter?,
    onClick: (String) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp, horizontal = 8.dp)
            .clickable {
                onClick(label)
            }
    ) {
        if (icon != null) {
            Icon(painter = icon, contentDescription = label, modifier = Modifier.padding(4.dp))
        }
        Text(label, modifier = Modifier.padding(4.dp))
    }
}

@Preview
@Composable
private fun IconMultiButtonPreview() {
    val options = mapOf("First" to null, "Second" to null, "Third" to rememberVectorPainter(image = Outlined.ArrowBackIos))

    ChatTheme {
        Column(
            Modifier
                .fillMaxHeight(1f)
                .fillMaxWidth(1f)
        ) {
            var selection by remember { mutableStateOf<String?>(options.asIterable().first().key) }

            Row {
                Text(selection.orEmpty())
            }
            Column(Modifier.padding(8.dp)) {
                ChatTheme.IconMultiButton(buttons = options) {
                    selection = it
                }
            }
        }
    }
}
