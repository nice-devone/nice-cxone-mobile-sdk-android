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

package com.nice.cxonechat

import com.nice.cxonechat.Cancellable.Companion.asCancellable
import com.nice.cxonechat.Cancellable.Companion.cancel
import com.nice.cxonechat.Cancellable.Companion.noop
import kotlinx.coroutines.Job
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class CancellableTest {

    @Test
    fun noop_doesNotThrow() {
        noop.cancel()
    }

    @Test
    fun jobAsCancellable_cancelsJob() = runTest {
        val job = Job()
        val cancellable = job.asCancellable()

        assertTrue(job.isActive)
        cancellable.cancel()
        assertFalse(job.isActive)
    }

    @Test
    fun compositeCancellable_cancelsAll() {
        var cancelled1 = false
        var cancelled2 = false
        val c1 = Cancellable { cancelled1 = true }
        val c2 = Cancellable { cancelled2 = true }

        val composite = Cancellable(c1, c2)
        composite.cancel()

        assertTrue(cancelled1)
        assertTrue(cancelled2)
    }

    @Test
    fun compositeCancellable_ignoresNull() {
        var cancelled = false
        val c1 = Cancellable { cancelled = true }

        val composite = Cancellable(c1, null)
        composite.cancel()

        assertTrue(cancelled)
    }

    @Test
    fun iterableCancel_cancelsAll() {
        var cancelled1 = false
        var cancelled2 = false
        val list: List<Cancellable?> = listOf(
            Cancellable { cancelled1 = true },
            null,
            Cancellable { cancelled2 = true },
        )

        list.cancel()

        assertTrue(cancelled1)
        assertTrue(cancelled2)
    }
}
