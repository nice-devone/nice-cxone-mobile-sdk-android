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

import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.WindowSize
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.nice.cxonechat.ui.AbstractComponentActivityUiTest
import com.nice.cxonechat.ui.composable.theme.ChatTheme.space
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI tests for [OfflineScreen].
 * Verifies that the screen is displayed and user interactions are handled correctly.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class OfflineScreenTest : AbstractComponentActivityUiTest() {

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
     * Asserts that the offline image is hidden when the window container is shorter than the
     * `offlineImageMinimumScreenHeight` threshold.
     */
    @Test
    fun offlineScreen_offlineImage_isHidden_onSmallWindow() {
        composeTestRule.setContent {
            val sizeDp = space.offlineImageMinimumScreenHeight - 1.dp
            // Simulate window smaller than required minimum
            DeviceConfigurationOverride(
                DeviceConfigurationOverride.WindowSize(DpSize(sizeDp, sizeDp)),
            ) {
                OfflineScreen(onBackPress = {}, snackbarHostState = SnackbarHostState())
            }
        }
        // Assert that offline_image is not displayed
        composeTestRule.onNodeWithTag("offline_image").assertDoesNotExist()
    }

    /**
     * Asserts that the offline image is shown when the window container is taller than the
     * `offlineImageMinimumScreenHeight` threshold, without relying on the device's own geometry.
     */
    @Test
    fun offlineScreen_offlineImage_isShown_onLargeWindow() {
        composeTestRule.setContent {
            val sizeDp = space.offlineImageMinimumScreenHeight + 1.dp
            // Simulate window taller than required minimum
            DeviceConfigurationOverride(
                DeviceConfigurationOverride.WindowSize(DpSize(sizeDp, sizeDp)),
            ) {
                OfflineScreen(onBackPress = {}, snackbarHostState = SnackbarHostState())
            }
        }
        composeTestRule.onNodeWithTag("offline_image").assertIsDisplayed()
    }
}
