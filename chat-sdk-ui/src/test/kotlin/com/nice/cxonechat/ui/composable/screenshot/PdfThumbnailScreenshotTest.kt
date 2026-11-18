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

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
import com.nice.cxonechat.ui.composable.conversation.PreviewAttachments
import com.nice.cxonechat.ui.composable.generic.PdfThumbnail
import com.nice.cxonechat.ui.composable.generic.ThumbnailSize
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import com.nice.cxonechat.ui.util.LocalPdfPageSource
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The two states a PDF attachment is drawn in on its own: its first page, and the file tile standing in until that
 * page renders.
 *
 * The tile is asked for at the picker's size, the one a thumbnail drawn outside a message takes. In a message it is
 * the smaller tile that fills the slot it is given and takes the sender's colours, which
 * [PdfAttachmentMessageScreenshotTest] captures with the bubble behind it.
 *
 * The page is stubbed, not decoded: `android.graphics.pdf.PdfRenderer` is a native with no Robolectric shadow,
 * and a golden of a real document would pin a decoder rather than the SDK. A flat colour is enough, because what
 * these check is what the SDK draws around the page. Green, so a stubbed page is never taken for the image
 * loader's cyan.
 *
 * The rendered page is captured in one theme, being the document's own pixels; the tile in both, being drawn in
 * theme tokens.
 */
@RunWith(RobolectricTestRunner::class)
@Config(
    sdk = [33],
    qualifiers = "w411dp-h891dp-normal-notlong-notround-notnight-xhdpi-finger"
)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PdfThumbnailScreenshotTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `pdf thumbnail with a rendered first page`() {
        capturePdfThumbnail(renderedPage = stubbedPdfPage())
    }

    @Test
    fun `pdf thumbnail before its first page renders`() {
        capturePdfThumbnail(renderedPage = null)
    }

    @Test
    @Config(qualifiers = DARK_QUALIFIERS)
    fun `pdf thumbnail before its first page renders dark`() {
        capturePdfThumbnail(renderedPage = null, darkTheme = true)
    }

    private fun capturePdfThumbnail(
        renderedPage: Bitmap?,
        darkTheme: Boolean = false,
    ) {
        // Built once, outside the composition: the thumbnail animates between documents, so handing it a fresh
        // instance on a recomposition would restart that animation instead of settling.
        val document = FakePdfPageSource(renderedPage = renderedPage)
        composeTestRule.setContent {
            CompositionLocalProvider(LocalPdfPageSource provides document) {
                ChatTheme(darkTheme = darkTheme) {
                    Surface {
                        Box(modifier = Modifier.padding(ChatTheme.space.large)) {
                            PdfThumbnail(
                                attachment = PreviewAttachments.pdf,
                                modifier = Modifier.width(THUMBNAIL_WIDTH),
                                isGroupAttachment = false,
                                showFrame = {},
                                thumbnailSize = ThumbnailSize.LARGE,
                            )
                        }
                    }
                }
            }
        }

        awaitSettled(pageRendered = renderedPage != null)
        composeTestRule.onRoot().captureRoboImage(roborazziOptions = screenshotRoborazziOptions)
    }

    /**
     * Waits until the thumbnail has settled on what it draws.
     *
     * The stand-in tile resolves its extension off the main thread and is composed showing "N/A" first, so
     * idleness alone would bake that placeholder into a golden — the placeholder frame is idle in its own right.
     * A rendered page draws no tile and so no label, and draws as soon as the document is open.
     *
     * The spinner assert catches a stub that never arrived: the thumbnail would fall back to opening the
     * attachment, find no Koin graph outside `ChatActivity`, and wait forever.
     */
    private fun awaitSettled(pageRendered: Boolean) {
        if (!pageRendered) {
            composeTestRule.waitUntil(timeoutMillis = SETTLE_TIMEOUT_MILLIS) {
                composeTestRule.onAllNodesWithText(PDF_EXTENSION_LABEL).fetchSemanticsNodes().isNotEmpty()
            }
        }
        composeTestRule.waitForIdle()
        assertTrue(
            "The thumbnail is still showing a spinner; refusing to record the wrong state.",
            composeTestRule.onAllNodesWithTag("loading", useUnmergedTree = true).fetchSemanticsNodes().isEmpty(),
        )
    }

    private companion object {
        /** Fixed here rather than measured from the window, so the goldens isolate the thumbnail. */
        val THUMBNAIL_WIDTH: Dp = 200.dp

        const val DARK_QUALIFIERS = "w411dp-h891dp-normal-notlong-notround-night-xhdpi-finger"

        /** Extension the fixture's document resolves to, drawn on the tile standing in for its page. */
        const val PDF_EXTENSION_LABEL = "PDF"
    }
}
