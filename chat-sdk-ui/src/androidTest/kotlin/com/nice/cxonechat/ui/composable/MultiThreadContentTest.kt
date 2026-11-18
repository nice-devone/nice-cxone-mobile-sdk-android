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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import androidx.compose.ui.unit.LayoutDirection
import com.nice.cxonechat.ui.AbstractComponentActivityUiTest
import com.nice.cxonechat.ui.R
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import com.nice.cxonechat.ui.domain.model.Thread
import com.nice.cxonechat.ui.viewmodel.ChatThreadsViewModel.State
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
    fun chatThreadView_longPress_showsContextMenuWithRenameAndArchive() {
        val thread = createThread("Active Thread", canAddMoreMessages = true)

        setThreadContent(thread, onArchiveThread = {}, onRenameThread = {})

        // Menu is not shown until the cell is long-pressed. A closed DropdownMenu is not composed at
        // all, so the item is absent from the semantics tree (assertDoesNotExist), not merely hidden.
        composeTestRule.onNodeWithTag("archive_thread_menu_item").assertDoesNotExist()

        composeTestRule.onNodeWithTag("chat_thread_view_${thread.id}")
            .performTouchInput { longClick() }

        composeTestRule.onNodeWithTag("change_thread_name_menu_item").assertIsDisplayed()
        composeTestRule.onNodeWithTag("archive_thread_menu_item").assertIsDisplayed()
    }

    @Test
    fun chatThreadView_longPress_archiveMenuItem_invokesArchiveCallback() {
        val thread = createThread("Active Thread", canAddMoreMessages = true)
        var archived: Thread? = null

        setThreadContent(thread, onArchiveThread = { archived = it }, onRenameThread = {})

        composeTestRule.onNodeWithTag("chat_thread_view_${thread.id}")
            .performTouchInput { longClick() }
        composeTestRule.onNodeWithTag("archive_thread_menu_item").performClick()

        assertEquals(thread.id, archived?.id)
    }

    @Test
    fun chatThreadView_longPress_renameMenuItem_invokesRenameCallback() {
        val thread = createThread("Active Thread", canAddMoreMessages = true)
        var renamed: Thread? = null

        setThreadContent(thread, onArchiveThread = {}, onRenameThread = { renamed = it })

        composeTestRule.onNodeWithTag("chat_thread_view_${thread.id}")
            .performTouchInput { longClick() }
        composeTestRule.onNodeWithTag("change_thread_name_menu_item").performClick()

        assertEquals(thread.id, renamed?.id)
    }

    @Test
    fun chatThreadView_archivedThread_longPress_doesNotShowArchiveAction() {
        val thread = createThread("Archived Thread", canAddMoreMessages = false)

        // Even when an archive callback is provided, an already-archived thread must not offer Archive.
        setThreadContent(thread, onArchiveThread = {}, onRenameThread = {})

        composeTestRule.onNodeWithTag("chat_thread_view_${thread.id}")
            .performTouchInput { longClick() }

        // Rename is still offered, Archive is not. A suppressed menu item is never composed, so it is
        // absent from the tree rather than present-but-hidden.
        composeTestRule.onNodeWithTag("change_thread_name_menu_item").assertIsDisplayed()
        composeTestRule.onNodeWithTag("archive_thread_menu_item").assertDoesNotExist()
    }

    @Test
    fun chatThreadView_rtl_longPress_opensMenuUnderTouchPoint() {
        assertMenuOpensUnderTouchPoint(LayoutDirection.Rtl)
    }

    @Test
    fun chatThreadView_ltr_longPress_opensMenuUnderTouchPoint() {
        assertMenuOpensUnderTouchPoint(LayoutDirection.Ltr)
    }

    /**
     * Long-presses near the cell's left edge and asserts the menu spans the press point horizontally,
     * whatever the layout direction. The menu is a popup in its own window, so both are compared in
     * screen coordinates.
     */
    private fun assertMenuOpensUnderTouchPoint(layoutDirection: LayoutDirection) {
        val thread = createThread("Active Thread", canAddMoreMessages = true)
        setThreadContent(thread, onArchiveThread = {}, onRenameThread = {}, layoutDirection = layoutDirection)

        val cell = composeTestRule.onNodeWithTag("chat_thread_view_${thread.id}")
        cell.performTouchInput { longClick(Offset(left + TOUCH_INSET_PX, centerY)) }

        val touchOnScreenX = cell.fetchSemanticsNode().positionOnScreen.x + TOUCH_INSET_PX
        val menu = composeTestRule.onNodeWithTag("chat_thread_context_menu_${thread.id}").fetchSemanticsNode()
        val menuLeft = menu.positionOnScreen.x
        val menuRight = menuLeft + menu.size.width
        // The offset makes a px -> dp -> px round trip, so allow a pixel of rounding on either edge.
        assertTrue(
            "touch at $touchOnScreenX should lie within the menu [$menuLeft, $menuRight]",
            touchOnScreenX in menuLeft - 1f..menuRight + 1f,
        )
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

    private fun setThreadContent(
        thread: Thread,
        onArchiveThread: ((Thread) -> Unit)? = null,
        onRenameThread: ((Thread) -> Unit)? = null,
        layoutDirection: LayoutDirection = LayoutDirection.Ltr,
    ) {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                ChatTheme {
                    Box(modifier = Modifier.systemBarsPadding()) {
                        ChatThreadView(
                            thread = thread,
                            onThreadSelected = {},
                            onArchiveThread = onArchiveThread,
                            onRenameThread = onRenameThread,
                        )
                    }
                }
            }
        }
    }

    private companion object {
        const val TOUCH_INSET_PX = 40f
    }
}
