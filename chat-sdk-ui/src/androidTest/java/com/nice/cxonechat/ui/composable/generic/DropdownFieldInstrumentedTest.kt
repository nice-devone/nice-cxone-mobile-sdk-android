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

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nice.cxonechat.ui.R
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import com.nice.cxonechat.ui.composable.theme.ChatTheme.chatColors
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DropdownFieldInstrumentedTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val options = sequenceOf(
        DropdownItem("aa", "1"),
        DropdownItem("ab", "2"),
        DropdownItem("ba", "3")
    )

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun dropdown_menu_appears_when_clicked() {
        var value by mutableStateOf<String?>(null)
        composeRule.setContent {
            ChatTheme {
                Column(
                    Modifier
                        .systemBarsPadding()
                        .background(chatColors.token.background.default)
                        .fillMaxSize()
                ) {
                    DropdownField(
                        modifier = Modifier.testTag("dropdown_box"),
                        value = value,
                        options = options,
                        onSelect = { value = it }
                    )
                }
            }
        }

        // Click the box which should toggle the menu
        composeRule.onNodeWithTag("dropdown_textfield").performClick()
        composeRule.waitForIdle()

        // Verify that menu items are displayed
        composeRule.onNodeWithTag("dropdown_item_aa").assertIsDisplayed()
        composeRule.onNodeWithText("ab").assertIsDisplayed()
        composeRule.onNodeWithText("ba").assertIsDisplayed()
    }

    @Test
    fun typing_accepts_only_valid_characters_and_filters_results() {
        var value by mutableStateOf<String?>(null)
        composeRule.setContent {
            ChatTheme {
                DropdownField(
                    modifier = Modifier.testTag("dropdown_box"),
                    value = value,
                    options = options,
                    onSelect = { value = it }
                )
            }
        }

        // Click the box to open the menu and focus text field
        composeRule.onNodeWithTag("dropdown_textfield").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("dropdown_item_aa").assertIsDisplayed()
        val field = composeRule.onNodeWithTag("dropdown_textfield")

        // Type 'a' -> should show aa and ab
        field.performTextInput("a")
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("dropdown_item_aa").assertIsDisplayed()
        composeRule.onNodeWithText("ab").assertIsDisplayed()
        // 'ba' should not be visible when filter starts with 'a'
        composeRule.onNodeWithText("ba").assertDoesNotExist()

        // Type another 'a' -> should filter down to only 'aa'
        field.performTextInput("a")
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("dropdown_item_aa").assertIsDisplayed()
        composeRule.onNodeWithText("ab").assertDoesNotExist()

        // Try typing invalid character 'z' - it should be ignored/discarded
        field.performTextInput("z")
        composeRule.waitForIdle()
        // Still only 'aa' should be displayed
        composeRule.onNodeWithTag("dropdown_item_aa").assertIsDisplayed()
    }

    @Test
    fun clicking_trailing_icon_clears_filter() {
        var value by mutableStateOf<String?>(null)
        composeRule.setContent {
            ChatTheme {
                DropdownField(
                    modifier = Modifier.testTag("dropdown_box"),
                    value = value,
                    options = options,
                    onSelect = { value = it }
                )
            }
        }

        // Open the menu and focus text field
        composeRule.onNodeWithTag("dropdown_textfield").performClick()
        composeRule.waitForIdle()

        val field = composeRule.onNodeWithTag("dropdown_textfield")
        // Type 'a' to show trailing clear icon
        field.performTextInput("a")
        composeRule.waitForIdle()
        composeRule.onNodeWithText("aa").assertIsDisplayed()

        // Click the trailing icon (content description from resources)
        val cancelDesc = composeRule.activity.getString(R.string.cancel)
        composeRule.onNodeWithContentDescription(cancelDesc).performClick()
        composeRule.waitForIdle()

        // After clearing, all items should be present
        composeRule.onNodeWithText("aa").assertIsDisplayed()
        composeRule.onNodeWithText("ab").assertIsDisplayed()
        composeRule.onNodeWithText("ba").assertIsDisplayed()
    }

    @Test
    fun clicking_dropdown_option_calls_callback() {
        var selectedValue: String? = null
        composeRule.setContent {
            ChatTheme {
                DropdownField(
                    modifier = Modifier.testTag("dropdown_box"),
                    value = null,
                    options = options,
                    onSelect = { selectedValue = it }
                )
            }
        }

        // Open dropdown
        composeRule.onNodeWithTag("dropdown_textfield").performClick()
        composeRule.waitForIdle()

        // Click on "ab"
        composeRule.onNodeWithTag("dropdown_item_ab").performClick()
        composeRule.waitForIdle()

        // Verify callback was called with "2"
        assertEquals("2", selectedValue)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun pressing_enter_with_single_option_selects_and_reports() {
        var selectedValue: String? = null
        composeRule.setContent {
            ChatTheme {
                DropdownField(
                    modifier = Modifier.testTag("dropdown_box"),
                    value = null,
                    options = options,
                    onSelect = { selectedValue = it }
                )
            }
        }

        // Open dropdown
        composeRule.onNodeWithTag("dropdown_textfield").performClick()
        composeRule.waitForIdle()

        // Type "ba" to filter to single option
        composeRule.onNodeWithTag("dropdown_textfield").performTextInput("ba")
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("dropdown_item_ba").assertIsDisplayed()

        // Simulate pressing Enter key
        composeRule.onNodeWithTag("dropdown_textfield").performKeyInput {
            keyDown(Key.Enter)
            keyUp(Key.Enter)
        }
        composeRule.waitForIdle()

        // Verify callback was called with "3"
        assertEquals("3", selectedValue)
    }
}
