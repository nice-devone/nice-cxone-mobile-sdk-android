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

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
import androidx.compose.material3.rememberTooltipState
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.nice.cxonechat.ui.composable.conversation.model.Message
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import com.nice.cxonechat.ui.util.preview.message.UiSdkUnsupportedMessage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Pins that [UnsupportedMessage]'s default tooltip state keeps the tooltip open past Material's
 * auto-dismiss.
 *
 * The negative case, with a non-persistent state, is what shows the test clock really reaches that
 * dismiss timeout; without it the positive case would pass even if the clock never got there.
 */
@RunWith(RobolectricTestRunner::class)
@Config(
    sdk = [33],
    qualifiers = "w411dp-h891dp-normal-notlong-notround-notnight-xhdpi-finger"
)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
internal class UnsupportedMessageTooltipPersistenceTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun `given the tooltip is shown, when advanced well past Material's default duration, then it is still displayed`() {
        composeTestRule.setContent {
            ChatTheme {
                Surface {
                    UnsupportedMessage(message = Message.Unsupported(UiSdkUnsupportedMessage()))
                }
            }
        }

        composeTestRule.onNodeWithTag(TAG_BODY).performClick()
        // The tooltip animates into its own window, so wait for it instead of for the tree to go idle.
        composeTestRule.waitUntil {
            composeTestRule.onAllNodesWithTag(TAG_TOOLTIP).fetchSemanticsNodes().isNotEmpty()
        }

        composeTestRule.mainClock.advanceTimeBy(PAST_DEFAULT_DURATION_MS)

        composeTestRule.onNodeWithTag(TAG_TOOLTIP).assertIsDisplayed()
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun `given a non-persistent tooltip state, when advanced the same amount, then it is gone`() {
        composeTestRule.setContent {
            ChatTheme {
                Surface {
                    UnsupportedMessage(
                        message = Message.Unsupported(UiSdkUnsupportedMessage()),
                        tooltipState = rememberTooltipState(isPersistent = false),
                    )
                }
            }
        }

        composeTestRule.onNodeWithTag(TAG_BODY).performClick()
        composeTestRule.waitUntil {
            composeTestRule.onAllNodesWithTag(TAG_TOOLTIP).fetchSemanticsNodes().isNotEmpty()
        }

        composeTestRule.mainClock.advanceTimeBy(PAST_DEFAULT_DURATION_MS)

        // A dismissed tooltip leaves the tree rather than staying hidden.
        composeTestRule.onNodeWithTag(TAG_TOOLTIP).assertDoesNotExist()
    }

    private companion object {
        const val TAG_BODY = "unsupported_message"
        const val TAG_TOOLTIP = "unsupported_message_tooltip"
        const val PAST_DEFAULT_DURATION_MS = 3_000L
    }
}
