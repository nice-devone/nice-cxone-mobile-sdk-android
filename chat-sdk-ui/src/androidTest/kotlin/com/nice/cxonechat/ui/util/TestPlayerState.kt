/*
 * Copyright (c) 2021-2026. NICE Ltd. All rights reserved.
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

package com.nice.cxonechat.ui.util

import androidx.compose.runtime.MutableLongState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import com.nice.cxonechat.ui.composable.player.PlayerState
import java.util.concurrent.TimeUnit
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

internal class TestPlayerState(
    available: Boolean = true,
    isPlaying: Boolean = false,
    durationMs: Long = TimeUnit.MINUTES.toMillis(1),
    positionMs: Long = TimeUnit.SECONDS.toMillis(20),
    progress: Float = 0.25f,
    canSeekForward: Boolean = true,
    canSeekBackward: Boolean = true,
) : PlayerState {
    override val available: MutableState<Boolean> = mutableStateOf(available)
    override val isPlaying: MutableState<Boolean> = mutableStateOf(isPlaying)
    override val duration: MutableState<Duration> = mutableStateOf(durationMs.milliseconds)
    override val position: MutableState<Duration> = mutableStateOf(positionMs.milliseconds)
    override val progress: MutableState<Float> = mutableStateOf(progress)
    override val canSeekForward: MutableState<Boolean> = mutableStateOf(canSeekForward)
    override val canSeekBackward: MutableState<Boolean> = mutableStateOf(canSeekBackward)
    override val seekBackIncrement: MutableLongState = mutableLongStateOf(10_000L)
    override val seekForwardIncrement: MutableLongState = mutableLongStateOf(10_000L)
}
