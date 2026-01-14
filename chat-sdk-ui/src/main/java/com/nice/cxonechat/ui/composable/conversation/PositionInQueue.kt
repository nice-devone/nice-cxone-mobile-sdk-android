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

package com.nice.cxonechat.ui.composable.conversation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.airbnb.lottie.LottieProperty
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.airbnb.lottie.compose.rememberLottieDynamicProperties
import com.airbnb.lottie.compose.rememberLottieDynamicProperty
import com.nice.cxonechat.ui.R
import com.nice.cxonechat.ui.R.string
import com.nice.cxonechat.ui.composable.HeaderBar
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import com.nice.cxonechat.ui.composable.theme.ChatTheme.chatColors
import com.nice.cxonechat.ui.composable.theme.ChatTheme.space

@Composable
internal fun PositionInQueue(
    position: Int,
    modifier: Modifier = Modifier,
) {
    val title = when {
        position == 1 -> stringResource(id = string.position_in_queue_next)
        else -> stringResource(id = string.position_in_queue_cardinal, position)
    }
    Column(
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .testTag("position_in_queue_content_view")
    ) {
        HeaderBar(
            titleText = title,
            containerColor = chatColors.token.background.surface.emphasis,
            messageText = stringResource(string.position_in_queue_supporting_text),
            leadingContent = { LoadingAnimationView() }
        )
    }
}

private const val STROKE_VALUE = "Stroke 1"

@Composable
private fun LoadingAnimationView() {
    val composition = rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.loading_sand)).value
    val progress = animateLottieCompositionAsState(
        composition,
        iterations = LottieConstants.IterateForever,
        speed = 1.0f
    ).value
    val dynamicProperties = rememberLottieDynamicProperties(
        rememberLottieDynamicProperty(
            property = LottieProperty.STROKE_COLOR,
            value = chatColors.token.brand.onPrimary.toArgb(),
            keyPath = arrayOf("**", "Group 1", STROKE_VALUE)
        ),
        rememberLottieDynamicProperty(
            property = LottieProperty.STROKE_COLOR,
            value = chatColors.token.brand.onPrimary.toArgb(),
            keyPath = arrayOf("**", "Group 2", STROKE_VALUE)
        ),
        rememberLottieDynamicProperty(
            property = LottieProperty.STROKE_COLOR,
            value = chatColors.token.brand.onPrimary.toArgb(),
            keyPath = arrayOf("**", "Group 3", STROKE_VALUE)
        ),
        rememberLottieDynamicProperty(
            property = LottieProperty.STROKE_COLOR,
            value = chatColors.token.brand.onPrimary.toArgb(),
            keyPath = arrayOf("**", "Group 4", STROKE_VALUE)
        ),
    )

    Box(
        modifier = Modifier
            .padding(vertical = space.large, horizontal = space.medium)
            .size(space.positionInQueueIconSize)
            .testTag("loading_animation_view")
            .background(color = chatColors.token.brand.primary, shape = CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        LottieAnimation(
            composition = composition,
            progress = { progress },
            modifier = Modifier.size(space.xxl),
            dynamicProperties = dynamicProperties
        )
    }
}

@PreviewLightDark
@Composable
private fun PositionInQueue_Preview() {
    ChatTheme {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
        ) {
            PositionInQueue(position = 4)
        }
    }
}
