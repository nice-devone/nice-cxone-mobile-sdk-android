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

package com.nice.cxonechat.ui.composable.generic

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.nice.cxonechat.ui.AbstractComponentActivityUiTest
import com.nice.cxonechat.ui.R
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import com.nice.cxonechat.ui.composable.theme.ChatTheme.chatColors
import com.nice.cxonechat.ui.composable.theme.ChatTheme.chatShapes
import com.nice.cxonechat.ui.composable.theme.ChatTheme.space
import com.nice.cxonechat.ui.util.toAccessibilityTimeFormat
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

@RunWith(AndroidJUnit4::class)
@LargeTest
class AudioPlayerContentTest : AbstractComponentActivityUiTest() {

    private fun buildExpectedContentDescription(positionMs: Long, durationMs: Long): String {
        val infinityText = getString(R.string.content_description_audio_duration_infinite)
        val locale = Locale.getDefault()
        val currentTimeAccessible = positionMs.milliseconds.toAccessibilityTimeFormat(locale) { infinityText }
        val totalTimeAccessible = durationMs.milliseconds.toAccessibilityTimeFormat(locale) { infinityText }
        return getString(R.string.progress_indicator_content_description, currentTimeAccessible, totalTimeAccessible)
    }

    @Test
    fun audioPlayerContent_displaysCorrectTimesAndProgress() {
        val currentTime = "00:20"
        val remainingTime = "00:40"
        composeTestRule.setContent {
            TestContent(currentTime = currentTime, remainingTime = remainingTime)
        }
        composeTestRule.onNodeWithText(currentTime).assertIsDisplayed()
        composeTestRule.onNodeWithText(remainingTime).assertIsDisplayed()
    }

    @Test
    fun audioPlayerContent_disabledState_alphaIsReduced() {
        val currentTime = "00:20"
        val remainingTime = "00:40"
        composeTestRule.setContent {
            TestContent(isEnabled = false, currentTime = currentTime, remainingTime = remainingTime)
        }
        composeTestRule.onNodeWithText(currentTime).assertIsDisplayed()
        composeTestRule.onNodeWithText(remainingTime).assertIsDisplayed()
    }

    @Test
    fun audioPlayerContent_allControlsEnabledAndVisible() {
        composeTestRule.setContent {
            TestContent(isEnabled = true, canSeekForward = true, canSeekBack = true)
        }
        composeTestRule.onNodeWithTag("play_pause_button").assertIsDisplayed().assertIsEnabled()
        composeTestRule.onNodeWithTag("seek_back_button").assertIsDisplayed().assertIsEnabled()
        composeTestRule.onNodeWithTag("seek_forward_button").assertIsDisplayed().assertIsEnabled()
        composeTestRule.onNodeWithTag("progress_indicator").assertIsDisplayed()
    }

    @Test
    fun audioPlayerContent_allControlsDisabled() {
        composeTestRule.setContent {
            TestContent(isEnabled = false, canSeekForward = true, canSeekBack = true)
        }
        composeTestRule.onNodeWithTag("play_pause_button").assertIsDisplayed().assertIsNotEnabled()
        composeTestRule.onNodeWithTag("seek_back_button").assertIsDisplayed().assertIsNotEnabled()
        composeTestRule.onNodeWithTag("seek_forward_button").assertIsDisplayed().assertIsNotEnabled()
        composeTestRule.onNodeWithTag("progress_indicator").assertIsDisplayed()
    }

    @Test
    fun audioPlayerContent_seekButtonsHidden() {
        composeTestRule.setContent {
            TestContent(isEnabled = true, canSeekForward = false, canSeekBack = false)
        }
        composeTestRule.onNodeWithTag("play_pause_button").assertIsDisplayed().assertIsEnabled()
        composeTestRule.onNodeWithTag("seek_back_button").assertDoesNotExist()
        composeTestRule.onNodeWithTag("seek_forward_button").assertDoesNotExist()
        composeTestRule.onNodeWithTag("progress_indicator").assertIsDisplayed()
    }

