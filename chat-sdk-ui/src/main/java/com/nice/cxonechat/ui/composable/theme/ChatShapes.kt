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

import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CornerSize
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
    val bubbleShapeToClient: BubbleShapes = BubbleShapes(
        bubbleFirstShape = DefaultChatShapes.bubbleSoloShape.copy(
            bottomStart = DefaultChatShapes.bubbleSmallCornerSize,
        ),
        bubbleMiddleShape = DefaultChatShapes.bubbleSoloShape.copy(
            topStart = DefaultChatShapes.bubbleSmallCornerSize,
            bottomStart = DefaultChatShapes.bubbleSmallCornerSize,
        ),
        bubbleLastShape = DefaultChatShapes.bubbleSoloShape.copy(
            topStart = DefaultChatShapes.bubbleSmallCornerSize,
        ),
    ),
    val bubbleShapeToAgent: BubbleShapes = BubbleShapes(
        bubbleFirstShape = DefaultChatShapes.bubbleSoloShape.copy(
            bottomEnd = DefaultChatShapes.bubbleSmallCornerSize,
        ),
        bubbleMiddleShape = DefaultChatShapes.bubbleSoloShape.copy(
            topEnd = DefaultChatShapes.bubbleSmallCornerSize,
            bottomEnd = DefaultChatShapes.bubbleSmallCornerSize,
        ),
        bubbleLastShape = DefaultChatShapes.bubbleSoloShape.copy(
            topEnd = DefaultChatShapes.bubbleSmallCornerSize,
        ),
    ),
    val bubbleSoloShape: Shape = DefaultChatShapes.bubbleSoloShape,
    val chip: Shape = DefaultChatShapes.chip,
    val selectionFrame: Shape = DefaultChatShapes.selectionFrame,
)

@Immutable
internal data class BubbleShapes(
    val bubbleFirstShape: Shape,
    val bubbleMiddleShape: Shape,
    val bubbleLastShape: Shape,
)

internal object DefaultChatShapes {
    val bubbleSmallCornerSize = CornerSize(4.dp)

    val bubbleSoloShape = RoundedCornerShape(20.dp)

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
        "bubbleShapeToClientFirst" to ChatTheme.chatShapes.bubbleShapeToClient.bubbleFirstShape,
        "bubbleShapeToClientMiddle" to ChatTheme.chatShapes.bubbleShapeToClient.bubbleMiddleShape,
        "bubbleShapeToClientLast" to ChatTheme.chatShapes.bubbleShapeToClient.bubbleLastShape,
        "bubbleShapeToAgentFirst" to ChatTheme.chatShapes.bubbleShapeToAgent.bubbleFirstShape,
        "bubbleShapeToAgentMiddle" to ChatTheme.chatShapes.bubbleShapeToAgent.bubbleMiddleShape,
        "bubbleShapeToAgentLast" to ChatTheme.chatShapes.bubbleShapeToAgent.bubbleLastShape,
        "bubbleSoloShape" to ChatTheme.chatShapes.bubbleSoloShape,
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
