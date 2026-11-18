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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.LayoutDirection
import androidx.media3.common.Player
import androidx.media3.common.SimpleBasePlayer.MediaItemData
import androidx.media3.common.VideoSize
import androidx.media3.test.utils.FakePlayer
import com.github.takahirom.roborazzi.captureRoboImage
import com.nice.cxonechat.ui.composable.generic.VideoPlayerContent
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Verifies the playback controls drawn over a video: the scrim behind them, the inset their content observes
 * and the arrangement of the progress row.
 *
 * Driven by a [FakePlayer] rather than by fixed strings, so the elapsed and remaining labels come from the
 * same position and duration the screen would read off a real player. The duration is set explicitly because
 * an unset one is reported as `C.TIME_UNSET`, which the screen renders as `NaN`.
 *
 * The player is left paused on purpose, for two reasons. The controls hide themselves after three seconds of
 * playback, so a playing capture would race that timeout; and the position poll loops on a delay while
 * playing, which would never let the test clock idle. Paused is also the state in which the controls are
 * guaranteed to be on screen.
 */
@RunWith(RobolectricTestRunner::class)
@Config(
    sdk = [33],
    qualifiers = "w411dp-h891dp-normal-notlong-notround-notnight-xhdpi-finger"
)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class VideoPlayerScreenshotTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `video player paused portrait`() {
        capturePlayer()
    }

    @Test
    @Config(qualifiers = LANDSCAPE_QUALIFIERS)
    fun `video player paused landscape`() {
        capturePlayer()
    }

    /** Right to left, where the elapsed and remaining timestamps have to swap ends. */
    @Test
    @Config(qualifiers = LANDSCAPE_QUALIFIERS)
    fun `video player paused landscape right to left`() {
        capturePlayer(layoutDirection = LayoutDirection.Rtl)
    }

    /**
     * A clip taller than it is wide, in a window shaped the other way, so the frame is fitted rather than filled.
     *
     * The frame draws nothing, since nothing decodes here, which leaves the controls as the only visible record
     * of the area it was given — they span the fitted frame instead of the window. `VideoViewLayoutTest`
     * measures that area; this case is what it looks like.
     */
    @Test
    @Config(qualifiers = LANDSCAPE_QUALIFIERS)
    fun `video player paused portrait clip in a landscape window`() {
        capturePlayer(videoSize = PORTRAIT_CLIP)
    }

    private fun capturePlayer(
        layoutDirection: LayoutDirection = LayoutDirection.Ltr,
        videoSize: VideoSize? = null,
    ) {
        val player = FakePlayer(
            playbackState = Player.STATE_READY,
            playWhenReady = false,
            playlist = listOf(MediaItemData.Builder(ITEM_UID).build()),
        ).apply {
            setDuration(ITEM_UID, DURATION_MS)
            setPosition(POSITION_MS)
            if (videoSize != null) setVideoSize(videoSize)
        }

        composeTestRule.setContent {
            ChatTheme {
                CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        VideoPlayerContent(player = player, modifier = Modifier.fillMaxSize())
                    }
                }
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onRoot().captureRoboImage(roborazziOptions = screenshotRoborazziOptions)
    }

    private companion object {
        const val ITEM_UID = "video"

        const val LANDSCAPE_QUALIFIERS = "w852dp-h412dp-normal-notlong-notround-notnight-xhdpi-finger"

        /** The proportions a phone camera records in held upright. */
        val PORTRAIT_CLIP = VideoSize(540, 960)

        /** Two minutes in, twelve seconds played, so the labels read 00:12 and 01:48. */
        const val DURATION_MS = 120_000L
        const val POSITION_MS = 12_000L
    }
}
