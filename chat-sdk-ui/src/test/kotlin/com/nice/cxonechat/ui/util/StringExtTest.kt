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

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class StringExtTest {

    @Test
    fun capitalizeFirstChar_basic() {
        // Tests basic capitalization scenarios
        assertEquals("Hello", "Hello".capitalizeFirstChar(Locale.ENGLISH))
        assertEquals("Hello world", "hello world".capitalizeFirstChar(Locale.ENGLISH))
    }

    @Test
    fun capitalizeFirstChar_edgeCases() {
        // Tests edge cases: empty, number, special char, whitespace, all uppercase
        assertEquals("", "".capitalizeFirstChar(Locale.ENGLISH))
        assertEquals("123abc", "123abc".capitalizeFirstChar(Locale.ENGLISH))
        assertEquals("!hello", "!hello".capitalizeFirstChar(Locale.ENGLISH))
        assertEquals(" hello", " hello".capitalizeFirstChar(Locale.ENGLISH))
        assertEquals("HELLO", "HELLO".capitalizeFirstChar(Locale.ENGLISH))
    }

    @Test
    fun capitalizeFirstChar_germanLocale() {
        val result = "über".capitalizeFirstChar(Locale.GERMAN)
        // The titlecase operation for 'ü' in German locale
        assertTrue(result[0].isUpperCase() || result.startsWith("Ü"))
    }
}
