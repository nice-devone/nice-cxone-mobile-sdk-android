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

package com.nice.cxonechat.tool

import com.nice.cxonechat.Cancellable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit.MILLISECONDS
import kotlin.time.Duration

internal inline fun <T> awaitResult(
    timeout: Duration? = null,
    body: (trigger: (T) -> Unit) -> Any,
): T {
    val latch = CountDownLatch(1)
    var result: T? = null
    val bodyResult = body {
        result = it
        latch.countDown()
    }
    try {
        when (timeout) {
            null -> latch.await()
            else -> latch.await(timeout.inWholeMilliseconds, MILLISECONDS)
        }
    } finally {
        if (bodyResult is Cancellable) bodyResult.cancel()
    }
    @Suppress("UNCHECKED_CAST")
    return result as T
}
