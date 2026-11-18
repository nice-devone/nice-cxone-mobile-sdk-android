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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.core.app.ApplicationProvider
import coil3.ColorImage
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.annotation.DelicateCoilApi
import coil3.test.FakeImageLoaderEngine
import com.github.takahirom.roborazzi.captureRoboImage
import com.nice.cxonechat.ui.composable.generic.ImageViewerDialogCard
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import kotlinx.coroutines.Dispatchers
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.coroutines.EmptyCoroutineContext

/**
 * Verifies the drawn appearance of the full screen image viewer: its header, the divider under it and the
 * background the image sits on.
 *
 * Captures [ImageViewerDialogCard] itself rather than a host built to look like it, so a golden shows the
 * bands the screen really draws. The viewer's own `Surface` is not a separate window, so `onRoot` reaches
 * all of it.
 *
 * The landscape cases are the ones that matter for insets: the header has to observe the same content
 * inset as the rest of the chat rather than sitting against the window edge.
 */
@RunWith(RobolectricTestRunner::class)
@Config(
    sdk = [33],
    qualifiers = "w411dp-h891dp-normal-notlong-notround-notnight-xhdpi-finger"
)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ImageViewerScreenshotTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    /** The viewed image loads through Coil, which would otherwise record whatever the network returned. */
    @OptIn(DelicateCoilApi::class)
    @Before
    fun stubImageLoader() {
        val engine = FakeImageLoaderEngine.Builder().default(ColorImage(Color.CYAN)).build()
        SingletonImageLoader.setUnsafe(
            ImageLoader.Builder(ApplicationProvider.getApplicationContext<Context>())
                .components { add(engine) }
                // Resolve on the caller's thread, so the image is drawn during composition rather than
                // after a dispatcher hop no capture below awaits.
                .coroutineContext(EmptyCoroutineContext)
                .mainCoroutineContext(Dispatchers.Main.immediate)
                .build()
        )
    }

    // The loader is process wide and this module reuses one JVM across test classes, so leaving the stub
    // installed would paint a cyan square into any later test that loads an image.
    @OptIn(DelicateCoilApi::class)
    @After
    fun resetImageLoader() {
        SingletonImageLoader.reset()
    }

    @Test
    fun `image viewer portrait`() {
        captureViewer()
    }

    @Test
    @Config(qualifiers = "w411dp-h891dp-normal-notlong-notround-night-xhdpi-finger")
    fun `image viewer portrait dark`() {
        captureViewer(darkTheme = true)
    }

    @Test
    @Config(qualifiers = "w852dp-h412dp-normal-notlong-notround-notnight-xhdpi-finger")
    fun `image viewer landscape`() {
        captureViewer()
    }

    @Test
    @Config(qualifiers = "w852dp-h412dp-normal-notlong-notround-night-xhdpi-finger")
    fun `image viewer landscape dark`() {
        captureViewer(darkTheme = true)
    }

    /**
     * Right to left, where the back and share actions have to swap ends. Unlike the attachment selection
     * sheet the viewer is drawn in the host's own window, so a provider around it does reach its content.
     */
    @Test
    @Config(qualifiers = "w852dp-h412dp-normal-notlong-notround-notnight-xhdpi-finger")
    fun `image viewer landscape right to left`() {
        captureViewer(layoutDirection = LayoutDirection.Rtl)
    }

    private fun captureViewer(
        darkTheme: Boolean = false,
        layoutDirection: LayoutDirection = LayoutDirection.Ltr,
    ) {
        composeTestRule.setContent {
            ChatTheme(darkTheme = darkTheme) {
                CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                    ImageViewerDialogCard(image = IMAGE, title = TITLE, onDismiss = {}, onShare = {})
                }
            }
        }

        // The image arrives through Coil's asynchronous pipeline even from a stubbed loader, so an
        // unsettled frame would record an empty viewer.
        composeTestRule.waitForIdle()
        // The spinner is composed while the load is outstanding, so its absence is what proves the image
        // is drawn; the image's own node is laid out from the first frame either way.
        assertTrue(
            "The viewer image did not load before capture; refusing to record a spinner.",
            composeTestRule.onAllNodesWithTag("loading", useUnmergedTree = true).fetchSemanticsNodes().isEmpty(),
        )
        composeTestRule.onRoot().captureRoboImage(roborazziOptions = screenshotRoborazziOptions)
    }

    private companion object {
        const val TITLE = "St Michaels Mount, Marazion in Cornwall UK"

        /** Never fetched — the stubbed loader answers every request with the same colour. */
        const val IMAGE = "https://example.com/st-michaels-mount.jpg"
    }
}
