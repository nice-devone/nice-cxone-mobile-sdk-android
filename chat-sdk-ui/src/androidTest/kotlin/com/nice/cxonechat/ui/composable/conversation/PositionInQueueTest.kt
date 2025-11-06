/*
 * Copyright (c) 2021-2025. NICE Ltd. All rights reserved.
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

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test

class PositionInQueueTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun positionInQueue_displaysCorrectTitleForPosition() {
        val testPosition = 2
        composeTestRule.setContent {
            PositionInQueue(position = testPosition)
        }
        composeTestRule.onNodeWithTag("position_in_queue_content_view").assertIsDisplayed()
        composeTestRule.onNodeWithText("You are number $testPosition in line.").assertIsDisplayed()
        composeTestRule.onNodeWithTag("loading_animation_view",useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun positionInQueue_displaysNextTitleForPositionOne() {
        composeTestRule.setContent {
            PositionInQueue(position = 1)
        }
        // Replace with the actual string from resources if needed
        composeTestRule.onNodeWithText("You are next in line.").assertIsDisplayed()
    }
}
