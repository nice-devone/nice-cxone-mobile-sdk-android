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

package com.nice.cxonechat.log

import com.nice.cxonechat.log.Level.All
import com.nice.cxonechat.log.Level.Debug
import com.nice.cxonechat.log.Level.Error
import com.nice.cxonechat.log.Level.Info
import com.nice.cxonechat.log.Level.Verbose
import com.nice.cxonechat.log.Level.Warning
import org.junit.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class LevelTest {

    @Test
    fun error_hasLevel1000() {
        val level = Error
        assertEquals(1000, level.intValue)
    }

    @Test
    fun warning_hasLevel900() {
        val level = Warning
        assertEquals(900, level.intValue)
    }

    @Test
    fun info_hasLevel800() {
        val level = Info
        assertEquals(800, level.intValue)
    }

    @Test
    fun debug_hasLevel400() {
        val level = Debug
        assertEquals(400, level.intValue)
    }

    @Test
    fun verbose_hasLevel300() {
        val level = Verbose
        assertEquals(300, level.intValue)
    }

    @Test
    fun all_hasLevelMinValue() {
        val level = All
        assertEquals(Int.MIN_VALUE, level.intValue)
    }

    @Test
    fun custom_hasLevelUnmodified() {
        val level = Level.Custom(154)
        assertEquals(154, level.intValue)
    }

    @Test
    fun compareTo_returnsValidInteger() {
        assert(Level.Custom(0) < Level.Custom(400)) {
            "compareTo returned invalid value for comparison. It's expected that 0 < 400"
        }
    }

    @Test
    fun verify_order() {
        val list = mutableListOf(
            All,
            Info,
            Debug,
            Verbose,
            Error,
            Warning,
        )
            .also(MutableList<Level>::sort)
            .toList()
        val expected = listOf(
            All,
            Verbose,
            Debug,
            Info,
            Warning,
            Error,
        )
        assertContentEquals(expected, list)
    }
}
