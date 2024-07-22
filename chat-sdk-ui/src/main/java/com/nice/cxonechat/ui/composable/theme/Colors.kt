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

import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.LocalContentColor
import androidx.compose.material.LocalRippleConfiguration
import androidx.compose.material.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.takeOrElse

/**
 * Expands [contentColorFor] pairs with content color for [Color.Transparent].
 * @see [contentColorFor].
 */
@Composable
@ReadOnlyComposable
fun contentColorFor(backgroundColor: Color): Color = when (backgroundColor) {
    Color.Transparent -> ChatTheme.colors.primary
    ChatTheme.chatColors.agent.background -> ChatTheme.chatColors.agent.foreground
    ChatTheme.chatColors.customer.background -> ChatTheme.chatColors.customer.foreground
    else -> ChatTheme.colors.contentColorFor(backgroundColor)
}.takeOrElse { LocalContentColor.current }

/**
 * Applies [lerp] between this color and [androidx.compose.material.ripple.RippleTheme.defaultColor] and
 * [androidx.compose.material.ripple.RippleTheme.rippleAlpha],
 * [androidx.compose.material.ripple.RippleAlpha.pressedAlpha] as a fraction.
 */
@OptIn(ExperimentalMaterialApi::class)
@Composable
internal fun Color.rippleVariant(): Color = lerp(
    start = this,
    stop = LocalRippleConfiguration.current?.color ?: Color.Unspecified,
    fraction = LocalRippleConfiguration.current?.rippleAlpha?.pressedAlpha ?: 0.1f,
)
