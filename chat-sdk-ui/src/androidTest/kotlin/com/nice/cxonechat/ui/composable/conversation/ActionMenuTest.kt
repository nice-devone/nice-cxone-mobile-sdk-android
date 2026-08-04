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

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.nice.cxonechat.thread.ChatThreadState
import com.nice.cxonechat.ui.AbstractComponentActivityUiTest
import com.nice.cxonechat.ui.R.string
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ActionMenuTest : AbstractComponentActivityUiTest() {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun showArchivedThreadMenu_isDisplayedCorrectly_andAccessible() {
        composeTestRule.setContent {
            ChatTheme {
                ShowArchivedThreadMenu(displayEndConversation = {})
            }
        }

        val expectedText = context.getString(string.livechat_conversation_options)

        // Verify text display and accessibility
        composeTestRule
            .onNodeWithText(expectedText)
            .assertIsDisplayed()

        // Verify test tag and clickability
        composeTestRule
            .onNodeWithTag("show_archived_thread_menu_item")
            .assertIsDisplayed()
            .assertHasClickAction()
    }

    @Test
    fun showArchivedThreadMenu_triggersCallback_whenClicked() {
        var wasClicked = false
        composeTestRule.setContent {
            ChatTheme {
                ShowArchivedThreadMenu(displayEndConversation = { wasClicked = true })
            }
        }

        composeTestRule
            .onNodeWithTag("show_archived_thread_menu_item")
            .performClick()

        assertTrue(wasClicked)
    }

    @Test
    fun editThreadValuesMenu_isDisplayedCorrectly_andAccessible() {
        composeTestRule.setContent {
            ChatTheme {
                EditThreadValuesMenu(onEditThreadValues = {})
            }
        }

        val expectedText = context.getString(string.change_details_label)

        // Verify text display and accessibility
        composeTestRule
            .onNodeWithText(expectedText)
            .assertIsDisplayed()

        // Verify test tag and clickability
        composeTestRule
            .onNodeWithTag("edit_thread_custom_values_menu_item")
            .assertIsDisplayed()
            .assertHasClickAction()
    }

    @Test
    fun editThreadValuesMenu_triggersCallback_whenClicked() {
        var wasClicked = false
        composeTestRule.setContent {
            ChatTheme {
                EditThreadValuesMenu(onEditThreadValues = { wasClicked = true })
            }
        }

        composeTestRule
            .onNodeWithTag("edit_thread_custom_values_menu_item")
            .performClick()

        assertTrue(wasClicked)
    }

    @Test
    fun sendTranscriptMenu_isDisplayedCorrectly_andAccessible() {
        composeTestRule.setContent {
            ChatTheme {
                SendTranscriptMenu(onSendTranscript = {})
            }
        }

        val expectedText = context.getString(string.send_transcript)

        // Verify text display and accessibility
        composeTestRule
            .onNodeWithText(expectedText)
            .assertIsDisplayed()

        // Verify test tag and clickability
        composeTestRule
            .onNodeWithTag("send_transcript_menu_item")
            .assertIsDisplayed()
            .assertHasClickAction()
    }

    @Test
    fun sendTranscriptMenu_triggersCallback_whenClicked() {
        var wasClicked = false
        composeTestRule.setContent {
            ChatTheme {
                SendTranscriptMenu(onSendTranscript = { wasClicked = true })
            }
        }

        composeTestRule
            .onNodeWithTag("send_transcript_menu_item")
            .performClick()

        assertTrue(wasClicked)
    }

    @Test
    fun changeThreadNameMenu_isDisplayedCorrectly_andAccessible() {
        composeTestRule.setContent {
            ChatTheme {
                ChangeThreadNameMenu(onEditThreadName = {})
            }
        }

        val expectedText = context.getString(string.change_thread_name)

        // Verify text display and accessibility
        composeTestRule
            .onNodeWithText(expectedText)
            .assertIsDisplayed()

        // Verify test tag and clickability
        composeTestRule
            .onNodeWithTag("change_thread_name_menu_item")
            .assertIsDisplayed()
            .assertHasClickAction()
    }

    @Test
    fun changeThreadNameMenu_triggersCallback_whenClicked() {
        var wasClicked = false
        composeTestRule.setContent {
            ChatTheme {
                ChangeThreadNameMenu(onEditThreadName = { wasClicked = true })
            }
        }

        composeTestRule
            .onNodeWithTag("change_thread_name_menu_item")
            .performClick()

        assertTrue(wasClicked)
    }

    @Test
    fun endConversationMenu_isDisplayedCorrectly_andAccessible_whenReady() {
        val threadState = mutableStateOf(ChatThreadState.Ready)
        composeTestRule.setContent {
            ChatTheme {
                EndConversationMenu(
                    threadState = threadState,
                    onClick = {}
                )
            }
        }

        val expectedText = context.getString(string.action_end_conversation)

        // Verify text display and accessibility
        composeTestRule
            .onNodeWithText(expectedText)
            .assertIsDisplayed()

        // Verify test tag, clickability, and enabled state
        composeTestRule
            .onNodeWithTag("end_conversation_menu_item")
            .assertIsDisplayed()
            .assertHasClickAction()
            .assertIsEnabled()
    }

    @Test
    fun endConversationMenu_triggersCallback_whenClicked() {
        var wasClicked = false
        val threadState = mutableStateOf(ChatThreadState.Ready)
        composeTestRule.setContent {
            ChatTheme {
                EndConversationMenu(
                    threadState = threadState,
                    onClick = { wasClicked = true }
                )
            }
        }

        composeTestRule
            .onNodeWithTag("end_conversation_menu_item")
            .performClick()

        assertTrue(wasClicked)
    }

    @Test
    fun endConversationMenu_isDisabled_whenThreadStateIsNotReady() {
        // Test with Pending state
        val threadState = mutableStateOf(ChatThreadState.Pending)
        composeTestRule.setContent {
            ChatTheme {
                EndConversationMenu(
                    threadState = threadState,
                    onClick = {}
                )
            }
        }

        composeTestRule
            .onNodeWithTag("end_conversation_menu_item")
            .assertIsNotEnabled()

        // Change to Closed state and verify still disabled
        threadState.value = ChatThreadState.Closed

        composeTestRule
            .onNodeWithTag("end_conversation_menu_item")
            .assertIsNotEnabled()
    }

    @Test
    fun endConversationMenu_enabledStateChanges_whenThreadStateChanges() {
        val threadState = mutableStateOf(ChatThreadState.Pending)
        composeTestRule.setContent {
            ChatTheme {
                EndConversationMenu(
                    threadState = threadState,
                    onClick = {}
                )
            }
        }

        // Initially disabled
        composeTestRule
            .onNodeWithTag("end_conversation_menu_item")
            .assertIsNotEnabled()

        // Change to ready state
        threadState.value = ChatThreadState.Ready

        // Should now be enabled
        composeTestRule
            .onNodeWithTag("end_conversation_menu_item")
            .assertIsEnabled()
    }
}
