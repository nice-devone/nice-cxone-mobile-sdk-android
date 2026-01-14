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

package com.nice.cxonechat.ui.screen

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import org.junit.Rule
import org.junit.Test

class LoadingOverlayFullScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun loadingIndicatorIsDisplayedInitially() {
        composeTestRule.setContent {
            LoadingOverlayFullScreen(onClose = {})
        }
        composeTestRule
            .onNodeWithTag("preparing_dialog")
            .assertIsDisplayed()
    }

    @Test
    fun closeButtonAppearsAfterDelay() {
        composeTestRule.setContent {
            LoadingOverlayFullScreen(onClose = {})
        }

        composeTestRule.mainClock.advanceTimeBy(20_000)
        composeTestRule
            .onNodeWithTag("close_button")
            .assertIsDisplayed()
    }
}
