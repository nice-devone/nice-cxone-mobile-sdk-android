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
 * AN "AS IS" BASIS. NICE HEREBY DISCLAIMS ALL WARRANTIES AND CONDITIONS, EXPRESS
 * OR IMPLIED, INCLUDING (WITHOUT LIMITATION) WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE, NON-INFRINGEMENT, AND TITLE.
 */

package com.nice.cxonechat.utilities

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for [isEmpty] extension on [Iterable].
 *
 * Uses a plain [Iterable] adapter (not a [Collection]) so the compiler resolves
 * to our custom extension rather than the stdlib's [Collection.isEmpty].
 */
class IterableExtTest {

    /**
     * Wraps a [List] as a plain [Iterable] that does not implement [Collection],
     * ensuring [isEmpty] resolves to our extension function.
     */
    private fun <T> iterableOf(vararg elements: T): Iterable<T> = Iterable { elements.iterator() }

    @Test
    fun `isEmpty returns true for iterable with no elements`() {
        assertTrue(iterableOf<String>().isEmpty())
    }

    @Test
    fun `isEmpty returns false for iterable with a single element`() {
        assertFalse(iterableOf("item").isEmpty())
    }

    @Test
    fun `isEmpty returns false for iterable with multiple elements`() {
        assertFalse(iterableOf(1, 2, 3).isEmpty())
    }

    @Test
    fun `isEmpty creates the iterator only once`() {
        // Verify the check is based on firstOrNull and does not iterate multiple times
        var callCount = 0
        val iterable = Iterable<Int> {
            callCount++
            emptyList<Int>().iterator()
        }
        assertTrue(iterable.isEmpty())
        // iterator() should be called exactly once
        assertEquals(1, callCount)
    }
}
