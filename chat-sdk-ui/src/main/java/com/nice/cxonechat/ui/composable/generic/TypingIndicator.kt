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

package com.nice.cxonechat.ui.composable.generic

import androidx.compose.animation.core.InfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import kotlin.math.roundToInt

/**
 * An animated "typing indicator".
 *
 * @param color color of dots
 * @param modifier modifiers to use and pass
 * @param dotSize diameter of each dot
 * @param dotSpace space between dots
 * @param waveHeight height of dot wave
 * @param dotCount # of dots in wave
 * @param animationDuration duration, in ms, of each animation step, ie a dot moving up
 * @param loopDelay delay, in ms, between successive waves
 */
@Composable
internal fun TypingIndicator(
    color: Color = ChatTheme.colorScheme.primary,
    modifier: Modifier = Modifier,
    dotSize: Dp = 8.dp,
    dotSpace: Dp = 4.dp,
    waveHeight: Dp = 4.dp,
    dotCount: Int = 3,
    animationDuration: Int = 250,
    loopDelay: Int = 500,
) {
    val transition = rememberInfiniteTransition("typingIndicator")
    val totalDuration = (dotCount + 1) * animationDuration + loopDelay
    val phases = List(dotCount) { index ->
        transition.animateDotPhase(index, animationDuration, totalDuration)
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(dotSpace),
        modifier = modifier.height(dotSize + waveHeight)
    ) {
        phases.forEach {
            TypingIndicatorDot(it.value, color, waveHeight, dotSize)
        }
    }
}

@Composable
private fun InfiniteTransition.animateDotPhase(
    index: Int,
    animationDuration: Int,
    totalDuration: Int,
): State<Float> {
    val delay = animationDuration * index

    return animateFloat(
        initialValue = 0f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            keyframes {
                durationMillis = totalDuration
                0f at delay
                1f at delay + animationDuration
                1f at delay + animationDuration * 2
                0f at delay + animationDuration * 3
            }
        ),
        label = "state$index"
    )
}

@Composable
private fun TypingIndicatorDot(
    phase: Float,
    color: Color,
    waveHeight: Dp,
    size: Dp
) {
    val offset = ((1.0 - phase) * waveHeight.value * 2).roundToInt()
    val alpha0 = 0.48f
    val alpha1 = 0.20f
    val muted = color.copy(alpha = alpha0 + (1 - phase) * (alpha0 - alpha1))

    Box(
        modifier = Modifier
            .offset {
                IntOffset(0, offset)
            }
            .size(size, size)
            .clip(CircleShape)
            .background(muted)
    )
}

@Preview
@Composable
private fun PreviewTypingIndicator() {
    MaterialTheme {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.background
        ) {
            TypingIndicator(
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(8.dp)
            )
        }
    }
}
