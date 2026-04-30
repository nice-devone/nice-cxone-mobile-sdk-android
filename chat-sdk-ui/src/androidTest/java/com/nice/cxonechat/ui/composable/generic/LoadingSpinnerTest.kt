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

package com.nice.cxonechat.ui.composable.generic

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LoadingSpinnerTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun loadingSpinner_showsProgressIndicatorWithLoadingTag_forGroupAttachment() {
        composeTestRule.setContent {
            ChatTheme {
                LoadingSpinner(
                    isGroupAttachment = true,
                    showLoadingBorder = true,
                )
            }
        }

        composeTestRule
            .onNodeWithTag("loading")
            .assertExists()
            .assertIsDisplayed()
            .tryPerformAccessibilityChecks()
    }

    @Test
    fun loadingSpinner_showsProgressIndicatorWithLoadingTag_forSingleAttachment() {
        composeTestRule.setContent {
            ChatTheme {
                LoadingSpinner(
                    isGroupAttachment = false,
                    showLoadingBorder = false,
                )
            }
        }

        composeTestRule
            .onNodeWithTag("loading")
            .assertExists()
            .assertIsDisplayed()
            .tryPerformAccessibilityChecks()
    }
}
