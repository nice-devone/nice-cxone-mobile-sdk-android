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

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
import com.nice.cxonechat.message.TimeSlot
import com.nice.cxonechat.ui.composable.conversation.TimePickerBottomSheet
import com.nice.cxonechat.ui.composable.conversation.model.Message.TimePicker
import com.nice.cxonechat.ui.composable.conversation.uniqueSlotRowTag
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import com.nice.cxonechat.ui.util.preview.message.TimePickerSlot
import com.nice.cxonechat.ui.util.preview.message.UiSdkTimePicker
import com.nice.cxonechat.ui.util.toDateKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

private const val LANDSCAPE_QUALIFIERS = "w852dp-h412dp-normal-notlong-notround-notnight-xhdpi-finger"

/**
 * Goldens for the time picker sheet, built on the same [ChatTheme.ModalBottomSheet] the list picker
 * shares: its title, date groups and slot grid keep the design's 16dp portrait margin and widen to the
 * shared 64dp inset in a landscape window (DE-163607), mirroring the list picker sheet (DE-163605). The
 * slot grid also gains a scrollbar once its content overflows.
 */
@RunWith(RobolectricTestRunner::class)
@Config(
    sdk = [33],
    qualifiers = "w412dp-h852dp-normal-notlong-notround-notnight-xhdpi-finger"
)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class TimePickerBottomSheetScreenshotTest {

    /** These goldens hold formatted dates, so an unpinned zone has to say so rather than diff. */
    @get:Rule
    internal val pinnedEnvironmentRule = PinnedEnvironmentRule()

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `time picker bottom sheet portrait`() {
        setContent()
        composeTestRule.onRoot().captureRoboImage(roborazziOptions = screenshotRoborazziOptions)
    }

    @Test
    fun `time picker bottom sheet portrait dark`() {
        setContent(darkTheme = true)
        composeTestRule.onRoot().captureRoboImage(roborazziOptions = screenshotRoborazziOptions)
    }

    @Config(qualifiers = LANDSCAPE_QUALIFIERS)
    @Test
    fun `time picker bottom sheet landscape`() {
        setContent()
        composeTestRule.onRoot().captureRoboImage(roborazziOptions = screenshotRoborazziOptions)
    }

    @Config(qualifiers = LANDSCAPE_QUALIFIERS)
    @Test
    fun `time picker bottom sheet landscape dark`() {
        setContent(darkTheme = true)
        composeTestRule.onRoot().captureRoboImage(roborazziOptions = screenshotRoborazziOptions)
    }

    @Test
    fun `time picker bottom sheet portrait with overflowing slots shows scrollbar`() {
        setContent(timeSlots = MANY_TIME_SLOTS)
        composeTestRule.onRoot().captureRoboImage(roborazziOptions = screenshotRoborazziOptions)
    }

    @Config(qualifiers = LANDSCAPE_QUALIFIERS)
    @Test
    fun `time picker bottom sheet landscape with overflowing slots shows scrollbar`() {
        setContent(timeSlots = MANY_TIME_SLOTS)
        composeTestRule.onRoot().captureRoboImage(roborazziOptions = screenshotRoborazziOptions)
    }

    @Test
    fun `time picker bottom sheet portrait fills the width with three slots per row`() {
        setContent(timeSlots = MANY_TIME_SLOTS)

        assertFirstRowSpans(slotsPerRow = 3, start = 16.dp, end = 16.dp)
    }

    @Config(qualifiers = LANDSCAPE_QUALIFIERS)
    @Test
    fun `time picker bottom sheet landscape fills the width with six slots per row`() {
        setContent(timeSlots = MANY_TIME_SLOTS)

        // The end keeps the wide-screen 64dp plus the 12dp reserved for the scrollbar.
        assertFirstRowSpans(slotsPerRow = 6, start = 64.dp, end = 76.dp)
    }

    /**
     * Asserts that the first day's first [slotsPerRow] slots share one row, starting [start] and ending [end]
     * inside the sheet's edges, and that the slot after them sits lower. That slot only proves a wrap while it
     * belongs to the same day, so where [slotsPerRow] is a whole day it is the row end that pins the count.
     */
    private fun assertFirstRowSpans(slotsPerRow: Int, start: Dp, end: Dp) {
        val sheet = composeTestRule.onNodeWithTag("time_picker_bottom_sheet").fetchSemanticsNode().boundsInWindow
        val slots = MANY_TIME_SLOTS.take(slotsPerRow + 1).mapIndexed { index, slot ->
            val tag = uniqueSlotRowTag(slot = slot, dateKey = slot.startTime.toDateKey(), index = index % SLOTS_PER_DAY)
            // The tag sits on the slot's padded content; its parent is the slot's own surface.
            composeTestRule.onNodeWithTag(tag, useUnmergedTree = true).fetchSemanticsNode().parent!!.boundsInWindow
        }
        val firstRow = slots.take(slotsPerRow)
        val wrapped = slots.last()
        with(composeTestRule.density) {
            assertEquals("row start", start.toPx(), firstRow.first().left - sheet.left, 0.5f)
            assertEquals("row end", end.toPx(), sheet.right - firstRow.last().right, 0.5f)
        }
        firstRow.forEach { assertEquals("same row", firstRow.first().top, it.top, 0.5f) }
        assertTrue("next slot wraps", wrapped.top > firstRow.first().bottom)
    }

    private fun setContent(darkTheme: Boolean = false, timeSlots: List<TimeSlot> = FIXED_TIME_SLOTS) {
        composeTestRule.setContent {
            ChatTheme(darkTheme = darkTheme) {
                TimePickerBottomSheet(
                    message = TimePicker(message = UiSdkTimePicker(timeSlots = timeSlots)),
                    onDismiss = {},
                    onDone = { _, _ -> },
                )
            }
        }
    }

    private companion object {
        /** Two days, so the day grouping stays covered. The default would derive these from the clock. */
        val FIXED_TIME_SLOTS: List<TimeSlot> = listOf(
            TimePickerSlot(id = "slot-1", startTime = Instant.parse("2026-07-29T10:00:00Z")),
            TimePickerSlot(id = "slot-2", startTime = Instant.parse("2026-07-29T10:30:00Z")),
            TimePickerSlot(id = "slot-3", startTime = Instant.parse("2026-07-30T09:00:00Z")),
            TimePickerSlot(id = "slot-4", startTime = Instant.parse("2026-07-30T09:30:00Z")),
            TimePickerSlot(id = "slot-5", startTime = Instant.parse("2026-07-30T10:00:00Z")),
            TimePickerSlot(id = "slot-6", startTime = Instant.parse("2026-07-30T10:30:00Z")),
        )

        const val SLOTS_PER_DAY = 6

        /** Six days of slots, well past what a portrait window fits, to prove the grid scrolls. */
        val MANY_TIME_SLOTS: List<TimeSlot> = (0 until 6).flatMap { day ->
            (0 until SLOTS_PER_DAY).map { slot ->
                TimePickerSlot(
                    id = "slot-$day-$slot",
                    startTime = Instant.parse("2026-07-29T09:00:00Z") + (day * 24).hours + (slot * 30).minutes,
                )
            }
        }
    }
}
