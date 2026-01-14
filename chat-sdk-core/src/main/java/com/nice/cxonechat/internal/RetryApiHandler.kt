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

package com.nice.cxonechat.internal

import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

internal class RetryApiHandler(
    private val maxRetries: Int,
    private val retryIntervalMs: Long,
) {
    private var retryCount = 0
    private var isCancelled = false
    private val executor = Executors.newSingleThreadScheduledExecutor()
    private var scheduledFuture: ScheduledFuture<*>? = null

    fun <T> executeWithRetry(
        action: () -> T,
        onSuccess: (T) -> Unit,
        onFailure: (Throwable) -> Unit,
    ) {
        if (isCancelled) return
        executor.execute {
            runCatching { action() }
                .onSuccess { onSuccess(it) }
                .onFailure { throwable ->
                    if (retryCount < maxRetries) {
                        retryCount++
                        scheduledFuture = executor.schedule({
                            executeWithRetry(action, onSuccess, onFailure)
                        }, retryIntervalMs, TimeUnit.MILLISECONDS)
                    } else {
                        onFailure(throwable)
                    }
                }
        }
    }

    fun cancel() {
        isCancelled = true
        scheduledFuture?.cancel(true)
        executor.shutdownNow()
    }
}
