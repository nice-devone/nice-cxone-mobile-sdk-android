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

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import com.nice.cxonechat.thread.Agent
import com.nice.cxonechat.ui.composable.conversation.EndConversationContent
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.annotation.GraphicsMode.Mode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], qualifiers = "w411dp-h891dp-normal-notlong-notround-notnight-mdpi-finger")
@GraphicsMode(Mode.NATIVE)
class AgentPiiHidingScreenshotTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `end conversation no agent`() {
        composeTestRule.setContent {
            ChatTheme {
                Surface(Modifier.fillMaxWidth()) {
                    EndConversationContent(
                        agentState = null,
                        liveChatAllowTranscript = false,
                        onUserSelection = {},
                        onDismiss = {},
                    )
                }
            }
        }
        composeTestRule.onRoot().captureRoboImage()
    }

    @Test
    fun `end conversation agent with hidden personal information`() {
        composeTestRule.setContent {
            ChatTheme {
                Surface(Modifier.fillMaxWidth()) {
                    EndConversationContent(
                        agentState = hiddenPiiAgent(),
                        liveChatAllowTranscript = false,
                        onUserSelection = {},
                        onDismiss = {},
                    )
                }
            }
        }
        composeTestRule.onRoot().captureRoboImage()
    }

    @Test
    fun `end conversation agent with visible personal information`() {
        composeTestRule.setContent {
            ChatTheme {
                Surface(Modifier.fillMaxWidth()) {
                    EndConversationContent(
                        agentState = visiblePiiAgent(),
                        liveChatAllowTranscript = false,
                        onUserSelection = {},
                        onDismiss = {},
                    )
                }
            }
        }
        composeTestRule.onRoot().captureRoboImage()
    }

    @Test
    fun `end conversation agent with visible name and hidden image shows initials`() {
        composeTestRule.setContent {
            ChatTheme {
                Surface(Modifier.fillMaxWidth()) {
                    EndConversationContent(
                        agentState = visibleNameHiddenImageAgent(),
                        liveChatAllowTranscript = false,
                        onUserSelection = {},
                        onDismiss = {},
                    )
                }
            }
        }
        composeTestRule.onRoot().captureRoboImage()
    }

    @Test
    fun `end conversation agent with nickname shows nickname initials over full name`() {
        composeTestRule.setContent {
            ChatTheme {
                Surface(Modifier.fillMaxWidth()) {
                    EndConversationContent(
                        agentState = nicknamedAgent(),
                        liveChatAllowTranscript = false,
                        onUserSelection = {},
                        onDismiss = {},
                    )
                }
            }
        }
        composeTestRule.onRoot().captureRoboImage()
    }

    private fun hiddenPiiAgent(): Agent = object : Agent() {
        override val id = 1
        override val firstName: String? = null
        override val lastName: String? = null
        override val nickname: String? = null
        override val isBotUser: Boolean? = null
        override val isSurveyUser: Boolean? = null
        override val imageUrl: String? = null
        override val isTyping = false
    }

    private fun visiblePiiAgent(): Agent = object : Agent() {
        override val id = 2
        override val firstName = "Alice"
        override val lastName = "Smith"
        override val nickname: String? = null
        override val isBotUser = false
        override val isSurveyUser = false
        override val imageUrl = ""
        override val isTyping = false
    }

    private fun visibleNameHiddenImageAgent(): Agent = object : Agent() {
        override val id = 3
        override val firstName = "Bob"
        override val lastName = "Taylor"
        override val nickname: String? = null
        override val isBotUser = false
        override val isSurveyUser = false
        override val imageUrl: String? = null
        override val isTyping = false
    }

    private fun nicknamedAgent(): Agent = object : Agent() {
        override val id = 4
        override val firstName = "John"
        override val lastName = "Doe"
        override val nickname: String = "Bombardak"
        override val isBotUser = false
        override val isSurveyUser = false
        override val imageUrl: String? = null
        override val isTyping = false
    }
}
