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

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.width
import androidx.test.core.app.ApplicationProvider
import com.nice.cxonechat.ui.R.string
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import com.nice.cxonechat.ui.composable.theme.wideScreenHorizontalPadding
import com.nice.cxonechat.ui.data.RequestResult
import com.nice.cxonechat.ui.util.ChatConfigurationRule
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.time.Duration

/**
 * Pins the inset between the window and the voice recording controls.
 *
 * The row is reached by pressing the microphone rather than composed directly, so the inset is measured
 * through the chain that decides it: the larger of the wide-screen padding `ChatConversation` resolves and the
 * row's own margin.
 *
 * Edges of the row, not of the buttons in it. `ChatIconButton` applies `minimumInteractiveComponentSize`, so a
 * button reports Material's touch target rather than the 44dp it paints.
 */
@RunWith(RobolectricTestRunner::class)
@Config(
    sdk = [33],
    qualifiers = "w411dp-h891dp-normal-notlong-notround-notnight-xhdpi-finger"
)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
internal class VoiceRecordingInputLayoutTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @get:Rule
    val chatConfiguration = ChatConfigurationRule()

    /** `852 − 2 × 64` leaves the design's 724dp, the wide inset having replaced the row's own 16dp. */
    @Test
    @Config(qualifiers = "w852dp-h412dp-normal-notlong-notround-notnight-xhdpi-finger")
    fun `a wide window insets the recording controls by the wide-screen padding`() {
        startRecording()

        val row = recordingRowBounds()
        assertEquals(64f, row.left.value, TOLERANCE)
        assertEquals(788f, row.left.value + row.width.value, TOLERANCE)
    }

    /** `411 − 2 × 16` leaves 379dp; the wide-screen padding does not apply to a compact window. */
    @Test
    fun `a compact window insets the recording controls by 16dp`() {
        startRecording()

        val row = recordingRowBounds()
        assertEquals(16f, row.left.value, TOLERANCE)
        assertEquals(395f, row.left.value + row.width.value, TOLERANCE)
    }

    /**
     * The tag sits after the padding in the row's chain, so these bounds start at the inset being measured.
     *
     * Unmerged, because the row is a traversal group the input bar merges away.
     */
    private fun recordingRowBounds(): DpRect =
        composeTestRule.onNodeWithTag("audio_input_row", useUnmergedTree = true).getUnclippedBoundsInRoot()

    private fun startRecording() {
        composeTestRule.setContent { InputBar() }
        composeTestRule.onNodeWithContentDescription(startRecordingLabel).performClick()
        composeTestRule.waitForIdle()
    }

    @Composable
    private fun InputBar() {
        ChatTheme {
            UserInput(
                conversationUiState = previewUiState(),
                audioRecordingUiState = recordingState(),
                onAttachmentTypeSelection = {},
                // Resolved as ChatConversation resolves it: one of the two values the row takes the larger of.
                horizontalPadding = wideScreenHorizontalPadding(),
                showMessageProcessing = false,
                onError = {},
            )
        }
    }

    private companion object {
        const val TOLERANCE = 0.5f

        val startRecordingLabel: String
            get() = ApplicationProvider.getApplicationContext<Context>()
                .getString(string.record_audio_start_content_description)

        /**
         * Reports recording in progress from the first frame after the toggle succeeds.
         *
         * `previewAudioState` is unusable here: its toggle answers `FAILURE`, which sends the input bar back to
         * the text state. `Uri.EMPTY` holds the player at `Loading` without building an `ExoPlayer`, which
         * would never reach `Ready` under Robolectric.
         */
        fun recordingState() = AudioRecordingUiState(
            isRecordingAllowedFlow = MutableStateFlow(true),
            uriFlow = MutableStateFlow(Uri.EMPTY),
            isRecordingFlow = MutableStateFlow(true),
            durationFlow = MutableStateFlow(Duration.ZERO),
            onDismiss = {},
            onApprove = {},
            onAudioRecordToggle = { RequestResult.SUCCESS },
        )
    }
}
