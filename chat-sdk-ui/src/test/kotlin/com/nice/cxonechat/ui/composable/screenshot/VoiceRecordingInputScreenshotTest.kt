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

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.core.app.ApplicationProvider
import com.github.takahirom.roborazzi.captureRoboImage
import com.nice.cxonechat.ui.R.string
import com.nice.cxonechat.ui.composable.conversation.AudioRecordingUiState
import com.nice.cxonechat.ui.composable.conversation.UserInput
import com.nice.cxonechat.ui.composable.conversation.previewUiState
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import com.nice.cxonechat.ui.composable.theme.wideScreenHorizontalPadding
import com.nice.cxonechat.ui.data.RequestResult
import com.nice.cxonechat.ui.util.ChatConfigurationRule
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Captures the drawn appearance of the message input bar while a voice message is recording, and while the
 * finished recording is offered for review.
 */
@RunWith(RobolectricTestRunner::class)
@Config(
    sdk = [33],
    qualifiers = "w411dp-h891dp-normal-notlong-notround-notnight-xhdpi-finger"
)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
internal class VoiceRecordingInputScreenshotTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @get:Rule
    val chatConfiguration = ChatConfigurationRule()

    /**
     * Models the real recorder: each toggle succeeds and flips the flag, so the microphone begins recording and
     * stop ends it.
     *
     * `previewAudioState` cannot serve here — its toggle answers `FAILURE`, which sends the bar back to text.
     */
    private val recordingState: AudioRecordingUiState = MutableStateFlow(false).let { isRecording ->
        AudioRecordingUiState(
            isRecordingAllowedFlow = MutableStateFlow(true),
            uriFlow = MutableStateFlow(Uri.EMPTY),
            isRecordingFlow = isRecording,
            durationFlow = MutableStateFlow(RECORDED_DURATION),
            onDismiss = {},
            onApprove = {},
            onAudioRecordToggle = {
                isRecording.value = !isRecording.value
                RequestResult.SUCCESS
            },
        )
    }

    @Test
    fun `voice recording compact light`() {
        showInputBar()
        startRecording()
        capture()
    }

    @Test
    @Config(qualifiers = "w411dp-h891dp-normal-notlong-notround-night-xhdpi-finger")
    fun `voice recording compact dark`() {
        showInputBar(darkTheme = true)
        startRecording()
        capture()
    }

    @Test
    @Config(qualifiers = "w852dp-h412dp-normal-notlong-notround-notnight-xhdpi-finger")
    fun `voice recording expanded light`() {
        showInputBar()
        startRecording()
        capture()
    }

    @Test
    @Config(qualifiers = "w852dp-h412dp-normal-notlong-notround-night-xhdpi-finger")
    fun `voice recording expanded dark`() {
        showInputBar(darkTheme = true)
        startRecording()
        capture()
    }

    /**
     * Capture RTL order — the trailing edge, contents and leading edge (reversed from LTR perspective).
     */
    @Test
    @Config(qualifiers = "w852dp-h412dp-normal-notlong-notround-notnight-xhdpi-finger")
    fun `voice recording expanded right to left`() {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                ChatTheme { InputBar() }
            }
        }
        startRecording()
        capture()
    }

    @Test
    fun `voice recording review compact light`() {
        showInputBar()
        startRecording()
        stopRecording()
        capture()
    }

    @Test
    @Config(qualifiers = "w852dp-h412dp-normal-notlong-notround-notnight-xhdpi-finger")
    fun `voice recording review expanded light`() {
        showInputBar()
        startRecording()
        stopRecording()
        capture()
    }

    private fun showInputBar(darkTheme: Boolean = false) {
        composeTestRule.setContent {
            ChatTheme(darkTheme = darkTheme) { InputBar() }
        }
    }

    private fun startRecording() = tap(string.record_audio_start_content_description)

    private fun stopRecording() = tap(string.record_audio_stop_content_description)

    private fun tap(descriptionRes: Int) {
        composeTestRule.onNodeWithContentDescription(label(descriptionRes)).performClick()
        composeTestRule.waitForIdle()
    }

    /** The bar merges its descendants, so the tag on its own surface is the one reachable unmerged. */
    private fun capture() {
        composeTestRule
            .onNodeWithTag("user_input", useUnmergedTree = true)
            .captureRoboImage(roborazziOptions = screenshotRoborazziOptions)
    }

    @Composable
    private fun InputBar() {
        UserInput(
            conversationUiState = previewUiState(),
            audioRecordingUiState = recordingState,
            onAttachmentTypeSelection = {},
            // Resolved as ChatConversation resolves it, that being what insets the controls.
            horizontalPadding = wideScreenHorizontalPadding(),
            showMessageProcessing = false,
            onError = {},
        )
    }

    private companion object {
        /**
         * A fixed, non-zero elapsed time, so the recording goldens show a timer rather than `00:00`. The
         * review pair reads the player's duration instead, which never leaves zero.
         */
        val RECORDED_DURATION: Duration = 12.seconds

        fun label(descriptionRes: Int): String =
            ApplicationProvider.getApplicationContext<Context>().getString(descriptionRes)
    }
}
