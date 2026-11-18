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

import android.app.Application
import android.content.pm.ApplicationInfo.FLAG_SUPPORTS_RTL
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.test.core.app.ApplicationProvider
import com.github.takahirom.roborazzi.captureRoboImage
import com.nice.cxonechat.message.MessageDirection.ToAgent
import com.nice.cxonechat.thread.Agent
import com.nice.cxonechat.ui.composable.ChatThreadView
import com.nice.cxonechat.ui.composable.PreviewThread
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import com.nice.cxonechat.ui.domain.model.Thread
import com.nice.cxonechat.ui.util.preview.message.Text
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import org.junit.runners.model.Statement
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.annotation.GraphicsMode.Mode
import kotlin.time.Instant

/**
 * Pins where the thread-list long-press menu opens: under the press point, in both layout directions.
 *
 * Each case long-presses near the cell's start edge, so the RTL golden mirrors the LTR one. RTL comes from an
 * Arabic locale rather than [androidx.compose.ui.platform.LocalLayoutDirection], which does not reach the menu's
 * popup window; the SDK has no Arabic strings, so the labels stay English. [onRoot] captures the popup together
 * with the cell.
 *
 * The `ldrtl` qualifier leaves the configuration LTR under Robolectric, and a locale alone is not enough either:
 * Views resolve RTL only in an app declaring `supportsRtl`, which the test app does not, hence [rtlSupportRule].
 */
@RunWith(RobolectricTestRunner::class)
@Config(
    sdk = [33],
    qualifiers = "w411dp-h891dp-normal-notlong-notround-notnight-xhdpi-finger"
)
@GraphicsMode(Mode.NATIVE)
class ThreadContextMenuScreenshotTest {

    // Ordered first: the window resolves its layout direction once, when the compose rule launches the activity.
    @get:Rule(order = 0)
    val rtlSupportRule = TestRule { base, _ ->
        object : Statement() {
            override fun evaluate() {
                ApplicationProvider.getApplicationContext<Application>().applicationInfo.apply {
                    flags = flags or FLAG_SUPPORTS_RTL
                }
                base.evaluate()
            }
        }
    }

    @get:Rule(order = 1)
    val composeTestRule = createComposeRule()

    @get:Rule(order = 2)
    internal val pinnedEnvironmentRule = PinnedEnvironmentRule()

    private val agent: Agent = object : Agent() {
        override val id = 1
        override val firstName: String? = "Agent"
        override val lastName: String? = "Smith"
        override val nickname: String? = null
        override val isBotUser: Boolean = false
        override val isSurveyUser: Boolean = false
        override val imageUrl: String? = null
        override val isTyping = false
    }

    private val thread: Thread = PreviewThread(
        threadName = "Conversation",
        threadAgent = agent,
        messages = listOf(Text(text = "Last message", direction = ToAgent, createdAt = FIXED_MESSAGE_TIME)),
        canAddMoreMessages = true,
    ).let { Thread(it, it.threadName) }

    private val menuTag = "chat_thread_context_menu_${thread.id}"

    @Test
    fun `context menu opens under a press near the start edge - ltr`() {
        val touchX = longPressNearStartEdge(isRtl = false)
        capture()
        assertMenuSpans(touchX)
    }

    @Test
    @Config(qualifiers = "+ar")
    fun `context menu opens under a press near the start edge - rtl`() {
        val touchX = longPressNearStartEdge(isRtl = true)
        capture()
        assertMenuSpans(touchX)
    }

    /** Returns the press point's x on screen. */
    private fun longPressNearStartEdge(isRtl: Boolean): Float {
        composeTestRule.setContent {
            ChatTheme {
                Surface(Modifier.fillMaxSize()) {
                    // Surface passes its full-window minimum size on to its child; the Column releases it,
                    // as the real list does, so the menu anchors to the cell rather than to the window.
                    Column {
                        ChatThreadView(
                            thread = thread,
                            onThreadSelected = {},
                            onArchiveThread = {},
                            onRenameThread = {},
                        )
                    }
                }
            }
        }
        val cell = composeTestRule.onNodeWithTag("chat_thread_view_${thread.id}")
        var touchX = 0f
        cell.performTouchInput {
            touchX = if (isRtl) right - TOUCH_INSET_PX else left + TOUCH_INSET_PX
            longClick(Offset(touchX, centerY))
        }
        composeTestRule.waitUntil {
            composeTestRule.onAllNodesWithTag(menuTag).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.waitForIdle()
        return cell.fetchSemanticsNode().positionOnScreen.x + touchX
    }

    // The golden shows it; this pins it numerically too, since the menu is small against the window.
    private fun assertMenuSpans(touchX: Float) {
        val menu = composeTestRule.onNodeWithTag(menuTag).fetchSemanticsNode()
        val menuLeft = menu.positionOnScreen.x
        assertTrue(
            "touch at $touchX should lie within the menu [$menuLeft, ${menuLeft + menu.size.width}]",
            touchX in menuLeft..menuLeft + menu.size.width,
        )
    }

    private fun capture() {
        composeTestRule.onRoot().captureRoboImage(roborazziOptions = screenshotRoborazziOptions)
    }

    private companion object {
        // 30dp at xhdpi: near the start edge, so a menu opened from the wrong edge lands far from the press.
        const val TOUCH_INSET_PX = 60f

        val FIXED_MESSAGE_TIME = Instant.fromEpochMilliseconds(1_700_000_000_000)
    }
}
