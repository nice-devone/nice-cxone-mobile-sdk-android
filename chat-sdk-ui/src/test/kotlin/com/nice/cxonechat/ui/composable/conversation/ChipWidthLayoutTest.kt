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

package com.nice.cxonechat.ui.composable.conversation

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.width
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import com.nice.cxonechat.ui.composable.theme.Space
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Pins the width a [Chip] is given depending on whether its text fits [Space.maxButtonWidth] on one
 * line, per the Figma design (node 63819-71662): short text keeps a compact, hug-content chip, while
 * text too long for that width fills the row instead of wrapping inside a narrow chip.
 *
 * A [Row] *wider* than [Space.maxButtonWidth] stands in for the row a chip is packed into, so a filled
 * chip's measured width (matching the row) can only be explained by the fill: a chip still capped at
 * [Space.maxButtonWidth] would measure narrower than the row instead, catching a regression to the old
 * behavior.
 */
@RunWith(RobolectricTestRunner::class)
@Config(
    sdk = [33],
    qualifiers = "w411dp-h891dp-normal-notlong-notround-notnight-xhdpi-finger"
)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
internal class ChipWidthLayoutTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `short text keeps a compact chip narrower than the row`() {
        showChip(SHORT_TEXT)

        assertTrue(
            "a short chip (${chipBounds().width}) should hug its text, not fill the row ($ROW_WIDTH)",
            chipBounds().width < ROW_WIDTH,
        )
    }

    @Test
    fun `text too long for one line fills the row instead of wrapping inside a narrow chip`() {
        showChip(LONG_TEXT)

        assertEquals(
            "a chip whose text does not fit Space.maxButtonWidth on one line should fill the row",
            ROW_WIDTH.value,
            chipBounds().width.value,
            TOLERANCE,
        )
    }

    private fun showChip(text: String) {
        composeTestRule.setContent {
            ChatTheme {
                Row(Modifier.width(ROW_WIDTH)) {
                    Chip(
                        text = text,
                        modifier = Modifier.testTag("chip"),
                        onSelected = {},
                    )
                }
            }
        }
        composeTestRule.waitForIdle()
    }

    private fun chipBounds(): DpRect =
        composeTestRule.onNodeWithTag("chip").getUnclippedBoundsInRoot()

    private companion object {
        const val SHORT_TEXT = "Yes"

        /** Comfortably longer, on one line, than Space.maxButtonWidth (320dp) can hold at the chip's font size. */
        const val LONG_TEXT = "This is a very long option text that should not fit on a single normal chip width at all"

        /**
         * Wider than Space.maxButtonWidth (320dp): a chip still capped at that width would measure 320dp
         * here, not this row's width, so filling the row is unambiguous evidence of the new behavior.
         */
        val ROW_WIDTH = 400.dp

        const val TOLERANCE = 0.5f
    }
}
