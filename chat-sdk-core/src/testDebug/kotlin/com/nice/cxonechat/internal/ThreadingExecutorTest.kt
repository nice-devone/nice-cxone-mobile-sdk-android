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

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertSame

internal class ThreadingExecutorTest {

    @Test
    fun `exposes all injected dispatchers and scopes`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val storageScope = CoroutineScope(SupervisorJob())
        val executor = ThreadingExecutor(
            coroutineScope = this,
            storageWriteScope = storageScope,
            storageDispatcher = dispatcher,
            mainDispatcher = dispatcher,
            backgroundDispatcher = dispatcher,
            ioDispatcher = dispatcher,
        )
        assertSame(this, executor.coroutineScope)
        assertSame(storageScope, executor.storageWriteScope)
        assertSame(dispatcher, executor.storageDispatcher)
        assertSame(dispatcher, executor.mainDispatcher)
        assertSame(dispatcher, executor.backgroundDispatcher)
        assertSame(dispatcher, executor.ioDispatcher)
    }
}
