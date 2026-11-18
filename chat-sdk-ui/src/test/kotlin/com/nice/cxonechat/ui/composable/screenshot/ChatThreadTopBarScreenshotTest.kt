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

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import com.nice.cxonechat.thread.ChatThreadState.Ready
import com.nice.cxonechat.ui.composable.conversation.ChatThreadTopBar
import com.nice.cxonechat.ui.composable.conversation.model.ConversationTopBarState
import com.nice.cxonechat.ui.composable.theme.BackButton
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Verifies that the conversation top app bar keeps its title on a single row in a landscape window,
 * where vertical space is scarcest, and uses the taller two-row layout in a portrait window.
 */
@RunWith(RobolectricTestRunner::class)
@Config(
    sdk = [33],
    qualifiers = "w411dp-h891dp-normal-notlong-notround-notnight-xhdpi-finger"
)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ChatThreadTopBarScreenshotTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `thread top bar portrait is two rows`() {
        captureTopBar()
    }

    @Test
    @Config(qualifiers = "w852dp-h412dp-normal-notlong-notround-notnight-xhdpi-finger")
    fun `thread top bar landscape is single row`() {
        captureTopBar()
    }

    @Test
    @Config(qualifiers = "w852dp-h412dp-normal-notlong-notround-notnight-xhdpi-finger")
    fun `thread top bar landscape with ai badge is single row`() {
        captureTopBar(hasAiMessages = true)
    }

    @Test
    fun `thread top bar portrait with ai badge is two rows`() {
        captureTopBar(hasAiMessages = true)
    }

    private fun captureTopBar(hasAiMessages: Boolean = false) {
        composeTestRule.setContent {
            ChatTheme {
                ChatThreadTopBar(
                    scrollBehavior = null,
                    conversationState = topBarState(hasAiMessages),
                    onEditThreadName = {},
                    onEditThreadValues = {},
                    onEndContact = {},
                    displayEndConversation = {},
                    onSendTranscript = {},
                    navigationIcon = { BackButton {} },
                )
            }
        }
        composeTestRule.onRoot().captureRoboImage(roborazziOptions = screenshotRoborazziOptions)
    }

    private fun topBarState(hasAiMessages: Boolean) = ConversationTopBarState(
        threadName = flowOf(null),
        isMultiThreaded = false,
        isLiveChat = false,
        hasQuestions = false,
        isArchived = MutableStateFlow(false),
        threadState = MutableStateFlow(Ready),
        liveChatAllowTranscript = false,
        agentName = flowOf("Agent Smith"),
        hasAiMessages = MutableStateFlow(hasAiMessages),
        hasAgent = flowOf(true),
    )
}
