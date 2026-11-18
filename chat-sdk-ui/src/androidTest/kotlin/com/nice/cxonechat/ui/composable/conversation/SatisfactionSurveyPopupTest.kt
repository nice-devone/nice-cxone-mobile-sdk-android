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

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.nice.cxonechat.ui.AbstractComponentActivityUiTest
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicBoolean

@RunWith(AndroidJUnit4::class)
@LargeTest
class SatisfactionSurveyPopupTest : AbstractComponentActivityUiTest() {

    @Test
    fun cancelButtonCallsOnDismiss() {
        val dismissCalled = AtomicBoolean(false)
        composeTestRule.setContent {
            ChatTheme {
                SatisfactionSurveyPopup(createPreviewSurveyPopup(), onDismiss = { dismissCalled.set(true) })
            }
        }
        composeTestRule.onNodeWithTag("satisfaction_survey_cancel_button").performClick()
        assertTrue(dismissCalled.get())
    }

    /**
     * Test that the popup and its Cancel control have proper accessibility support.
     */
    @Test
    fun satisfactionSurveyPopup_hasAccessibility() {
        composeTestRule.setContent {
            ChatTheme {
                SatisfactionSurveyPopup(createPreviewSurveyPopup(), onDismiss = {})
            }
        }
        composeTestRule.onNodeWithTag("satisfaction_survey_popup")
            .assertIsDisplayed()
            .tryPerformAccessibilityChecks()

        composeTestRule.onNodeWithTag("satisfaction_survey_cancel_button")
            .assertIsDisplayed()
            .tryPerformAccessibilityChecks()
    }
}
