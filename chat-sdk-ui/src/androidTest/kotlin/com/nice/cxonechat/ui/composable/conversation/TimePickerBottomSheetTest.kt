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

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasStateDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.nice.cxonechat.message.TimeSlot
import com.nice.cxonechat.ui.R
import com.nice.cxonechat.ui.composable.conversation.model.Message.TimePicker
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import com.nice.cxonechat.ui.util.preview.message.UiSdkTimePicker
import com.nice.cxonechat.ui.util.toDateKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class TimePickerBottomSheetTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun timePickerBottomSheet_displaysTitleSubtitleSlots_andHandlesActions() {
        val timePicker = TimePicker(UiSdkTimePicker())
        var dismissCalled = false
        var doneCalled = false
        var doneSlot: TimeSlot? = null
        var doneLocalizedText: String? = null

        composeTestRule.setContent {
            ChatTheme {
                TimePickerBottomSheetContent(
                    message = timePicker,
                    onDismiss = { dismissCalled = true },
                    onDone = { slot, localizedText ->
                        doneCalled = true
                        doneSlot = slot
                        doneLocalizedText = localizedText
                    }
                )
            }
        }

        val expectedSubtitle = composeTestRule.activity.getString(R.string.time_picker_dialogue_description)
        composeTestRule.onNodeWithTag("time_picker_header")
            .assertIsDisplayed()
            .assertTextContains(timePicker.popupTitle)
            .assertTextContains(expectedSubtitle)
        composeTestRule.onNodeWithText(timePicker.popupTitle).assertIsDisplayed()
        composeTestRule.onNodeWithText(expectedSubtitle).assertIsDisplayed()

        val firstSlot = timePicker.timeSlots.first()
        val firstSlotTag = uniqueSlotRowTag(
            slot = firstSlot,
            dateKey = firstSlot.startTime.toDateKey(),
            index = 0
        )
        composeTestRule.onNodeWithTag(firstSlotTag, useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.submit)).assertIsNotEnabled()

        selectableSlotNode(firstSlotTag).performClick()
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.submit)).assertIsEnabled().performClick()

        assertTrue(doneCalled)
        assertEquals(firstSlot, doneSlot)
        assertNotNull(doneLocalizedText)

        composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.cancel)).performClick()
        assertTrue(dismissCalled)
    }

    @Test
    fun timePickerBottomSheet_clickingSelectedSlotAgain_unselectsIt_andDisablesSubmit() {
        val timePicker = TimePicker(UiSdkTimePicker())

        composeTestRule.setContent {
            ChatTheme {
                TimePickerBottomSheetContent(
                    message = timePicker,
                    onDismiss = {},
                    onDone = { _, _ -> }
                )
            }
        }

        val firstSlot = timePicker.timeSlots.first()
        val firstSlotTag = uniqueSlotRowTag(
            slot = firstSlot,
            dateKey = firstSlot.startTime.toDateKey(),
            index = 0
        )
        val submitText = composeTestRule.activity.getString(R.string.submit)

        val slotNode = selectableSlotNode(firstSlotTag)

        slotNode.performClick()
        composeTestRule.onNodeWithText(submitText).assertIsEnabled()

        slotNode.performClick()
        composeTestRule.onNodeWithText(submitText).assertIsNotEnabled()
    }

    @Test
    fun timePickerBottomSheet_displaysDateHeadersForGroupedSlots() {
        val timePicker = TimePicker(UiSdkTimePicker())

        composeTestRule.setContent {
            ChatTheme {
                TimePickerBottomSheetContent(
                    message = timePicker,
                    onDismiss = {},
                    onDone = { _, _ -> }
                )
            }
        }

        val groupedByDateKeys = timePicker.timeSlots
            .map { it.startTime.toDateKey() }
            .distinct()
            .sorted()

        groupedByDateKeys.forEach { key ->
            composeTestRule.onNodeWithTag("time_picker_date_header_$key").assertIsDisplayed()
        }
    }

    @Test
    fun timePickerBottomSheet_accessibilityDragHandleExists_onBottomSheet() {
        val timePicker = TimePicker(UiSdkTimePicker())

        composeTestRule.setContent {
            ChatTheme {
                TimePickerBottomSheet(
                    message = timePicker,
                    onDismiss = {},
                    onDone = { _, _ -> }
                )
            }
        }

        composeTestRule.onNodeWithTag("time_picker_bottom_sheet").assertIsDisplayed()
    }

    @Test
    fun timePickerBottomSheet_accessibilityHeaderContentDescriptions_matchVisibleText() {
        val timePicker = TimePicker(UiSdkTimePicker())

        composeTestRule.setContent {
            ChatTheme {
                TimePickerBottomSheetContent(
                    message = timePicker,
                    onDismiss = {},
                    onDone = { _, _ -> }
                )
            }
        }

        val expectedSubtitle = composeTestRule.activity.getString(R.string.time_picker_dialogue_description)

        composeTestRule.onNodeWithTag("time_picker_header")
            .assertIsDisplayed()
            .assertTextContains(timePicker.popupTitle)
            .assertTextContains(expectedSubtitle)
        composeTestRule.onNodeWithText(timePicker.popupTitle).assertIsDisplayed()
        composeTestRule.onNodeWithText(expectedSubtitle).assertIsDisplayed()
    }

    private fun selectableSlotNode(slotTag: String) = composeTestRule.onNode(
        hasStateDescriptionMatchingSelection()
            .and(hasAnyDescendant(hasTestTag(slotTag))),
        useUnmergedTree = true
    )

    private fun hasStateDescriptionMatchingSelection(): SemanticsMatcher =
        hasStateDescription(composeTestRule.activity.getString(R.string.time_slot_state_selected))
            .or(hasStateDescription(composeTestRule.activity.getString(R.string.time_slot_state_not_selected)))
}
