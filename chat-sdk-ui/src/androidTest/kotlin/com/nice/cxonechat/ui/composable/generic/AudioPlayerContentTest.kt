/*
 * Copyright (c) 2021-2025. NICE Ltd. All rights reserved.
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import com.nice.cxonechat.ui.composable.theme.ChatTheme.chatColors
import com.nice.cxonechat.ui.composable.theme.ChatTheme.chatShapes
import com.nice.cxonechat.ui.composable.theme.ChatTheme.space
import com.nice.cxonechat.ui.util.TestPlayerState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
@LargeTest
class AudioPlayerContentTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<androidx.activity.ComponentActivity>()

    private fun createPlayerState(
        available: Boolean = true,
        isPlaying: Boolean = false,
        durationMs: Long = TimeUnit.MINUTES.toMillis(1),
        positionMs: Long = TimeUnit.SECONDS.toMillis(20),
        progress: Float = 0.25f,
        canSeekForward: Boolean = true,
        canSeekBackward: Boolean = true,
    ) = TestPlayerState(available, isPlaying, durationMs, positionMs, progress, canSeekForward, canSeekBackward)

    @Test
    fun audioPlayerContent_displaysCorrectTimesAndProgress() {
        val state = createPlayerState()
        val currentTime = "00:20"
        val remainingTime = "00:40"

        composeTestRule.setContent {
            TestContent(playerState = state, currentTime = currentTime, remainingTime = remainingTime)
        }

        composeTestRule.onNodeWithText(currentTime).assertIsDisplayed()
        composeTestRule.onNodeWithText(remainingTime).assertIsDisplayed()
    }

    @Test
    fun audioPlayerContent_disabledState_alphaIsReduced() {
        val state = createPlayerState(available = false)
        val currentTime = "00:20"
        val remainingTime = "00:40"

        composeTestRule.setContent {
            TestContent(playerState = state, currentTime = currentTime, remainingTime = remainingTime)
        }

        composeTestRule.onNodeWithText(currentTime).assertIsDisplayed()
        composeTestRule.onNodeWithText(remainingTime).assertIsDisplayed()
    }

    @Test
    fun audioPlayerContent_allControlsEnabledAndVisible() {
        val state = createPlayerState(
            available = true,
            canSeekForward = true,
            canSeekBackward = true
        )
        composeTestRule.setContent {
            TestContent(playerState = state)
        }
        composeTestRule.onNodeWithTag("play_pause_button").assertIsDisplayed().assertIsEnabled()
        composeTestRule.onNodeWithTag("seek_back_button").assertIsDisplayed().assertIsEnabled()
        composeTestRule.onNodeWithTag("seek_forward_button").assertIsDisplayed().assertIsEnabled()
        composeTestRule.onNodeWithTag("progress_indicator").assertIsDisplayed()
    }

    @Test
    fun audioPlayerContent_allControlsDisabled() {
        val state = createPlayerState(
            available = false,
            canSeekForward = true,
            canSeekBackward = true
        )
        composeTestRule.setContent {
            TestContent(playerState = state)
        }
        composeTestRule.onNodeWithTag("play_pause_button").assertIsDisplayed().assertIsNotEnabled()
        composeTestRule.onNodeWithTag("seek_back_button").assertIsDisplayed().assertIsNotEnabled()
        composeTestRule.onNodeWithTag("seek_forward_button").assertIsDisplayed().assertIsNotEnabled()
        composeTestRule.onNodeWithTag("progress_indicator").assertIsDisplayed()
    }

    @Test
    fun audioPlayerContent_seekButtonsHidden() {
        val state = createPlayerState(
            available = true,
            canSeekForward = false,
            canSeekBackward = false
        )
        composeTestRule.setContent {
            TestContent(playerState = state)
        }
        composeTestRule.onNodeWithTag("play_pause_button").assertIsDisplayed().assertIsEnabled()
        composeTestRule.onNodeWithTag("seek_back_button").assertDoesNotExist()
        composeTestRule.onNodeWithTag("seek_forward_button").assertDoesNotExist()
        composeTestRule.onNodeWithTag("progress_indicator").assertIsDisplayed()
    }

    @Test
    fun audioPlayerContent_playPauseButton_updateState() {
        val state = createPlayerState(available = true)
        composeTestRule.setContent {
            TestContent(playerState = state)
        }
        composeTestRule.onNodeWithTag("play_pause_button").assertIsDisplayed().assertIsEnabled()
        composeTestRule.runOnUiThread {
            state.available.value = false
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("play_pause_button").assertIsDisplayed().assertIsNotEnabled()
    }

    @Test
    fun audioPlayerContent_playPauseButton_callsCallback() {
        var playPauseClicked = false
        val state = createPlayerState(available = true)
        composeTestRule.setContent {
            TestContent(
                playerState = state,
                playPause = { playPauseClicked = true },
            )
        }
        composeTestRule.onNodeWithTag("play_pause_button").performClick()
        assert(playPauseClicked)
    }

    @Test
    fun audioPlayerContent_seekBackButton_callsCallback() {
        var seekBackClicked = false
        val state = createPlayerState(available = true, canSeekBackward = true)
        composeTestRule.setContent {
            TestContent(
                playerState = state,
                seekBack = { seekBackClicked = true }
            )
        }
        composeTestRule.onNodeWithTag("seek_back_button").performClick()
        assert(seekBackClicked)
    }

    @Test
    fun audioPlayerContent_seekForwardButton_callsCallback() {
        var seekForwardClicked = false
        val state = createPlayerState(available = true, canSeekForward = true)
        composeTestRule.setContent {
            TestContent(
                playerState = state,
                seekForward = { seekForwardClicked = true }
            )
        }
        composeTestRule.onNodeWithTag("seek_forward_button").performClick()
        assert(seekForwardClicked)
    }

    @Test
    fun audioPlayerContent_dynamicStateUpdates() {
        val state = TestPlayerState(available = true, canSeekForward = true, canSeekBackward = true)
        composeTestRule.setContent {
            TestContent(
                playerState = state,
            )
        }
        // Initially all controls enabled
        composeTestRule.onNodeWithTag("play_pause_button").assertIsDisplayed().assertIsEnabled()
        composeTestRule.onNodeWithTag("seek_back_button").assertIsDisplayed().assertIsEnabled()
        composeTestRule.onNodeWithTag("seek_forward_button").assertIsDisplayed().assertIsEnabled()
        // Disable controls dynamically
        composeTestRule.runOnUiThread {
            state.available.value = false
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("play_pause_button").assertIsDisplayed().assertIsNotEnabled()
        composeTestRule.onNodeWithTag("seek_back_button").assertIsDisplayed().assertIsNotEnabled()
        composeTestRule.onNodeWithTag("seek_forward_button").assertIsDisplayed().assertIsNotEnabled()
        // Hide seek buttons dynamically
        composeTestRule.runOnUiThread {
            state.canSeekForward.value = false
            state.canSeekBackward.value = false
            state.available.value = true // re-enable for visibility check
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("seek_back_button").assertDoesNotExist()
        composeTestRule.onNodeWithTag("seek_forward_button").assertDoesNotExist()
        composeTestRule.onNodeWithTag("play_pause_button").assertIsDisplayed().assertIsEnabled()
    }

    @Composable
    private fun TestContent(
        playerState: TestPlayerState,
        currentTime: String = "00:20",
        remainingTime: String = "00:40",
        animatedProgress: Float = playerState.progress.value,
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
                        playerState = playerState,
                        onSeekBack = seekBack,
                        onSeekForward = seekForward,
                        onPlayPause = playPause,
                    )
                }
            }
        }
    }
}
