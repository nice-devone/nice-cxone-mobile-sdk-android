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

package com.nice.cxonechat.tool

import com.nice.cxonechat.internal.Threading
import com.nice.cxonechat.internal.ThreadingExecutor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher

@OptIn(ExperimentalStdlibApi::class, ExperimentalCoroutinesApi::class)
internal fun Threading.Companion.identity(testScope: TestScope): ThreadingExecutor {
    val dispatcher = UnconfinedTestDispatcher(testScope.testScheduler)
    // storageWriteScope uses a real dispatcher so that the drain test in ChatImplTest can
    // join the scope's Job via runBlocking without deadlocking the virtual-time scheduler.
    val storageDispatcher = Dispatchers.IO.limitedParallelism(1)
    return ThreadingExecutor(
        coroutineScope = testScope,
        storageWriteScope = CoroutineScope(
            testScope.coroutineContext +
                    SupervisorJob(testScope.coroutineContext[Job]) +
                    storageDispatcher
        ),
        storageDispatcher = storageDispatcher,
        mainDispatcher = dispatcher,
        backgroundDispatcher = dispatcher,
        ioDispatcher = dispatcher,
    )
}
