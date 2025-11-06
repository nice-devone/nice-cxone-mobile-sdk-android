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

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.SwipeToDismissBoxValue.EndToStart
import androidx.compose.material3.SwipeToDismissBoxValue.StartToEnd
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nice.cxonechat.ui.composable.theme.ChatTheme.colorScheme
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ChatTheme.SwipeToDismiss(
    dismissState: SwipeToDismissBoxState,
    directions: Map<SwipeToDismissBoxValue, SwipeBackground>,
    modifier: Modifier = Modifier,
    background: @Composable RowScope.() -> Unit = {
        SwipeToDismissBackground(
            dismissState = dismissState,
            directions
        )
    },
    onDismiss: (SwipeToDismissBoxValue) -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    SwipeToDismissBox(
        state = dismissState,
        modifier = Modifier
            .testTag("swipe_box")
            .then(modifier),
        enableDismissFromStartToEnd = directions.contains(StartToEnd),
        enableDismissFromEndToStart = directions.contains(EndToStart),
        backgroundContent = background,
        onDismiss = onDismiss,
        content = {
            // Wrap the row content in a card or the dismiss background bleeds through
            // the content when swiping.
            Card(shape = shapes.medium.copy(CornerSize(0.dp))) {
                content()
            }
        }
    )
}

@Immutable
internal data class SwipeBackground(
    val color: Color? = null,
    val icon: ImageVector? = null,
    val iconTint: Color? = null,
    val contentDescription: String? = null,
)

@ExperimentalMaterial3Api
@Composable
internal fun RowScope.SwipeToDismissBackground(
    dismissState: SwipeToDismissBoxState,
    directions: Map<SwipeToDismissBoxValue, SwipeBackground>,
) {
    // Don't draw background if there's no swipe happening
    dismissState.dismissDirection
    val entry = remember(directions, dismissState.targetValue) {
        directions[dismissState.targetValue] ?: SwipeBackground()
    }
    val animatedColor by animateColorAsState(
        entry.color ?: colorScheme.background
    )

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(animatedColor),
        horizontalArrangement = if (dismissState.targetValue == EndToStart) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .minimumInteractiveComponentSize(),
        ) {
            val icon = entry.icon
            if (icon != null) {
                Icon(
                    icon,
                    contentDescription = entry.contentDescription,
                    tint = entry.iconTint ?: Color.Unspecified,
                    modifier = Modifier.size(ChatTheme.space.swipeToDismissIconSize)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showSystemUi = true, showBackground = true)
@Composable
private fun SwipeToDismissPreview() {
    val dismissState = rememberSwipeToDismissBoxState()

    ChatTheme {
        Column(Modifier.padding(8.dp)) {
            val scope = rememberCoroutineScope()
            Button(onClick = { scope.launch { dismissState.reset() } }) {
                Text("Dismiss")
            }
            Card(colors = CardDefaults.cardColors(containerColor = Color.Yellow)) {
                ChatTheme.SwipeToDismiss(
                    dismissState = dismissState,
                    directions = mapOf(
                        EndToStart to SwipeBackground(
                            color = Color.Red,
                            icon = Icons.Default.Delete,
                            iconTint = Color.White,
                            contentDescription = "Delete"
                        ),
                        StartToEnd to SwipeBackground(
                            color = Color.Green,
                            icon = Icons.Default.Settings,
                            iconTint = Color.White,
                            contentDescription = "Settings"
                        )
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = colorScheme.background),
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
}
