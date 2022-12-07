package com.nice.cxonechat.log

import org.junit.Test
import kotlin.test.assertEquals

internal class LevelTest {

    @Test
    fun severe_hasLevel1000() {
        val level = Level.Severe
        assertEquals(1000, level.intValue)
    }

    @Test
    fun warning_hasLevel900() {
        val level = Level.Warning
        assertEquals(900, level.intValue)
    }

    @Test
    fun info_hasLevel800() {
        val level = Level.Info
        assertEquals(800, level.intValue)
    }

    @Test
    fun config_hasLevel700() {
        val level = Level.Config
        assertEquals(700, level.intValue)
    }

    @Test
    fun fine_hasLevel500() {
        val level = Level.Fine
        assertEquals(500, level.intValue)
    }

    @Test
    fun finer_hasLevel400() {
        val level = Level.Finer
        assertEquals(400, level.intValue)
    }

    @Test
    fun finest_hasLevel300() {
        val level = Level.Finest
        assertEquals(300, level.intValue)
    }

    @Test
    fun all_hasLevelMinValue() {
        val level = Level.All
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

}
