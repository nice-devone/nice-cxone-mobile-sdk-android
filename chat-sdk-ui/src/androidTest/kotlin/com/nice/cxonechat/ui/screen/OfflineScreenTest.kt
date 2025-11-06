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

package com.nice.cxonechat.ui.screen

import android.graphics.Rect
import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.window.core.layout.WindowSizeClass
import androidx.window.testing.layout.TestWindowMetrics
import androidx.window.testing.layout.WindowMetricsCalculatorRule
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI tests for [OfflineScreen].
 * Verifies that the screen is displayed and user interactions are handled correctly.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class OfflineScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    /**
     * Rule to set window size for testing different layouts.
     */
    @get:Rule
    val windowMetricsCalculatorRule = WindowMetricsCalculatorRule()

    /**
     * Asserts that the OfflineScreen and its content are displayed.
     */
    @Test
    fun offlineScreen_isDisplayed() {
        composeTestRule.setContent {
            OfflineScreen(onBackPress = {}, snackbarHostState = SnackbarHostState())
        }
        // Assert the main offline view is displayed
        composeTestRule.onNodeWithTag("offline_view").assertIsDisplayed()
        // Assert the offline content view is displayed
        composeTestRule.onNodeWithTag("offline_content_view").assertIsDisplayed()
        composeTestRule.onNodeWithTag("offline_header_bar").assertIsDisplayed()
        composeTestRule.onNodeWithTag("offline_image").assertIsDisplayed()
        composeTestRule.onNodeWithTag("offline_close_button").assertIsDisplayed()
    }

    /**
     * Asserts that the close button is present and can be clicked.
     */
    @Test
    fun offlineScreen_closeButton_interaction() {
        var backPressed = false
        composeTestRule.setContent {
            OfflineScreen(onBackPress = { backPressed = true }, snackbarHostState = SnackbarHostState())
        }
        // Replace with expected button text from resources, e.g., "Close"
        composeTestRule.onNodeWithTag("offline_close_button").performClick()
        // Assert that the callback was triggered
        Assert.assertTrue(backPressed)
    }

    /**
     * Asserts that the back button triggers the onBackPress callback.
     */
    @Test
    fun offlineScreen_backButton_interaction() {
        var backPressed = false
        composeTestRule.setContent {
            OfflineScreen(onBackPress = { backPressed = true }, snackbarHostState = SnackbarHostState())
        }
        Espresso.pressBack()
        // Assert that the callback was triggered
        composeTestRule.runOnIdle {
            Assert.assertTrue(backPressed)
        }
    }

    /**
     * Asserts that the offline image is hidden when window size is smaller than medium size class.
     */
    @Test
    fun offlineScreen_offlineImage_isHidden_onSmallWindow() {
        // The image should be hidden for anything smaller than 530dp in height
        windowMetricsCalculatorRule.overrideCurrentWindowBounds(
            TestWindowMetrics(
                Rect(
                    0,
                    0,
                    WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND,
                    WindowSizeClass.HEIGHT_DP_MEDIUM_LOWER_BOUND
                )
            )
        )
        composeTestRule.setContent {
            OfflineScreen(onBackPress = {}, snackbarHostState = SnackbarHostState())
        }
        // Assert that offline_image is not displayed
        composeTestRule.onNodeWithTag("offline_image").assertDoesNotExist()
    }
}
