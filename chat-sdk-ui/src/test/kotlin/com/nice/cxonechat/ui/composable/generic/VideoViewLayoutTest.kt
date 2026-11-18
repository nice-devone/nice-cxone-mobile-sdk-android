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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import androidx.media3.common.Player
import androidx.media3.common.SimpleBasePlayer.MediaItemData
import androidx.media3.common.VideoSize
import androidx.media3.test.utils.FakePlayer
import com.nice.cxonechat.ui.composable.conversation.PreviewAttachments
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Pins where the full screen video viewer places its header actions, and the area and shape it gives the video.
 *
 * Bounds rather than pixels, because the viewer's goldens cannot settle this. Nothing decodes video under
 * Robolectric, so a captured viewer draws the player's placeholder, and that placeholder fills whatever it is
 * given — it looks the same whether the video area spans the window or is capped. Measurement is visible to a
 * bounds assertion even when the drawn result is not, and Robolectric does run measurement and layout.
 *
 * `VideoViewScreenshotTest` covers the appearance these numbers produce.
 */
@RunWith(RobolectricTestRunner::class)
@Config(
    sdk = [33],
    qualifiers = "w411dp-h891dp-normal-notlong-notround-notnight-xhdpi-finger"
)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
internal class VideoViewLayoutTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    /**
     * The header's actions line up with the conversation's content column instead of the window edge.
     *
     * Asserted as a distance from the bar's own edge rather than an absolute position, so it holds
     * regardless of the padding Material 3 puts around a navigation icon.
     */
    @Test
    @Config(qualifiers = WIDE_QUALIFIERS)
    fun `a wide window insets the header actions to the content column`() {
        showViewer()

        val bar = barBounds()

        assertTrue(
            "the back button sits inside the wide-screen content inset",
            backButtonBounds().left - bar.left >= WIDE_INSET,
        )
        assertTrue(
            "the share button sits inside the wide-screen content inset",
            bar.right - shareButtonBounds().right >= WIDE_INSET,
        )
    }

    /**
     * The inset is applied to the bar's content, not to the bar, so its background still spans the window.
     *
     * A `Modifier.padding` on the bar would shrink the background with it and leave an unpainted strip down
     * each side of the header.
     */
    @Test
    @Config(qualifiers = WIDE_QUALIFIERS)
    fun `the header background spans a wide window`() {
        showViewer()

        assertEquals(windowBounds().width.value, barBounds().width.value, TOLERANCE)
    }

    /** The inset belongs to wide windows only: a phone held upright has no width to give away. */
    @Test
    fun `a compact window leaves the header actions at the bar's own inset`() {
        showViewer()

        val bar = barBounds()

        assertTrue(
            "the compact header must not pay the wide-screen inset",
            backButtonBounds().left - bar.left < WIDE_INSET,
        )
    }

    /** Stretched across a wide window the video would be unwatchable, so it keeps to a centred column. */
    @Test
    @Config(qualifiers = WIDE_QUALIFIERS)
    fun `a wide window caps the video and centres it`() {
        showViewer()

        val window = windowBounds()
        val media = mediaBounds()

        assertEquals(MEDIA_MAX_WIDTH.value, media.width.value, TOLERANCE)
        assertEquals(((window.width - MEDIA_MAX_WIDTH) / 2).value, media.left.value, TOLERANCE)
    }

    /** The cap is above any compact window's width, so it must not narrow the video on a phone. */
    @Test
    fun `a compact window gives the video its full width`() {
        showViewer()

        assertEquals(windowBounds().width.value, mediaBounds().width.value, TOLERANCE)
    }

    /**
     * The video area takes the space the viewer leaves it without being told to fill it.
     *
     * The viewer relies on this: it caps the player's width and otherwise lets it size itself. Measured on a
     * real [Player] rather than the placeholder, since the placeholder fills its space on its own and so would
     * report the same bounds either way.
     */
    @Test
    @Config(qualifiers = WIDE_QUALIFIERS)
    fun `the player fills the space it is given without a size modifier`() {
        showPlayer(readyPlayer())

        val window = windowBounds()
        val surface = playerBounds()

        assertEquals(window.width.value, surface.width.value, TOLERANCE)
        assertEquals(window.height.value, surface.height.value, TOLERANCE)
    }

    /**
     * A clip is drawn in the proportions it was recorded in, whatever shape the area it is given happens to be.
     *
     * A `SurfaceView` scales its buffer to its own bounds, so those bounds are the only thing that can hold the
     * clip's proportions, and a surface handed the whole area draws a portrait clip in the area's shape. Only
     * the ratio of the source size matters here, so the units it is given in do not.
     */
    @Test
    @Config(qualifiers = WIDE_QUALIFIERS)
    fun `a portrait clip keeps its own proportions instead of filling the viewer`() {
        showPlayer(readyPlayer(videoSize = PORTRAIT_CLIP))

        val area = windowBounds()
        val frame = frameBounds()

        // The clip is taller than the area is, so height is what the fit runs out of first.
        assertEquals(area.height.value, frame.height.value, TOLERANCE)
        assertEquals(area.height.value * PORTRAIT_CLIP_ASPECT_RATIO, frame.width.value, TOLERANCE)
        assertEquals(((area.width - frame.width) / 2).value, frame.left.value, TOLERANCE)
        // Only the frame narrows. Were the container to shrink onto it, the bars would fall outside the
        // tap-to-show-controls handler, and the centring asserted above would be measured against nothing.
        assertEquals(area.width.value, playerBounds().width.value, TOLERANCE)
    }

    /**
     * The controls span the frame the user is watching, not the bars it is letterboxed into.
     *
     * The final assertion is what gives the other two their meaning: matching the frame is no evidence while
     * the frame still fills its container, which is the state this whole class exists to rule out.
     */
    @Test
    @Config(qualifiers = WIDE_QUALIFIERS)
    fun `the playback controls follow the video frame rather than its container`() {
        showPlayer(readyPlayer(videoSize = PORTRAIT_CLIP))

        val frame = frameBounds()
        val controls = controlsBounds()

        assertEquals(frame.width.value, controls.width.value, TOLERANCE)
        assertEquals(frame.left.value, controls.left.value, TOLERANCE)
        assertTrue(
            "the controls must not span the area the frame is letterboxed into",
            controls.width < windowBounds().width,
        )
    }

    /**
     * Draws the viewer with the player replaced by its placeholder, since a real one decodes nothing here.
     *
     * The placeholder stands in for the video's own content only. Everything these tests measure — the
     * header's insets and the area the video is given — is decided by the viewer around it.
     */
    private fun showViewer() {
        composeTestRule.setContent {
            ChatTheme {
                CompositionLocalProvider(LocalInspectionMode provides true) {
                    VideoView(
                        uri = PreviewAttachments.movie.url,
                        title = PreviewAttachments.movie.friendlyName,
                        onDismiss = {},
                        onShare = {},
                    )
                }
            }
        }
        composeTestRule.waitForIdle()
    }

    /** Draws [VideoPlayerContent] alone, filling the window, so the area the video is fitted into is known. */
    private fun showPlayer(player: Player) {
        composeTestRule.setContent {
            ChatTheme {
                Box(modifier = Modifier.fillMaxSize()) {
                    VideoPlayerContent(player = player, modifier = Modifier.testTag(PLAYER_TAG))
                }
            }
        }
        composeTestRule.waitForIdle()
    }

    /**
     * A paused player holding one item, reporting [videoSize] as its source size when one is given.
     *
     * Paused because the controls hide themselves after three seconds of playback, and given a position and a
     * duration because the progress row reads both.
     */
    private fun readyPlayer(videoSize: VideoSize? = null): FakePlayer = FakePlayer(
        playbackState = Player.STATE_READY,
        playWhenReady = false,
        playlist = listOf(MediaItemData.Builder(ITEM_UID).build()),
    ).apply {
        setDuration(ITEM_UID, CLIP_DURATION_MS)
        setPosition(CLIP_POSITION_MS)
        if (videoSize != null) setVideoSize(videoSize)
    }

    private fun windowBounds(): DpRect = composeTestRule.onRoot().getUnclippedBoundsInRoot()

    private fun barBounds(): DpRect =
        composeTestRule.onNodeWithTag("chat_top_bar").getUnclippedBoundsInRoot()

    private fun backButtonBounds(): DpRect =
        composeTestRule.onNodeWithTag("back_button").getUnclippedBoundsInRoot()

    private fun shareButtonBounds(): DpRect =
        composeTestRule.onNodeWithTag("share_button").getUnclippedBoundsInRoot()

    private fun mediaBounds(): DpRect =
        composeTestRule.onNodeWithTag("video_player").getUnclippedBoundsInRoot()

    private fun playerBounds(): DpRect =
        composeTestRule.onNodeWithTag(PLAYER_TAG).getUnclippedBoundsInRoot()

    // Both sit under the player's tap-to-show-controls handler, which merges its descendants, so neither is a
    // node of its own in the merged tree.
    private fun frameBounds(): DpRect =
        composeTestRule.onNodeWithTag("video_frame", useUnmergedTree = true).getUnclippedBoundsInRoot()

    private fun controlsBounds(): DpRect =
        composeTestRule.onNodeWithTag("video_controls", useUnmergedTree = true).getUnclippedBoundsInRoot()

    private companion object {
        /** Landscape, and wide enough for the cap to bite. */
        const val WIDE_QUALIFIERS = "w852dp-h412dp-normal-notlong-notround-notnight-xhdpi-finger"

        const val PLAYER_TAG = "player_under_test"
        const val ITEM_UID = "video"

        const val CLIP_DURATION_MS = 120_000L
        const val CLIP_POSITION_MS = 12_000L

        /** The proportions a phone camera records in held upright, and the shape the viewer least expects. */
        val PORTRAIT_CLIP = VideoSize(540, 960)
        val PORTRAIT_CLIP_ASPECT_RATIO = PORTRAIT_CLIP.width.toFloat() / PORTRAIT_CLIP.height

        val WIDE_INSET = 64.dp
        val MEDIA_MAX_WIDTH = 640.dp
        const val TOLERANCE = 0.5f
    }
}
