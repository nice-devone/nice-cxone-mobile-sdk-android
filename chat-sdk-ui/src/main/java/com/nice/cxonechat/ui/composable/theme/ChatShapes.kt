/*
 * Copyright (c) 2021-2023. NICE Ltd. All rights reserved.
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

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Shape
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
}

internal val LocalChatShapes = staticCompositionLocalOf {
    ChatShapes()
}
