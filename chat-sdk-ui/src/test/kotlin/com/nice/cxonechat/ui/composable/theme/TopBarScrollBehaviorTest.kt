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

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.test.junit4.v2.createComposeRule
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Pins the scroll behavior the conversation's top app bar is given, in terms of what that behavior does
 * with a scroll delta rather than which factory produced it.
 *
 * The bar on a wide window is a single row with no collapsed state to scroll into, so a delta it took
 * would be a delta the conversation never moved by.
 */
@OptIn(ExperimentalMaterial3Api::class)
@RunWith(RobolectricTestRunner::class)
@Config(
    sdk = [33],
    qualifiers = "w411dp-h891dp-normal-notlong-notround-notnight-xhdpi-finger"
)
internal class TopBarScrollBehaviorTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `a compact window lets the top bar take the scroll it collapses by`() {
        val behavior = scrollBehavior()

        assertEquals(-SCROLL_DELTA, behavior.takeFromScrollUp(SCROLL_DELTA), TOLERANCE)
        assertEquals("the bar did not collapse", -SCROLL_DELTA, behavior.state.heightOffset, TOLERANCE)
    }

    @Test
    @Config(qualifiers = WIDE_QUALIFIERS)
    fun `a wide window leaves the whole scroll to the conversation`() {
        val behavior = scrollBehavior()

        assertEquals(0f, behavior.takeFromScrollUp(SCROLL_DELTA), TOLERANCE)
        assertEquals("the pinned bar moved", 0f, behavior.state.heightOffset, TOLERANCE)
    }

    private fun scrollBehavior(): TopAppBarScrollBehavior {
        lateinit var behavior: TopAppBarScrollBehavior
        composeTestRule.setContent {
            ChatTheme {
                behavior = rememberTopBarScrollBehavior()
            }
        }
        composeTestRule.waitForIdle()
        return behavior
    }

    /**
     * Offers [delta] of upward scroll and answers how much of it the bar took for itself.
     *
     * The height offset limit is the room a composed bar's layout would give it to collapse into. No bar
     * is composed here, so the test grants it: without any room even a collapsing behavior has nowhere
     * to go, and both cases would take nothing for the same reason.
     */
    private fun TopAppBarScrollBehavior.takeFromScrollUp(delta: Float): Float {
        state.heightOffsetLimit = -COLLAPSE_ROOM
        return nestedScrollConnection.onPreScroll(Offset(0f, -delta), NestedScrollSource.UserInput).y
    }

    private companion object {
        const val WIDE_QUALIFIERS = "w852dp-h412dp-normal-notlong-notround-notnight-xhdpi-finger"

        /** Ample next to the delta below, so the bar's room is never what decides the outcome. */
        const val COLLAPSE_ROOM = 200f
        const val SCROLL_DELTA = 50f
        const val TOLERANCE = 0.5f
    }
}
