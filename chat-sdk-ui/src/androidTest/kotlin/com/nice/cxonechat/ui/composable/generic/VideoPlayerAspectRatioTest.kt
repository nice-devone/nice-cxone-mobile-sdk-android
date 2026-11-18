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

import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ComposeTimeoutException
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.WindowSize
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.exoplayer.ExoPlayer
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.concurrent.atomic.AtomicReference

/**
 * Proves the full screen video viewer takes its proportions from a clip that was really decoded.
 *
 * [VideoViewLayoutTest][com.nice.cxonechat.ui.composable.generic.VideoViewLayoutTest] pins the same geometry far
 * more cheaply, but it hands the player a video size directly, so it cannot show that a size reaches the layout
 * from a decoder at all. Only a real [ExoPlayer] reading a real file does that, which is what this test is for.
 *
 * Both clips came off a phone, and between them they cover the two ways a fit can fall: one is a recording made
 * with the phone held upright, so the coded frame is landscape and a rotation matrix turns it — the shape the
 * cheap test cannot reach, and the one where the crop the player asks for is documented as possibly not applying,
 * so it is where the defect showed as distortion rather than a crop. The other is plainly landscape, which is
 * what leaves blank above and below rather than at the sides.
 *
 * Each window is overridden rather than taken from the device, because the interesting case is a clip shaped
 * unlike the window it is watched in, and a device left in one orientation matches one of these clips almost by
 * accident.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class VideoPlayerAspectRatioTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun videoFrame_keepsTheDecodedAspectRatioAndIsCentred_whenTheClipIsNarrowerThanTheWindow() {
        playing(PORTRAIT_CLIP, LANDSCAPE_WINDOW) {
            val frame = frameBounds()
            val area = boundsOf(PLAYER_TAG)
            // The clip is taller than this window is, so height is what the fit runs out of first.
            assertEquals("frame height", area.height.value, frame.height.value, DP_TOLERANCE)
            assertEquals(
                "the frame is $frame, which is not the clip's shape",
                area.height.value * PORTRAIT_CLIP.aspectRatio,
                frame.width.value,
                DP_TOLERANCE,
            )
            assertTrue(
                "the frame $frame must leave part of the area $area blank rather than spanning it",
                frame.width < area.width,
            )
            assertEquals(
                "the blank space must be split evenly, but the frame is $frame in $area",
                ((area.width - frame.width) / 2).value,
                frame.left.value,
                DP_TOLERANCE,
            )
        }
    }

    @Test
    fun videoFrame_keepsTheDecodedAspectRatioAndIsCentred_whenTheClipIsShallowerThanTheWindow() {
        playing(LANDSCAPE_CLIP, PORTRAIT_WINDOW) {
            val frame = frameBounds()
            val area = boundsOf(PLAYER_TAG)
            // This clip is wider than the window is, so width is what the fit runs out of first instead.
            assertEquals("frame width", area.width.value, frame.width.value, DP_TOLERANCE)
            assertEquals(
                "the frame is $frame, which is not the clip's shape",
                area.width.value / LANDSCAPE_CLIP.aspectRatio,
                frame.height.value,
                DP_TOLERANCE,
            )
            assertTrue(
                "the frame $frame must leave part of the area $area blank rather than spanning it",
                frame.height < area.height,
            )
            assertEquals(
                "the blank space must be split evenly, but the frame is $frame in $area",
                ((area.height - frame.height) / 2).value,
                frame.top.value,
                DP_TOLERANCE,
            )
        }
    }

    /**
     * Runs [assertions] against the player the production code builds, showing [clip] in a [window] shaped unlike it.
     *
     * [buildProgressivePlayerForUri] prepares the player it returns, and an [ExoPlayer] must be built and driven
     * from a single thread, so both that and the release run on the main one. The player is left paused: the size
     * does not wait on playback, and the controls hide themselves three seconds into it.
     */
    private fun playing(clip: Clip, window: DpSize, assertions: () -> Unit) {
        val uri = copyClipOutOfAssets(clip)
        val player = runBlocking(Dispatchers.Main) {
            buildProgressivePlayerForUri(
                context = InstrumentationRegistry.getInstrumentation().targetContext,
                uri = uri,
            ).apply { playWhenReady = false }
        }
        try {
            showAndAwaitDecodeOf(clip, player, window)
            assertions()
        } finally {
            runBlocking(Dispatchers.Main) { player.release() }
        }
    }

    /**
     * Draws the player and waits for the decoder to report the size [clip] says it presents at.
     *
     * Waiting on the decoder's own report rather than on the frame's shape keeps the assertions honest: were the
     * viewer to stop fitting the clip, this returns anyway and the assertion in the test is what fails. The size
     * is checked here because every test rests on it, and because a wrong one means the asset is not the clip
     * these tests describe.
     */
    private fun showAndAwaitDecodeOf(clip: Clip, player: ExoPlayer, window: DpSize) {
        val reported = AtomicReference(VideoSize.UNKNOWN)
        val failure = AtomicReference<PlaybackException?>(null)
        runBlocking(Dispatchers.Main) {
            player.addListener(object : Player.Listener {
                override fun onVideoSizeChanged(videoSize: VideoSize) = reported.set(videoSize)
                override fun onPlayerError(error: PlaybackException) = failure.set(error)
            })
            // The player was prepared before this point and a new listener is told nothing that has already
            // been delivered, so take the state it holds now. Same main thread, so nothing can arrive between.
            reported.set(player.videoSize)
            failure.set(player.playerError)
        }

        composeTestRule.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.WindowSize(window)) {
                ChatTheme {
                    Box(modifier = Modifier.fillMaxSize()) {
                        VideoPlayerContent(player = player, modifier = Modifier.testTag(PLAYER_TAG))
                    }
                }
            }
        }

        // A device that cannot decode the clip reports no size and no error either, so it would otherwise
        // surface as a bare timeout. Naming the codec is what tells that apart from a broken viewer.
        try {
            composeTestRule.waitUntil(DECODE_TIMEOUT_MS) {
                reported.get().height > 0 || failure.get() != null
            }
        } catch (logged: ComposeTimeoutException) {
            throw AssertionError("no size and no error arrived for $clip; check logcat for NoSupport", logged)
        }
        failure.get()?.let { throw AssertionError("this device could not play $clip", it) }

        val decoded = reported.get()
        assertEquals("$clip decoded as $decoded", clip.width, decoded.width)
        assertEquals("$clip decoded as $decoded", clip.height, decoded.height)

        composeTestRule.waitForIdle()
    }

    /**
     * Copies [clip] to a file the player can open.
     *
     * The clips are packaged into the test APK, so they are read from the instrumentation context — the target
     * context's assets are the SDK's own, and hold nothing of the sort.
     */
    private fun copyClipOutOfAssets(clip: Clip): Uri {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val file = File(instrumentation.targetContext.cacheDir, clip.asset)
        instrumentation.context.assets.open(clip.asset).use { asset ->
            file.outputStream().use(asset::copyTo)
        }
        return Uri.fromFile(file)
    }

    // The frame and the area holding it both sit under the player's tap-to-show-controls handler, which merges
    // its descendants, so neither is a node of its own in the merged tree.
    private fun frameBounds(): DpRect = boundsOf(FRAME_TAG)

    private fun boundsOf(tag: String): DpRect =
        composeTestRule.onNodeWithTag(tag, useUnmergedTree = true).getUnclippedBoundsInRoot()

    /** A clip in the test assets, at the size it presents once any rotation matrix it carries is applied. */
    private data class Clip(val asset: String, val width: Int, val height: Int) {
        val aspectRatio: Float = width.toFloat() / height

        override fun toString(): String = asset
    }

    private companion object {
        /**
         * 1920x1080 as coded, turned by a -90 degree rotation matrix, so it presents portrait in a landscape frame.
         *
         * H.264 and BT.709 rather than the HEVC and HLG the phone recorded, because an emulator's
         * `c2.goldfish.hevc.decoder` reports an HDR clip as supported and then fails once asked to render it.
         *
         * ```
         * ffmpeg -noautorotate -t 2 -i input_portrait_clip.mp4 -c:v libx264 -profile:v high -crf 28 \
         *     -vf colorspace=iall=bt2020:all=bt709:format=yuv420p -c:a aac -b:a 64k portrait_clip.mp4
         * ```
         */
        val PORTRAIT_CLIP = Clip(asset = "portrait_clip.mp4", width = 1080, height = 1920)

        /**
         * The counterpart: 1440x1080 with no rotation matrix, so it is landscape both as coded and as shown.
         *
         * ```
         * ffmpeg -t 2 -i input_landscape_clip.mp4 -c:v libx264 \
         *     -profile:v high -level 4.0 -pix_fmt yuv420p -r 15 -crf 32 -an landscape_clip.mp4
         * ```
         */
        val LANDSCAPE_CLIP = Clip(asset = "landscape_clip.mp4", width = 1440, height = 1080)

        /** A wider ratio than the portrait clip's, so the fit leaves a bar at each side. */
        val LANDSCAPE_WINDOW = DpSize(852.dp, 412.dp)

        /** The same window turned upright, so the narrower ratio, and the bars fall above and below instead. */
        val PORTRAIT_WINDOW = DpSize(412.dp, 852.dp)

        /** Generous: a first decode competes with app start-up on whatever device this runs on. */
        const val DECODE_TIMEOUT_MS = 15_000L

        /** Bounds are rounded to whole pixels before they are reported in dp. */
        const val DP_TOLERANCE = 0.5f

        const val PLAYER_TAG = "video_player_content"
        const val FRAME_TAG = "video_frame"
    }
}