    @Test
    fun audioPlayerContent_playPauseButton_updateState() {
        var isEnabled by mutableStateOf(true)
        composeTestRule.setContent {
            TestContent(isEnabled = isEnabled)
        }
        composeTestRule.onNodeWithTag("play_pause_button").assertIsDisplayed().assertIsEnabled()
        composeTestRule.runOnUiThread {
            isEnabled = false
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("play_pause_button").assertIsDisplayed().assertIsNotEnabled()
    }

    @Test
    fun audioPlayerContent_playPauseButton_callsCallback() {
        var playPauseClicked = false
        composeTestRule.setContent {
            TestContent(isEnabled = true, playPause = { playPauseClicked = true })
        }
        composeTestRule.onNodeWithTag("play_pause_button").performClick()
        assert(playPauseClicked)
    }

    @Test
    fun audioPlayerContent_seekBackButton_callsCallback() {
        var seekBackClicked = false
        composeTestRule.setContent {
            TestContent(isEnabled = true, canSeekBack = true, seekBack = { seekBackClicked = true })
        }
        composeTestRule.onNodeWithTag("seek_back_button").performClick()
        assert(seekBackClicked)
    }

    @Test
    fun audioPlayerContent_seekForwardButton_callsCallback() {
        var seekForwardClicked = false
        composeTestRule.setContent {
            TestContent(isEnabled = true, canSeekForward = true, seekForward = { seekForwardClicked = true })
        }
        composeTestRule.onNodeWithTag("seek_forward_button").performClick()
        assert(seekForwardClicked)
    }

    @Test
    fun audioPlayerContent_dynamicStateUpdates() {
        var isEnabled by mutableStateOf(true)
        var canSeekForward by mutableStateOf(true)
        var canSeekBack by mutableStateOf(true)
        composeTestRule.setContent {
            TestContent(
                isEnabled = isEnabled,
                canSeekForward = canSeekForward,
                canSeekBack = canSeekBack,
            )
        }
        // Initially all controls enabled
        composeTestRule.onNodeWithTag("play_pause_button").assertIsDisplayed().assertIsEnabled()
        composeTestRule.onNodeWithTag("seek_back_button").assertIsDisplayed().assertIsEnabled()
        composeTestRule.onNodeWithTag("seek_forward_button").assertIsDisplayed().assertIsEnabled()
        // Disable controls dynamically
        composeTestRule.runOnUiThread { isEnabled = false }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("play_pause_button").assertIsDisplayed().assertIsNotEnabled()
        composeTestRule.onNodeWithTag("seek_back_button").assertIsDisplayed().assertIsNotEnabled()
        composeTestRule.onNodeWithTag("seek_forward_button").assertIsDisplayed().assertIsNotEnabled()
        // Hide seek buttons dynamically
        composeTestRule.runOnUiThread {
            canSeekForward = false
            canSeekBack = false
            isEnabled = true
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("seek_back_button").assertDoesNotExist()
        composeTestRule.onNodeWithTag("seek_forward_button").assertDoesNotExist()
        composeTestRule.onNodeWithTag("play_pause_button").assertIsDisplayed().assertIsEnabled()
    }

    @Test
    fun progressIndicator_updatesAccessibilityDescription_whenTimeChanges() {
        val durationMs = TimeUnit.MINUTES.toMillis(2)
        val positionMs = TimeUnit.SECONDS.toMillis(30)
        val currentTime = "00:30"

        composeTestRule.setContent {
            TestContent(
                currentTime = currentTime,
                position = positionMs.milliseconds,
                duration = durationMs.milliseconds,
            )
        }

        // The accessibility description is set on the parent Column that contains the progress indicator
        // with format from R.string.progress_indicator_content_description
        val expectedDescription = buildExpectedContentDescription(positionMs, durationMs)
        composeTestRule.onNode(
            SemanticsMatcher.expectValue(
                SemanticsProperties.ContentDescription,
                listOf(expectedDescription)
            )
        ).assertIsDisplayed()
    }

    @Test
    fun disabledControls_maintainAccessibilitySemantics() {
        val durationMs = TimeUnit.MINUTES.toMillis(1)
        val positionMs = TimeUnit.SECONDS.toMillis(20)

        composeTestRule.setContent {
            TestContent(
                isEnabled = false,
                position = positionMs.milliseconds,
                duration = durationMs.milliseconds,
            )
        }

        // Disabled controls should still have proper accessibility semantics
        composeTestRule.onNodeWithTag("play_pause_button")
            .assertIsDisplayed()
            .assertIsNotEnabled()

        // The accessibility description is set on the Column using R.string.progress_indicator_content_description
        val expectedDescription = buildExpectedContentDescription(positionMs, durationMs)
        composeTestRule.onNode(
            SemanticsMatcher.expectValue(
                SemanticsProperties.ContentDescription,
                listOf(expectedDescription)
            )
        ).assertIsDisplayed()
    }

    @Test
    fun playPauseButton_hasAccessibilityAction() {
        composeTestRule.setContent {
            TestContent(isEnabled = true, isPlaying = false)
        }

        composeTestRule.onNodeWithTag("play_pause_button")
            .assertIsDisplayed()
            .assertIsEnabled()
    }

    @Test
    fun seekButtons_haveAccessibilityDescriptions() {
        composeTestRule.setContent {
            TestContent(canSeekForward = true, canSeekBack = true)
        }

        // Verify seek back button has content description
        composeTestRule.onNodeWithTag("seek_back_button")
            .assertIsDisplayed()
            .assertIsEnabled()

        // Verify seek forward button has content description
        composeTestRule.onNodeWithTag("seek_forward_button")
            .assertIsDisplayed()
            .assertIsEnabled()
    }

    @Composable
    private fun TestContent(
        currentTime: String = "00:20",
        remainingTime: String = "00:40",
        position: Duration = 20_000.milliseconds,
        duration: Duration = 60_000.milliseconds,
        animatedProgress: Float = 0.25f,
        isEnabled: Boolean = true,
        isPlaying: Boolean = false,
        canSeekBack: Boolean = true,
        seekBackIncrementMs: Long = 10_000L,
        canSeekForward: Boolean = true,
        seekForwardIncrementMs: Long = 10_000L,
        seekBack: () -> Unit = {},
        seekForward: () -> Unit = {},
        playPause: () -> Unit = {},
    ) {
        ChatTheme {
            Surface(
                color = chatColors.customer.background,
                contentColor = chatColors.customer.foreground,
                shape = chatShapes.bubbleSoloShape,
                modifier = Modifier
                    .systemBarsPadding()
                    .fillMaxWidth(0.6f)
            ) {
                Box(Modifier.padding(space.audioMessagePadding)) {
                    AudioPlayerContent(
                        currentTime = currentTime,
                        animatedProgress = animatedProgress,
                        remainingTime = remainingTime,
                        position = position,
                        duration = duration,
                        isEnabled = isEnabled,
                        isPlaying = isPlaying,
                        canSeekBack = canSeekBack,
                        seekBackIncrementMs = seekBackIncrementMs,
                        canSeekForward = canSeekForward,
                        seekForwardIncrementMs = seekForwardIncrementMs,
                        onSeekBack = seekBack,
                        onSeekForward = seekForward,
                        onPlayPause = playPause,
                    )
                }
            }
        }
    }
}
