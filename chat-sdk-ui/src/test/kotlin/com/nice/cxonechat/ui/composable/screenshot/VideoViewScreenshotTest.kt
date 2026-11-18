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

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.LayoutDirection
import com.github.takahirom.roborazzi.captureRoboImage
import com.nice.cxonechat.ui.composable.conversation.PreviewAttachments
import com.nice.cxonechat.ui.composable.generic.VideoView
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Verifies the drawn appearance of the full screen video viewer: its header, the actions in it, the
 * background the video sits on and the area the video is given.
 *
 * The golden covers the viewer's chrome rather than a decoded frame. Nothing decodes video under
 * Robolectric, so these run with [LocalInspectionMode] on and the player draws its placeholder in place of a
 * surface. That switch reaches the whole tree, not just the player: anything else in the viewer that grows an
 * inspection branch draws that branch here too, and the golden then moves for a reason that has nothing to do
 * with layout. The chrome is what the wide screen work changes, so that is the coverage worth having.
 *
 * The landscape cases are the ones that matter for insets: the header has to observe the same content inset
 * as the rest of the chat rather than sitting against the window edge, and the video should not stretch the
 * full width of a wide window.
 */
@RunWith(RobolectricTestRunner::class)
@Config(
    sdk = [33],
    qualifiers = "w411dp-h891dp-normal-notlong-notround-notnight-xhdpi-finger"
)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class VideoViewScreenshotTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `video viewer portrait`() {
        captureViewer()
    }

    @Test
    @Config(qualifiers = "w411dp-h891dp-normal-notlong-notround-night-xhdpi-finger")
    fun `video viewer portrait dark`() {
        captureViewer(darkTheme = true)
    }

    @Test
    @Config(qualifiers = "w852dp-h412dp-normal-notlong-notround-notnight-xhdpi-finger")
    fun `video viewer landscape`() {
        captureViewer()
    }

    @Test
    @Config(qualifiers = "w852dp-h412dp-normal-notlong-notround-night-xhdpi-finger")
    fun `video viewer landscape dark`() {
        captureViewer(darkTheme = true)
    }

    /** Right to left, where the back and share actions have to swap ends. */
    @Test
    @Config(qualifiers = "w852dp-h412dp-normal-notlong-notround-notnight-xhdpi-finger")
    fun `video viewer landscape right to left`() {
        captureViewer(layoutDirection = LayoutDirection.Rtl)
    }

    private fun captureViewer(
        darkTheme: Boolean = false,
        layoutDirection: LayoutDirection = LayoutDirection.Ltr,
    ) {
        composeTestRule.setContent {
            ChatTheme(darkTheme = darkTheme) {
                CompositionLocalProvider(
                    LocalLayoutDirection provides layoutDirection,
                    LocalInspectionMode provides true,
                ) {
                    VideoView(uri = VIDEO_URI, title = TITLE, onDismiss = {}, onShare = {})
                }
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onRoot().captureRoboImage(roborazziOptions = screenshotRoborazziOptions)
    }

    private companion object {
        val TITLE: String = PreviewAttachments.movie.friendlyName

        /** Never fetched — the player is replaced by its placeholder before it is built. */
        val VIDEO_URI: String = PreviewAttachments.movie.url
    }
}
