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

import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Immutable
internal data class ChatShapes(
    val bubbleShapeToClient: Shape = DefaultChatShapes.bubbleShapeToClient,
    val bubbleShapeToAgent: Shape = DefaultChatShapes.bubbleShapeToAgent,
    val chatCardShape: Shape = DefaultChatShapes.chatCardShape,
    val chatCardPadding: PaddingValues = DefaultChatShapes.chatCardPadding,
    val chatVideoPlayer: Shape = DefaultChatShapes.chatVideoPlayerClip,
    val chatAudioPlayer: Shape = DefaultChatShapes.chatAudioPlayerClip,
    val chip: Shape = DefaultChatShapes.chip,
    val selectionFrame: Shape = DefaultChatShapes.selectionFrame,
)

internal object DefaultChatShapes {

    val bubbleShapeToClient = RoundedCornerShape(
        topStart = 20.dp,
        topEnd = 20.dp,
        bottomEnd = 20.dp,
        bottomStart = 0.dp
    )

    val bubbleShapeToAgent = RoundedCornerShape(
        topStart = 20.dp,
        topEnd = 20.dp,
        bottomEnd = 0.dp,
        bottomStart = 20.dp
    )

    val chatCardShape = RoundedCornerShape(16.dp)

    val chatCardPadding = PaddingValues(8.dp)

    val chatVideoPlayerClip = RoundedCornerShape(24.dp)

    val chatAudioPlayerClip = RoundedCornerShape(8.dp)

    val chip = RoundedCornerShape(8.dp)

    val selectionFrame = RoundedCornerShape(8.dp)
}

internal val LocalChatShapes = staticCompositionLocalOf {
    ChatShapes()
}

@Composable
@Preview
private fun PreviewShapes() {
    val shapes = listOf(
        "bubbleShapeToAgent" to ChatTheme.chatShapes.bubbleShapeToAgent,
        "bubbleShapeToClient" to ChatTheme.chatShapes.bubbleShapeToClient,
        "chatCardShape" to ChatTheme.chatShapes.chatCardShape,
        "chatVideoPlayer" to ChatTheme.chatShapes.chatVideoPlayer,
        "chatAudioPlayer" to ChatTheme.chatShapes.chatAudioPlayer,
        "chip" to ChatTheme.chatShapes.chip,
        "selectionFrame" to ChatTheme.chatShapes.selectionFrame,
    )
    ChatTheme {
        Surface {
            Column(
                verticalArrangement = spacedBy(8.dp),
                modifier = Modifier.padding(8.dp)
            ) {
                shapes.forEach { (label, shape) ->
                    Surface(
                        color = ChatTheme.colorScheme.primary,
                        contentColor = ChatTheme.colorScheme.onPrimary,
                        shape = shape,
                    ) {
                        Text(text = label, modifier = Modifier.padding(24.dp))
                    }
                }
            }
        }
    }
}
