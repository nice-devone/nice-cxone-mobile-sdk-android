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

package com.nice.cxonechat.ui.composable

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import com.nice.cxonechat.ui.AbstractComponentActivityUiTest
import com.nice.cxonechat.ui.R
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import com.nice.cxonechat.ui.domain.model.Thread
import com.nice.cxonechat.ui.viewmodel.ChatThreadsViewModel.State
import org.junit.Test

/**
 * UI tests for MultiThreadContent composable with accessibility validation.
 */
class MultiThreadContentTest : AbstractComponentActivityUiTest() {

    /**
     * Creates a Thread with a specific name and active/archived status.
     */
    private fun createThread(name: String, canAddMoreMessages: Boolean = true): Thread {
        val chatThread = PreviewThread(threadName = name, canAddMoreMessages = canAddMoreMessages)
        return Thread(chatThread, name)
    }

    @Test
    fun chatThreadView_displaysThreadNameAndLastMessage() {
        val threadName = "Test Thread"
        val thread = createThread(threadName)

        setThreadContent(thread)

        composeTestRule.onNodeWithTag("conversation_name", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithTag("last_message_time", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithText(threadName).assertIsDisplayed()
    }

    @Test
    fun chatThreadView_passesAccessibilityChecks() {
        val threadName = "Customer Support"
        val thread = createThread(threadName)

        setThreadContent(thread)

        // Verify that the ChatThreadView passes accessibility checks
        composeTestRule.onNodeWithTag("chat_thread_view_${thread.id}")
            .assertIsDisplayed()
            .tryPerformAccessibilityChecks()
    }

    @Test
    fun multiThreadContent_displaysContentAndToggleWithAccessibilityChecks() {
        val threads = List(2) { PreviewThread.nextThread() }

        setMultiThreadContent(threads)

        composeTestRule.onNodeWithTag("multi_thread_content")
            .assertIsDisplayed()
            .tryPerformAccessibilityChecks()
        composeTestRule.onNodeWithTag("active_thread_toggle_view")
            .assertIsDisplayed()
            .tryPerformAccessibilityChecks()
    }

    @Test
    fun multiThreadContent_displaysThreadListWithAccessibilityChecks() {
        val threads = List(2) { PreviewThread.nextThread() }

        setMultiThreadContent(threads)

        composeTestRule.onNodeWithTag("chat_thread_list")
            .assertIsDisplayed()
            .tryPerformAccessibilityChecks()
    }

    @Test
    fun multiThreadContent_displaysArchivedThreadsWhenToggleSelected() {
        val threads = listOf(
            createThread("Active Thread", canAddMoreMessages = true),
            createThread("Archived Thread", canAddMoreMessages = false)
        )

        setMultiThreadContent(threads)

        // Verify initial state - active threads displayed
        composeTestRule.onNodeWithTag("chat_thread_list")
            .assertIsDisplayed()
            .tryPerformAccessibilityChecks()

        // Toggle to archived threads by clicking the archived segment (index 1)
        composeTestRule.onNodeWithTag("active_thread_toggle_button_1")
            .performClick()

        // Verify archived thread list is displayed
        composeTestRule.onNodeWithTag("archived_thread_list")
            .assertIsDisplayed()
            .tryPerformAccessibilityChecks()
    }

    @Test
    fun chatThreadView_unreadThread_passesAccessibilityChecks() {
        val threadName = "Unread Support Chat"
        val thread = createThread(threadName)

        setThreadContent(thread)

        // Verify unread thread view passes accessibility checks
        composeTestRule.onNodeWithTag("chat_thread_view_${thread.id}")
            .assertIsDisplayed()
            .tryPerformAccessibilityChecks()

        // Verify the unread indicator is displayed and accessible
        composeTestRule.onNodeWithTag("unread_indicator", useUnmergedTree = true)
            .assertIsDisplayed()
    }

    @Test
    fun noThreadView_isDisplayed_andPassesAccessibilityChecks_whenThreadListIsEmpty() {
        setMultiThreadContent(emptyList())

        composeTestRule.onNodeWithTag("no_threads_view")
            .assertIsDisplayed()
            .tryPerformAccessibilityChecks()
        composeTestRule.onNodeWithText(
            composeTestRule.activity.getString(R.string.label_no_threads)
        ).assertIsDisplayed()
    }

    /**
     * Sets up MultiThreadContent with default parameters for testing.
     */
    private fun setMultiThreadContent(threads: List<Thread>) {
        composeTestRule.setContent {
            ChatTheme {
                Box(modifier = Modifier.systemBarsPadding()) {
                    MultiThreadContent(
                        threads = threads,
                        onThreadSelected = {},
                        onArchiveThread = {},
                        state = State.Initial,
                        threadFailure = null,
                        resetState = {},
                        respondToSurvey = {},
                        resetCreateThreadState = {},
                        editThreadName = {}
                    )
                }
            }
        }
    }

    private fun setThreadContent(thread: Thread) {
        composeTestRule.setContent {
            ChatTheme {
                Box(modifier = Modifier.systemBarsPadding()) {
                    ChatThreadView(thread) {}
                }
            }
        }
    }
}
