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

import android.view.View
import android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
import android.view.WindowManager.LayoutParams.SOFT_INPUT_MASK_ADJUST
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsCompat.Type
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.math.roundToInt

/**
 * Pins how [ChatTheme.ModalBottomSheet] makes room for the keyboard: where its Cancel/Submit footer sits, and
 * the soft input mode of the sheet's own window.
 *
 * The keyboard is faked by dispatching window insets to the sheet's own window, since Robolectric shows
 * no real one.
 */
@RunWith(RobolectricTestRunner::class)
@Config(
    sdk = [33],
    qualifiers = "w411dp-h891dp-normal-long-notround-notnight-xhdpi-finger"
)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
internal class ModalBottomSheetTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    /** The keyboard inset the sheet's content last read, so a test can tell a missed dispatch from a bug. */
    private var imeBottomSeen: Int = -1

    /** A parameter Material 3's sheet dialog reads, so flipping it recomposes that dialog. */
    private var dismissOnClickOutside by mutableStateOf(true)

    /**
     * The keyboard's inset is measured from the bottom of the window, so it already covers the navigation
     * bar: the footer has to sit on top of the keyboard, not a navigation bar's height above it.
     */
    @Test
    fun `the footer sits on top of the keyboard`() {
        val sheetView = showSheet()
        val gapWithoutInsets = footerGapToWindowBottom(sheetView)

        dispatchInsets(sheetView, navigationBarBottom = NAVIGATION_BAR_PX, imeBottom = IME_PX)

        composeTestRule.runOnIdle {
            assertEquals("Sheet content did not receive the keyboard inset", IME_PX, imeBottomSeen)
        }
        assertEquals(IME_PX, footerGapToWindowBottom(sheetView) - gapWithoutInsets)
    }

    @Test
    fun `the footer sits on top of the navigation bar without a keyboard`() {
        val sheetView = showSheet()
        val gapWithoutInsets = footerGapToWindowBottom(sheetView)

        dispatchInsets(sheetView, navigationBarBottom = NAVIGATION_BAR_PX, imeBottom = 0)

        assertEquals(NAVIGATION_BAR_PX, footerGapToWindowBottom(sheetView) - gapWithoutInsets)
    }

    /**
     * Material 3 sets its sheet window to ignore the keyboard, which leaves the keyboard out of the window's
     * visible frame; an exposed dropdown measures its room with that frame and opens under the keyboard.
     * Robolectric computes no visible frame for a keyboard, so this checks the mode rather than the menu.
     */
    @Test
    fun `the sheet window resizes for the keyboard`() {
        val sheetView = showSheet()

        assertEquals(SOFT_INPUT_ADJUST_RESIZE, softInputAdjust(sheetView))
    }

    /** Material 3 sets the mode again every time its sheet dialog recomposes. */
    @Test
    fun `the sheet window still resizes for the keyboard after the sheet dialog recomposes`() {
        val sheetView = showSheet()

        dismissOnClickOutside = false
        composeTestRule.waitForIdle()

        assertEquals(SOFT_INPUT_ADJUST_RESIZE, softInputAdjust(sheetView))
    }

    /** Shows a fully expanded sheet and returns the view hosting its content. */
    @OptIn(ExperimentalMaterial3Api::class)
    private fun showSheet(): View {
        var sheetView: View? = null
        var isExpanded: () -> Boolean = { false }
        composeTestRule.setContent {
            ChatTheme {
                val sheetState = rememberBottomSheetState(SheetValue.Hidden, SheetAnchors.FullHeightOnly)
                isExpanded = { sheetState.currentValue == SheetValue.Expanded && !sheetState.isAnimationRunning }
                ChatTheme.ModalBottomSheet(
                    onDismiss = {},
                    onSubmit = {},
                    canSubmit = true,
                    title = "Title",
                    dismissOnClickOutside = dismissOnClickOutside,
                    sheetState = sheetState,
                ) {
                    sheetView = LocalView.current
                    imeBottomSeen = WindowInsets.ime.getBottom(LocalDensity.current)
                    Text("Body")
                }
            }
        }
        composeTestRule.waitUntil { sheetView != null && isExpanded() }
        return checkNotNull(sheetView)
    }

    private fun dispatchInsets(view: View, navigationBarBottom: Int, imeBottom: Int) {
        composeTestRule.runOnIdle {
            val insets = WindowInsetsCompat.Builder()
                .setInsets(Type.navigationBars(), Insets.of(0, 0, 0, navigationBarBottom))
                .setInsets(Type.ime(), Insets.of(0, 0, 0, imeBottom))
                .setVisible(Type.ime(), imeBottom > 0)
                .build()
            ViewCompat.dispatchApplyWindowInsets(view.rootView, insets)
        }
        composeTestRule.waitForIdle()
    }

    /**
     * Pixels between the Submit button and the bottom of the sheet's window. The button's touch target
     * leaves a few pixels below it even with no insets, so tests compare against that baseline.
     */
    private fun footerGapToWindowBottom(view: View): Int {
        val footerBottom = composeTestRule.onNodeWithTag("submit_button").fetchSemanticsNode().boundsInWindow.bottom
        return composeTestRule.runOnIdle { view.rootView.height - footerBottom.roundToInt() }
    }

    private fun softInputAdjust(view: View): Int = composeTestRule.runOnIdle {
        checkNotNull(view.findDialogWindow()).attributes.softInputMode and SOFT_INPUT_MASK_ADJUST
    }

    private companion object {
        const val NAVIGATION_BAR_PX = 96
        const val IME_PX = 600
    }
}
