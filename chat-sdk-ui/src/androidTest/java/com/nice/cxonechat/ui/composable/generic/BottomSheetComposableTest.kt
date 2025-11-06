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

package com.nice.cxonechat.ui.composable.generic

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class BottomSheetComposableTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun bottomSheetTitle_displaysTrailingContent_whenSupplied() {
        composeTestRule.setContent {
            ChatTheme {
                BottomSheetTitle(
                    message = "Test Title",
                    trailingContent = {
                        Box(Modifier.testTag("trailing_content")) { Text("TC") }
                    }
                )
            }
        }
        composeTestRule.onNodeWithTag("top_title").assertIsDisplayed()
        composeTestRule.onNodeWithTag("trailing_content", true).assertIsDisplayed()
    }

    @Test
    fun bottomSheetActionRow_displaysLeadingAndTrailingContent_whenSupplied() {
        composeTestRule.setContent {
            ChatTheme {
                BottomSheetActionRow(
                    text = "Action Text",
                    onClick = {},
                    testTag = "action_row",
                    leadingContent = {
                        Box(Modifier.testTag("leading_content")) { Text("LC") }
                    },
                    trailingContent = {
                        Box(Modifier.testTag("trailing_content")) { Text("TC") }
                    }
                )
            }
        }
        composeTestRule.onNodeWithTag("action_row").assertIsDisplayed()
        composeTestRule.onNodeWithTag("leading_content", true).assertIsDisplayed()
        composeTestRule.onNodeWithTag("trailing_content", true).assertIsDisplayed()
    }

    @Test
    fun bottomSheetActionRow_onClick_isCalled_whenClicked() {
        val wasClicked = mutableStateOf(false)
        composeTestRule.setContent {
            ChatTheme {
                BottomSheetActionRow(
                    text = "Clickable Action",
                    onClick = { wasClicked.value = true },
                    testTag = "action_row_clickable"
                )
            }
        }
        composeTestRule.onNodeWithTag("action_row_clickable").performClick()
        assertTrue(wasClicked.value)
    }
}
