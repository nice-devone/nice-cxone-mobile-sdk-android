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

package com.nice.cxonechat.ui.composable.screenshot

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import com.nice.cxonechat.message.TimeSlot
import com.nice.cxonechat.ui.composable.conversation.TimePickerBottomSheet
import com.nice.cxonechat.ui.composable.conversation.model.Message.TimePicker
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import com.nice.cxonechat.ui.util.preview.message.TimePickerSlot
import com.nice.cxonechat.ui.util.preview.message.UiSdkTimePicker
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.time.Instant

/**
 * Shows how much of the window a sheet takes, using the time picker sheet as the stand-in for all of
 * them that keep [com.nice.cxonechat.ui.composable.theme.wideScreenSheetMaxWidth]'s default rule: it is
 * the one whose content needs no image loader or chat service to compose, and whose own design - unlike
 * end conversation's - has no Figma frame drawing it as a centred card in landscape, so it is still
 * exercising the wrapper's default rather than an override.
 *
 * The capture includes the scrim and the space either side of the sheet, which is where the width rule is
 * visible - a sheet photographed on its own tells you nothing about how much of the window it left over.
 *
 * Only windows this sheet fills the width of are captured. Under Robolectric a sheet that is both narrower
 * than its window and offset down it - a short sheet against the
 * [com.nice.cxonechat.ui.composable.theme.wideScreenSheetMaxWidth] cap - is drawn a
 * second time at its un-offset position, so a golden of that combination records a rendering artefact
 * rather than the layout. The capped case is asserted numerically in
 * [com.nice.cxonechat.ui.composable.theme.WideScreenModalBottomSheetTest] and captured, on a sheet tall
 * enough to avoid the artefact, by [EditCustomValuesScreenshotTest].
 */
@RunWith(RobolectricTestRunner::class)
@Config(
    sdk = [33],
    qualifiers = "w852dp-h412dp-normal-notlong-notround-notnight-xhdpi-finger"
)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class BottomSheetWidthScreenshotTest {

    /** The header holds a formatted date, so an unpinned zone has to say so rather than diff. */
    @get:Rule
    internal val pinnedEnvironmentRule = PinnedEnvironmentRule()

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `bottom sheet in a landscape window`() {
        captureSheet()
    }

    @Test
    @Config(qualifiers = "w411dp-h891dp-normal-notlong-notround-notnight-xhdpi-finger")
    fun `bottom sheet in a portrait window`() {
        captureSheet()
    }

    private fun captureSheet() {
        composeTestRule.setContent {
            ChatTheme {
                // The sheet is drawn over this window rather than inside it, so the host needs a themed
                // surface of its own: without one the scrim either side of the sheet falls on an unpainted
                // window instead of on the chat, and a dark capture would show a light gap.
                Surface(modifier = Modifier.fillMaxSize()) {
                    TimePickerBottomSheet(
                        message = TimePicker(message = UiSdkTimePicker(timeSlots = FIXED_TIME_SLOTS)),
                        onDismiss = {},
                        onDone = { _, _ -> },
                    )
                }
            }
        }
        // The sheet owns its animation, so it is absent from the first frames. Its width does not change
        // while it slides, so any settled frame will do.
        composeTestRule.waitUntil {
            composeTestRule.onAllNodesWithTag(SHEET_TAG).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onRoot().captureRoboImage(roborazziOptions = screenshotRoborazziOptions)
    }

    private companion object {
        const val SHEET_TAG = "time_picker_bottom_sheet"

        /** Fixed rather than the default clock-derived slots, so the golden's date text is deterministic. */
        val FIXED_TIME_SLOTS: List<TimeSlot> = listOf(
            TimePickerSlot(id = "slot-1", startTime = Instant.parse("2026-07-29T10:00:00Z")),
            TimePickerSlot(id = "slot-2", startTime = Instant.parse("2026-07-29T10:30:00Z")),
        )
    }
}
