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

package com.nice.cxonechat.ui.composable.screenshot

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
import com.nice.cxonechat.message.MessageDirection.ToAgent
import com.nice.cxonechat.message.MessageDirection.ToClient
import com.nice.cxonechat.thread.Agent
import com.nice.cxonechat.ui.composable.MultiThreadContent
import com.nice.cxonechat.ui.composable.PreviewThread
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import com.nice.cxonechat.ui.domain.model.Thread
import com.nice.cxonechat.ui.util.preview.message.Text
import com.nice.cxonechat.ui.viewmodel.ChatThreadsViewModel.State
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.annotation.GraphicsMode.Mode
import kotlin.time.Instant

/**
 * Verifies the thread list layout on wide/foldable screens (DE-163621).
 *
 * On screens at or above the expanded-width breakpoint the list content is inset from the screen
 * edges; on compact/portrait widths it must remain full-bleed exactly as before. The wide-screen
 * cases use the 852dp-wide qualifier that matches the design frame and sits above the breakpoint.
 *
 * The pixels alone can't catch a missing inset — start-aligned avatar/name content is far narrower
 * than the window, so each case also asserts the list's left edge directly ([assertListInset]):
 * [ChatTheme.space].landscapeHorizontalPadding on wide screens, 0dp in portrait. The fixtures use
 * real last-message text and one unread thread (trailing timestamp + unread dot) so the dividers and
 * text edges actually reach the inset boundary, and fixed message timestamps so the rendered time
 * text — and therefore the baselines — stay stable across runs.
 */
@RunWith(RobolectricTestRunner::class)
@Config(
    sdk = [33],
    qualifiers = "w411dp-h891dp-normal-notlong-notround-notnight-xhdpi-finger"
)
@GraphicsMode(Mode.NATIVE)
class ThreadListWideScreenScreenshotTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `thread list is full-bleed in portrait`() {
        renderThreadList()
        assertListInset(0.dp)
        capture()
    }

    @Test
    @Config(qualifiers = "w411dp-h891dp-normal-notlong-notround-night-xhdpi-finger")
    fun `thread list is full-bleed in portrait - dark`() {
        renderThreadList()
        assertListInset(0.dp)
        capture()
    }

    @Test
    @Config(qualifiers = "w852dp-h412dp-normal-notlong-notround-notnight-xhdpi-finger")
    fun `thread list content is inset on wide screen`() {
        renderThreadList()
        assertListInset(WIDE_SCREEN_INSET)
        capture()
    }

    @Test
    @Config(qualifiers = "w852dp-h412dp-normal-notlong-notround-night-xhdpi-finger")
    fun `thread list content is inset on wide screen - dark`() {
        renderThreadList()
        assertListInset(WIDE_SCREEN_INSET)
        capture()
    }

    private fun renderThreadList() {
        composeTestRule.setContent {
            ChatTheme {
                Surface(Modifier.fillMaxSize()) {
                    MultiThreadContent(
                        threads = threads(),
                        onThreadSelected = {},
                        onArchiveThread = {},
                        state = State.Initial,
                        threadFailure = null,
                        resetState = {},
                        respondToSurvey = {},
                        resetCreateThreadState = {},
                        editThreadName = {},
                    )
                }
            }
        }
        composeTestRule.waitForIdle()
    }

    // Assert the list's left edge directly so a dropped inset fails loudly instead of hiding behind
    // start-aligned content that the goldens can't distinguish inset-from-not.
    private fun assertListInset(expected: Dp) {
        val left = composeTestRule.onNodeWithTag("chat_thread_list_content").getUnclippedBoundsInRoot().left
        assertEquals(expected.value, left.value, INSET_TOLERANCE_DP)
    }

    private fun capture() {
        composeTestRule.onRoot().captureRoboImage(roborazziOptions = screenshotRoborazziOptions)
    }

    // Three threads with real last-message text; the first is unread (inbound, unseen) so it renders
    // an unread dot and its content reaches the trailing edge — exactly where a missing end inset
    // would show. Fixed timestamps keep the rendered time text (and the baselines) deterministic.
    private fun threads(): List<Thread> = List(THREAD_COUNT) { index ->
        val unread = index == 0
        val chatThread = PreviewThread(
            threadName = "Conversation ${index + 1}",
            threadAgent = agent(firstName = "Agent", lastName = "${index + 1}"),
            messages = listOf(
                Text(
                    text = "Last message in conversation ${index + 1}",
                    // Inbound + unseen renders as unread; outbound is treated as read.
                    direction = if (unread) ToClient else ToAgent,
                    createdAt = FIXED_MESSAGE_TIME,
                ),
            ),
        )
        Thread(chatThread, chatThread.threadName)
    }

    private fun agent(firstName: String?, lastName: String?): Agent = object : Agent() {
        override val id = 1
        override val firstName: String? = firstName
        override val lastName: String? = lastName
        override val nickname: String? = null
        override val isBotUser: Boolean = false
        override val isSurveyUser: Boolean = false
        override val imageUrl: String? = null
        override val isTyping = false
    }

    private companion object {
        const val THREAD_COUNT = 3

        // Matches Space.landscapeHorizontalPadding, the inset applied at/above the expanded-width breakpoint.
        val WIDE_SCREEN_INSET = 64.dp

        // Bounds are laid out from dp paddings, so any real regression is >> this; the slack only
        // absorbs sub-pixel density rounding.
        const val INSET_TOLERANCE_DP = 0.5f

        // Fixed message timestamp so the rendered time text is deterministic across runs.
        val FIXED_MESSAGE_TIME = Instant.fromEpochMilliseconds(1_700_000_000_000)
    }
}
