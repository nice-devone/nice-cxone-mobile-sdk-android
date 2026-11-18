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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.width
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Pins how wide a sheet built from [WideScreenModalBottomSheet] gets to be, which is the one thing the
 * wrapper exists to decide and the one thing a golden of sheet content cannot show.
 *
 * Measured from the content rather than the sheet, because the content fills the sheet and its node is
 * the one with stable bounds; window insets are dropped so that the sheet's own width is all that is
 * left to measure.
 */
@RunWith(RobolectricTestRunner::class)
@Config(
    sdk = [33],
    qualifiers = "w852dp-h412dp-normal-notlong-notround-notnight-xhdpi-finger"
)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
internal class WideScreenModalBottomSheetTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `a landscape window gets a sheet as wide as the window`() {
        showSheet()

        assertEquals(852f, sheetWidth().value, TOLERANCE)
    }

    /**
     * A tablet-sized landscape window, where the cap would waste twice as much of the window as it does on
     * a landscape phone - so this is the size at which getting the rule wrong is most visible.
     */
    @Test
    @Config(qualifiers = "w1280dp-h800dp-normal-notlong-notround-notnight-xhdpi-finger")
    fun `an expanded landscape window gets a sheet as wide as the window`() {
        showSheet()

        assertEquals(1280f, sheetWidth().value, TOLERANCE)
    }

    /** A tablet-sized window held upright: wider than the Material cap, so the cap is what is measured. */
    @Test
    @Config(qualifiers = "w720dp-h1280dp-normal-notlong-notround-notnight-xhdpi-finger")
    fun `a tall window below the breakpoint keeps the material cap`() {
        showSheet()

        assertEquals(MATERIAL_SHEET_CAP.value, sheetWidth().value, TOLERANCE)
    }

    /**
     * The case that separates the two wide-screen predicates, and so the one that fails if the width rule
     * is ever widened back to `isWideScreen()`: at exactly the expanded breakpoint this window counts as
     * wide, yet it is still taller than it is wide, so it has the height to show a centred sheet as
     * designed and keeps the cap.
     */
    @Test
    @Config(qualifiers = "w840dp-h1280dp-normal-notlong-notround-notnight-xhdpi-finger")
    fun `a tall window at the breakpoint keeps the material cap`() {
        showSheet()

        assertEquals(MATERIAL_SHEET_CAP.value, sheetWidth().value, TOLERANCE)
    }

    /**
     * The opt-out the attachment picker takes, measured in the same window as the first case: the two
     * together pin the parameter rather than the window, since that window's default spans all 852dp.
     */
    @Test
    fun `a landscape window keeps the material cap when the sheet asks for it`() {
        showSheet(sheetMaxWidth = MATERIAL_SHEET_CAP)

        assertEquals(MATERIAL_SHEET_CAP.value, sheetWidth().value, TOLERANCE)
    }

    /** @param sheetMaxWidth Width to ask for, or `null` to take the wrapper's own rule. */
    @OptIn(ExperimentalMaterial3Api::class)
    private fun showSheet(sheetMaxWidth: Dp? = null) {
        composeTestRule.setContent {
            ChatTheme {
                ChatTheme.WideScreenModalBottomSheet(
                    onDismissRequest = {},
                    sheetMaxWidth = sheetMaxWidth ?: wideScreenSheetMaxWidth(),
                    contentWindowInsets = { WindowInsets() },
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(CONTENT_HEIGHT)
                            .testTag(CONTENT_TAG)
                    )
                }
            }
        }
        // The sheet lives in its own window and animates itself in, so its content is not present for
        // the first frames. Width does not change as it slides, so any settled frame will do.
        composeTestRule.waitUntil {
            composeTestRule.onAllNodesWithTag(CONTENT_TAG).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun sheetWidth(): Dp =
        composeTestRule.onNodeWithTag(CONTENT_TAG).getUnclippedBoundsInRoot().width

    private companion object {
        const val CONTENT_TAG = "sheet_content_under_test"
        const val TOLERANCE = 1f
        val CONTENT_HEIGHT = 200.dp

        /** [androidx.compose.material3.BottomSheetDefaults.SheetMaxWidth], restated so a change to it fails here. */
        val MATERIAL_SHEET_CAP = 640.dp
    }
}
