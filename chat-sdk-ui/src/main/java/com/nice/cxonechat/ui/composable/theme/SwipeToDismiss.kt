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

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ChatTheme.SwipeToDismiss(
    dismissState: SwipeToDismissBoxState,
    icon: ImageVector,
    contentDescription: String,
    modifier: Modifier = Modifier,
    directions: Set<SwipeToDismissBoxValue> = setOf(SwipeToDismissBoxValue.EndToStart),
    background: @Composable RowScope.() -> Unit = {
        SwipeToDismissBackground(
            dismissState = dismissState,
            icon = icon,
            contentDescription = contentDescription
        )
    },
    content: @Composable ColumnScope.() -> Unit,
) {
    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier,
        enableDismissFromStartToEnd = directions.contains(SwipeToDismissBoxValue.StartToEnd),
        enableDismissFromEndToStart = directions.contains(SwipeToDismissBoxValue.EndToStart),
        backgroundContent = background,
        content = {
            // Wrap the row content in a card or the dismiss background bleeds through
            // the content when swiping.
            Card(shape = shapes.medium.copy(CornerSize(0.dp))) {
                content()
            }
        }
    )
}

@ExperimentalMaterial3Api
@Composable
internal fun ChatTheme.SwipeToDismissBackground(
    dismissState: SwipeToDismissBoxState,
    icon: ImageVector,
    contentDescription: String,
) {
    // Don't draw background if there's no swipe happening
    dismissState.dismissDirection

    val color by animateColorAsState(
        when (dismissState.targetValue) {
            SwipeToDismissBoxValue.Settled -> colorScheme.background
            else -> Color.Red
        },
        label = "color"
    )
    val alignment = Alignment.CenterEnd
    val scale by animateFloatAsState(
        if (dismissState.targetValue == SwipeToDismissBoxValue.Settled) 0.75f else 1f,
        label = "scale"
    )

    Box(
        Modifier
            .fillMaxSize()
            .background(color)
            .padding(horizontal = 20.dp),
        contentAlignment = alignment
    ) {
        Icon(
            icon,
            contentDescription = contentDescription,
            modifier = Modifier.scale(scale)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showSystemUi = true, showBackground = true)
@Composable
private fun SwipeToDismissPreview() {
    val dismissState = rememberSwipeToDismissBoxState()

    ChatTheme {
        Card(colors = CardDefaults.cardColors(containerColor = Color.Yellow)) {
            ChatTheme.SwipeToDismiss(
                dismissState = dismissState,
                icon = Icons.Default.Delete,
                contentDescription = "Delete",
                modifier = Modifier.fillMaxWidth()
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = ChatTheme.colorScheme.background),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(ChatTheme.space.defaultPadding)
                    ) {
                        Text("Some Row")
                    }
                }
            }
        }
    }
}
