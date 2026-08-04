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

import com.nice.cxonechat.log.Level
import com.nice.cxonechat.log.Logger
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.sync.Mutex

internal interface Threading {

    /**
     * Per-Chat coordination mutexes. One [Guards] instance is created per [Threading] instance
     * (i.e. per Chat session) so all decorators derived from the same chat share the same locks.
     */
    @Suppress("UseDataClass")
    class Guards(
        val visitMutex: Mutex = Mutex(),
        val tokenRefreshMutex: Mutex = Mutex(),
    )

    val guards: Guards

    val backgroundDispatcher: CoroutineDispatcher

    val ioDispatcher: CoroutineDispatcher

    val mainDispatcher: CoroutineDispatcher

    val coroutineScope: CoroutineScope

    /**
     * Scope for fire-and-forget storage persistence writes, dispatched on [storageDispatcher].
     *
     * Lifecycle: a SupervisorJob child of [coroutineScope], outliving other children during
     * session shutdown so pending writes can complete before the scope is cancelled.
     */
    val storageWriteScope: CoroutineScope

    /**
     * Serial dispatcher used by [storageWriteScope] to serialise persistence writes.
     * Exposed for testing so that test doubles can verify the dispatcher without constructing
     * a full [ThreadingExecutor].
     */
    val storageDispatcher: CoroutineDispatcher

    companion object {

        @JvmName("getDefault")
        operator fun invoke(logger: Logger): Threading {
            val coroutineErrorHandler = CoroutineExceptionHandler { ctx, throwable ->
                val name = ctx[CoroutineName]?.name ?: "unknown"
                logger.log(Level.Error, "Coroutine failed ($name)", throwable)
            }
            val storageErrorHandler = CoroutineExceptionHandler { ctx, throwable ->
                val name = ctx[CoroutineName]?.name ?: "unknown"
                logger.log(Level.Error, "Storage write failed ($name)", throwable)
            }
            val coroutineScope = CoroutineScope(
                SupervisorJob() + Dispatchers.Default + coroutineErrorHandler
            )
            val storageDispatcher = Dispatchers.IO.limitedParallelism(1)
            return ThreadingExecutor(
                coroutineScope = coroutineScope,
                storageWriteScope = CoroutineScope(
                    coroutineScope.coroutineContext +
                            SupervisorJob(coroutineScope.coroutineContext[Job]) +
                            storageDispatcher +
                            storageErrorHandler
                ),
                storageDispatcher = storageDispatcher,
            )
        }
    }
}
