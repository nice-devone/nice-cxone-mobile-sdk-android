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

package com.nice.cxonechat.util

import kotlinx.coroutines.channels.BufferOverflow.DROP_OLDEST
import kotlinx.coroutines.flow.MutableSharedFlow

/**
 * Creates a [MutableSharedFlow] with [DROP_OLDEST] overflow policy and a default extra buffer
 * capacity of [DEFAULT_SHARED_FLOW_EXTRA_BUFFER], sized to absorb socket message bursts without
 * dropping under normal conditions.
 *
 * [DROP_OLDEST] is chosen so that slow collectors never block producers — stale values matter less
 * than keeping the flow responsive to new events.
 *
 * @param T Type of elements emitted by the flow.
 * @param replay Number of past values replayed to new subscribers (default 0).
 * @param extraBufferCapacity Additional buffer slots beyond [replay] (default [DEFAULT_SHARED_FLOW_EXTRA_BUFFER]).
 */
internal fun <T> newBufferedSharedFlow(
    replay: Int = 0,
    extraBufferCapacity: Int = DEFAULT_SHARED_FLOW_EXTRA_BUFFER,
): MutableSharedFlow<T> = MutableSharedFlow(
    replay = replay,
    extraBufferCapacity = extraBufferCapacity,
    onBufferOverflow = DROP_OLDEST,
)

internal const val DEFAULT_SHARED_FLOW_EXTRA_BUFFER = 64
