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

package com.nice.cxonechat.ui.composable.theme

import androidx.compose.ui.test.junit4.v2.createComposeRule
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Pins which windows [isCompactHeightWindow] counts, across the form factors the conversation is drawn in.
 *
 * The landscape tablet is the case that separates height from orientation.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
internal class CompactHeightWindowTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    @Config(qualifiers = "w852dp-h412dp-normal-notlong-notround-notnight-xhdpi-finger")
    fun `a landscape phone window is compact height`() {
        assertTrue(isCompactHeight())
    }

    @Test
    @Config(qualifiers = "w411dp-h891dp-normal-notlong-notround-notnight-xhdpi-finger")
    fun `a portrait phone window is not compact height`() {
        assertFalse(isCompactHeight())
    }

    @Test
    @Config(qualifiers = "w1280dp-h800dp-normal-notlong-notround-notnight-xhdpi-finger")
    fun `a landscape tablet window is not compact height`() {
        assertFalse(isCompactHeight())
    }

    @Test
    @Config(qualifiers = "w800dp-h1280dp-normal-notlong-notround-notnight-xhdpi-finger")
    fun `a portrait tablet window is not compact height`() {
        assertFalse(isCompactHeight())
    }

    private fun isCompactHeight(): Boolean {
        var compactHeight: Boolean? = null
        composeTestRule.setContent {
            ChatTheme {
                compactHeight = isCompactHeightWindow()
            }
        }
        composeTestRule.waitForIdle()
        return requireNotNull(compactHeight)
    }
}
