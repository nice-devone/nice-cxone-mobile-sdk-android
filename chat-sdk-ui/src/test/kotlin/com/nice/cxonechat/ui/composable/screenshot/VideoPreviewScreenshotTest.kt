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
import android.graphics.Color
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import coil3.ColorImage
import coil3.ImageLoader
import coil3.annotation.ExperimentalCoilApi
import coil3.test.FakeImageLoaderEngine
import com.github.takahirom.roborazzi.captureRoboImage
import com.nice.cxonechat.ui.composable.conversation.PreviewAttachments
import com.nice.cxonechat.ui.composable.conversation.attachments.PlayIndicator
import com.nice.cxonechat.ui.composable.conversation.attachments.VideoPreview
import com.nice.cxonechat.ui.composable.generic.ThumbnailSize
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.util.UUID
import kotlin.coroutines.EmptyCoroutineContext

/**
 * Verifies the video thumbnail at each size it is drawn at, with and without its play icon.
 *
 * The thumbnail's own loader decodes a frame out of the video, which nothing under Robolectric can do, so
 * these inject a stubbed loader that answers with a flat colour. That leaves the thumbnail's size, its frame
 * and the play icon over it — which is what these goldens are for.
 *
 * `HIDDEN` is drawn because a blurred preview relies on it: the play icon has to be absent there rather than
 * covering the blur.
 */
@RunWith(RobolectricTestRunner::class)
@Config(
    sdk = [33],
    qualifiers = "w411dp-h891dp-normal-notlong-notround-notnight-xhdpi-finger"
)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class VideoPreviewScreenshotTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    /**
     * Answers every request with the same colour, so no frame is ever decoded.
     *
     * Resolves on the caller's thread, so the thumbnail is drawn during composition rather than after a
     * dispatcher hop no capture below awaits. Built once, so a recomposition does not replace the loader.
     */
    @OptIn(ExperimentalCoilApi::class)
    private val stubbedImageLoader: ImageLoader by lazy {
        val engine = FakeImageLoaderEngine.Builder().default(ColorImage(Color.CYAN)).build()
        ImageLoader.Builder(ApplicationProvider.getApplicationContext<Context>())
            .components { add(engine) }
            .coroutineContext(EmptyCoroutineContext)
            .mainCoroutineContext(Dispatchers.Main.immediate)
            .build()
    }

    @Test
    fun `video thumbnail large`() {
        capturePreview(thumbnailSize = ThumbnailSize.LARGE)
    }

    @Test
    @Config(qualifiers = "w411dp-h891dp-normal-notlong-notround-night-xhdpi-finger")
    fun `video thumbnail large dark`() {
        capturePreview(thumbnailSize = ThumbnailSize.LARGE, darkTheme = true)
    }

    /** The play icon is hidden for a blurred preview, so the blur is not covered by it. */
    @Test
    fun `video thumbnail large without play icon`() {
        capturePreview(thumbnailSize = ThumbnailSize.LARGE, playIndicator = PlayIndicator.HIDDEN)
    }

    /** The size used in the conversation itself. */
    @Test
    fun `video thumbnail regular`() {
        capturePreview(thumbnailSize = ThumbnailSize.REGULAR)
    }

    /** The size used by the staged attachment row above the input bar. */
    @Test
    fun `video thumbnail small`() {
        capturePreview(thumbnailSize = ThumbnailSize.SMALL)
    }

    /**
     * The size the staged attachment row shrinks to while the keyboard covers a short window, which fills the tile it is
     * given: an eighth of the window's shorter edge.
     */
    @Test
    @Config(qualifiers = "w852dp-h412dp-normal-notlong-notround-notnight-xhdpi-finger")
    fun `video thumbnail compact`() {
        capturePreview(thumbnailSize = ThumbnailSize.COMPACT, modifier = Modifier.size(51.5.dp))
    }

    private fun capturePreview(
        thumbnailSize: ThumbnailSize,
        playIndicator: PlayIndicator = PlayIndicator.STANDARD,
        darkTheme: Boolean = false,
        modifier: Modifier = Modifier,
    ) {
        composeTestRule.setContent {
            ChatTheme(darkTheme = darkTheme) {
                Surface {
                    Box(modifier = Modifier.padding(ChatTheme.space.large).then(modifier)) {
                        VideoPreview(
                            attachment = PreviewAttachments.movie,
                            messageId = MESSAGE_ID,
                            playIndicator = playIndicator,
                            isGroupAttachment = false,
                            imageLoader = stubbedImageLoader,
                            thumbnailSize = thumbnailSize,
                        )
                    }
                }
            }
        }

        // The thumbnail arrives through Coil's asynchronous pipeline even from a stubbed loader, and the play
        // icon only fades in once it has, so an unsettled frame would record a spinner.
        composeTestRule.waitForIdle()
        assertTrue(
            "The thumbnail did not load before capture; refusing to record a spinner.",
            composeTestRule.onAllNodesWithTag("loading", useUnmergedTree = true).fetchSemanticsNodes().isEmpty(),
        )
        composeTestRule.onRoot().captureRoboImage(roborazziOptions = screenshotRoborazziOptions)
    }

    private companion object {
        /** Fixed, because it is part of the thumbnail's cache key. */
        val MESSAGE_ID: UUID = UUID.fromString("00000000-0000-0000-0000-000000000001")
    }
}
