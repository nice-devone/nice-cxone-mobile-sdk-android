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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import com.nice.cxonechat.thread.Agent
import com.nice.cxonechat.ui.composable.PreviewAgent
import com.nice.cxonechat.ui.composable.conversation.EndConversationBottomSheet
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Goldens for the end conversation choice sheet (DE-163624): [EndConversationBottomSheet] overrides
 * [com.nice.cxonechat.ui.composable.theme.WideScreenModalBottomSheet]'s `sheetMaxWidth`, so it stays a
 * centred 640dp card in a landscape window instead of spanning it, matching the Figma frames for this
 * ticket.
 */
@RunWith(RobolectricTestRunner::class)
@Config(
    sdk = [33],
    qualifiers = "w412dp-h852dp-normal-notlong-notround-notnight-xhdpi-finger"
)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class EndConversationBottomSheetScreenshotTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `end conversation bottom sheet portrait`() {
        captureSheet()
    }

    @Test
    @Config(qualifiers = "w412dp-h852dp-normal-notlong-notround-night-xhdpi-finger")
    fun `end conversation bottom sheet portrait dark`() {
        captureSheet(darkTheme = true)
    }

    @Test
    @Config(qualifiers = "w852dp-h412dp-normal-notlong-notround-notnight-xhdpi-finger")
    fun `end conversation bottom sheet landscape`() {
        captureSheet()
    }

    @Test
    @Config(qualifiers = "w852dp-h412dp-normal-notlong-notround-night-xhdpi-finger")
    fun `end conversation bottom sheet landscape dark`() {
        captureSheet(darkTheme = true)
    }

    private fun captureSheet(darkTheme: Boolean = false) {
        composeTestRule.setContent {
            ChatTheme(darkTheme = darkTheme) {
                // The sheet is drawn over this window rather than inside it, so the host needs a themed
                // surface of its own for the scrim either side of it to fall on.
                Surface(modifier = Modifier.fillMaxSize()) {
                    EndConversationBottomSheet(
                        assignedAgent = remember { mutableStateOf(AGENT) },
                        liveChatAllowTranscript = true,
                        onDismiss = {},
                        onUserSelection = {},
                    )
                }
            }
        }
        composeTestRule.waitUntil {
            composeTestRule.onAllNodesWithTag(SHEET_TAG).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.waitForIdle()
        composeTestRule.onRoot().captureRoboImage(roborazziOptions = screenshotRoborazziOptions)
    }

    private companion object {
        const val SHEET_TAG = "end_conversation_bottom_sheet"

        /**
         * Fixed rather than [PreviewAgent.nextAgent], which numbers each agent from a static counter and
         * points at a remote avatar — the name would then depend on what ran before this test, and the
         * avatar on whether the fetch happened to finish. No image url leaves the deterministic initials.
         */
        val AGENT: Agent = PreviewAgent(
            id = 1,
            firstName = "Sample",
            lastName = "Agent",
            imageUrl = null,
        )
    }
}
