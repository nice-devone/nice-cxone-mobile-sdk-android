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
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import com.nice.cxonechat.ui.composable.conversation.ChatConversation
import com.nice.cxonechat.ui.composable.conversation.previewAudioState
import com.nice.cxonechat.ui.composable.conversation.previewUiState
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import com.nice.cxonechat.ui.util.preview.message.QuickReply
import com.nice.cxonechat.ui.util.preview.message.ReplyButton
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.time.Instant

/**
 * Appearance of a quick reply message's options row, captured inside a real [ChatConversation] so the
 * bubble, spacing and wide-screen content inset are all in frame, in both portrait and landscape.
 *
 * Baseline for the quick reply options row's width in portrait and landscape (DE-163603): short options,
 * a long option that overflows a single chip's width, and many options wrapping onto a second row.
 */
@RunWith(RobolectricTestRunner::class)
@Config(
    sdk = [33],
    qualifiers = "w411dp-h891dp-normal-notlong-notround-notnight-xhdpi-finger"
)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class QuickReplyMessageScreenshotTest {

    /** These goldens hold the message's own timestamp, so an unpinned zone has to say so rather than diff. */
    @get:Rule
    internal val pinnedEnvironmentRule = PinnedEnvironmentRule()

    /** A conversation resolves its configuration from Koin as it composes. */
    @get:Rule
    internal val koinRule = ConversationKoinRule()

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `quick reply portrait`() {
        captureConversation(shortOptionsReply())
    }

    @Test
    fun `quick reply portrait long option`() {
        captureConversation(longOptionReply())
    }

    @Test
    @Config(qualifiers = LANDSCAPE_QUALIFIERS)
    fun `quick reply landscape`() {
        captureConversation(shortOptionsReply())
    }

    @Test
    @Config(qualifiers = LANDSCAPE_QUALIFIERS)
    fun `quick reply landscape many options`() {
        captureConversation(manyOptionsReply())
    }

    @Test
    @Config(qualifiers = LANDSCAPE_QUALIFIERS)
    fun `quick reply landscape long option`() {
        captureConversation(longOptionReply())
    }

    private fun captureConversation(message: QuickReply) {
        composeTestRule.setContent {
            ChatTheme {
                ChatConversation(
                    conversationState = previewUiState(
                        messages = listOf(message),
                        isArchived = false,
                        isLiveChat = false,
                        isAgentTyping = false,
                    ),
                    audioRecordingState = previewAudioState(),
                    onAttachmentTypeSelection = {},
                    onError = {},
                    showMessageProcessing = false,
                )
            }
        }
        // The class's own warm-up (first Compose/Robolectric frame in the JVM fork) can otherwise be
        // captured mid-layout, baking a blank frame into a golden: gate the capture on the message
        // actually being laid out rather than trusting waitForIdle() alone.
        composeTestRule.waitUntil(timeoutMillis = SETTLE_TIMEOUT_MILLIS) {
            composeTestRule.onAllNodesWithTag("quick_reply_message").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.waitForIdle()
        composeTestRule.onRoot().captureRoboImage(roborazziOptions = screenshotRoborazziOptions)
    }

    private fun shortOptionsReply(): QuickReply = QuickReply(
        title = "Which topic can we help you with today?",
        actions = listOf(
            ReplyButton("Billing"),
            ReplyButton("Technical support"),
            ReplyButton("Something else"),
        ),
        createdAt = FIXED_CREATED_AT,
    )

    private fun manyOptionsReply(): QuickReply = QuickReply(
        title = "Pick one of the following options",
        actions = List(MANY_OPTIONS_COUNT) { index -> ReplyButton("Option ${index + 1}") },
        createdAt = FIXED_CREATED_AT,
    )

    private fun longOptionReply(): QuickReply = QuickReply(
        title = "Which of these best describes your issue?",
        actions = listOf(
            ReplyButton("This is a very long option text that should not fit on a single normal chip width at all"),
            ReplyButton("Something else"),
        ),
        createdAt = FIXED_CREATED_AT,
    )

    private companion object {
        const val LANDSCAPE_QUALIFIERS = "w852dp-h412dp-normal-notlong-notround-notnight-xhdpi-finger"

        const val MANY_OPTIONS_COUNT = 12

        // Fixed message timestamp so the rendered time text is deterministic across runs.
        val FIXED_CREATED_AT = Instant.fromEpochMilliseconds(1_700_000_000_000)
    }
}
