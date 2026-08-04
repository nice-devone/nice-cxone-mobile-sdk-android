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

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive

internal class RetryApiHandler(
    private val maxRetries: Int,
    private val retryIntervalMs: Long,
) {
    tailrec suspend fun <T> executeWithRetry(
        coroutineScope: CoroutineScope,
        action: suspend () -> T,
        onSuccess: (T) -> Unit,
        onFailure: (Throwable) -> Unit,
        retryCount: Int = 0,
    ) {
        coroutineScope.ensureActive()
        val run = runCatching { action() }
        if (run.isSuccess) {
            onSuccess(run.getOrThrow())
            return
        }
        val exception = run.exceptionOrNull()
        // runCatching catches CancellationException — rethrow to preserve structured concurrency.
        if (exception is CancellationException) throw exception
        if (retryCount >= maxRetries) {
            exception?.let { throwable -> onFailure(throwable) }
            return
        }
        coroutineScope.ensureActive()
        delay(retryIntervalMs)
        coroutineScope.ensureActive()
        executeWithRetry(coroutineScope, action, onSuccess, onFailure, retryCount + 1)
    }
}
