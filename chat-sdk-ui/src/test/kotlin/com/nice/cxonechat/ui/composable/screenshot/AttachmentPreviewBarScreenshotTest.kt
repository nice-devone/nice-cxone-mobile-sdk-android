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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import coil3.ColorImage
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.annotation.DelicateCoilApi
import coil3.test.FakeImageLoaderEngine
import com.github.takahirom.roborazzi.captureRoboImage
import com.nice.cxonechat.message.Attachment
import com.nice.cxonechat.ui.composable.conversation.AttachmentPreviewBar
import com.nice.cxonechat.ui.composable.conversation.PreviewAttachments
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import com.nice.cxonechat.ui.util.LocalPdfPageSource
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Verifies the drawn appearance of [AttachmentPreviewBar]: the scrollbar's strip below the attachments, the
 * fade over the trailing edge, and the wide-screen content inset.
 *
 * The expanded qualifier is the one `ChatConversationScreenshotTest` uses, so the two agree on what a wide
 * window is.
 */
@RunWith(RobolectricTestRunner::class)
@Config(
    sdk = [33],
    qualifiers = "w411dp-h891dp-normal-notlong-notround-notnight-xhdpi-finger"
)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AttachmentPreviewBarScreenshotTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    /** Attachment thumbnails load through Coil, which would otherwise record whatever the network returned. */
    @OptIn(DelicateCoilApi::class)
    @Before
    fun stubImageLoader() {
        val engine = FakeImageLoaderEngine.Builder().default(ColorImage(Color.CYAN)).build()
        SingletonImageLoader.setUnsafe(
            ImageLoader.Builder(ApplicationProvider.getApplicationContext<Context>())
                .components { add(engine) }
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
    fun `attachment preview bar compact light`() {
        captureBar()
    }

    @Test
    @Config(qualifiers = "w411dp-h891dp-normal-notlong-notround-night-xhdpi-finger")
    fun `attachment preview bar compact dark`() {
        captureBar(darkTheme = true)
    }

    @Test
    @Config(qualifiers = "w852dp-h412dp-normal-notlong-notround-notnight-xhdpi-finger")
    fun `attachment preview bar expanded light`() {
        captureBar(horizontalPadding = WIDE_INSET)
    }

    @Test
    @Config(qualifiers = "w852dp-h412dp-normal-notlong-notround-night-xhdpi-finger")
    fun `attachment preview bar expanded dark`() {
        captureBar(horizontalPadding = WIDE_INSET, darkTheme = true)
    }

    /**
     * The fade has to mirror rather than shift: its box follows the layout direction while the gradient it
     * paints does not, so the opaque end has to be reversed to stay against the trailing edge.
     */
    @Test
    @Config(qualifiers = "w852dp-h412dp-normal-notlong-notround-notnight-xhdpi-finger")
    fun `attachment preview bar expanded right to left`() {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                ChatTheme {
                    BarUnderTest(horizontalPadding = WIDE_INSET)
                }
            }
        }
        capture()
    }

    /** Scrolled to the end, where the trailing inset becomes visible and the last remove icon must stay whole. */
    @Test
    fun `attachment preview bar compact scrolled to end`() {
        composeTestRule.setContent {
            ChatTheme {
                BarUnderTest()
            }
        }
        composeTestRule.onNodeWithTag("attachment_preview_row").performScrollToIndex(attachments.lastIndex)
        capture()
    }

    /**
     * The wide inset once the row has moved, which is the only place its scrolled behaviour shows.
     *
     * Because the inset is content padding rather than a layout inset, the row clips at the window edge
     * instead of at the inset: attachments travel through the whole leading gutter, and the trailing inset
     * only appears at the end. Both are visible here, so switching to a layout inset fails this golden
     * instead of quietly moving where content clips.
     */
    @Test
    @Config(qualifiers = "w852dp-h412dp-normal-notlong-notround-notnight-xhdpi-finger")
    fun `attachment preview bar expanded scrolled to end`() {
        composeTestRule.setContent {
            ChatTheme {
                BarUnderTest(horizontalPadding = WIDE_INSET)
            }
        }
        composeTestRule.onNodeWithTag("attachment_preview_row").performScrollToIndex(attachments.lastIndex)
        capture()
    }

    @Test
    @Config(qualifiers = "w852dp-h412dp-normal-notlong-notround-notnight-xhdpi-finger")
    fun `attachment preview bar documents expanded`() {
        captureDocuments(horizontalPadding = WIDE_INSET)
    }

    @Test
    @Config(qualifiers = "w852dp-h412dp-normal-notlong-notround-night-xhdpi-finger")
    fun `attachment preview bar documents expanded dark`() {
        captureDocuments(horizontalPadding = WIDE_INSET, darkTheme = true)
    }

    /** The landscape phone window with the keyboard up, the one window the bar is compact in. */
    @Test
    @Config(qualifiers = "w852dp-h412dp-normal-notlong-notround-notnight-xhdpi-finger")
    fun `attachment preview bar documents keyboard open`() {
        captureDocuments(horizontalPadding = WIDE_INSET, compact = true)
    }

    @Test
    @Config(qualifiers = "w852dp-h412dp-normal-notlong-notround-night-xhdpi-finger")
    fun `attachment preview bar documents keyboard open dark`() {
        captureDocuments(horizontalPadding = WIDE_INSET, compact = true, darkTheme = true)
    }

    /** The remove icons sit on the other corner, and the fade on the other edge. */
    @Test
    @Config(qualifiers = "w852dp-h412dp-normal-notlong-notround-notnight-xhdpi-finger")
    fun `attachment preview bar documents keyboard open right to left`() {
        captureDocuments(horizontalPadding = WIDE_INSET, compact = true, layoutDirection = LayoutDirection.Rtl)
    }

    /** Scrolled to the end, where the last remove icon must stay whole at the smaller size too. */
    @Test
    @Config(qualifiers = "w852dp-h412dp-normal-notlong-notround-notnight-xhdpi-finger")
    fun `attachment preview bar keyboard open scrolled to end`() {
        composeTestRule.setContent {
            ChatTheme {
                BarUnderTest(horizontalPadding = WIDE_INSET, compact = true)
            }
        }
        composeTestRule.onNodeWithTag("attachment_preview_row").performScrollToIndex(attachments.lastIndex)
        capture()
    }

    private fun captureBar(horizontalPadding: Dp = 0.dp, darkTheme: Boolean = false) {
        composeTestRule.setContent {
            ChatTheme(darkTheme = darkTheme) {
                BarUnderTest(horizontalPadding = horizontalPadding)
            }
        }
        capture()
    }

    /**
     * Captures a bar holding one attachment of each kind that draws a different tile, the document tiles among
     * them.
     *
     * Each document tile resolves its extension off the main thread and is composed with a stand-in first, so
     * this waits for every resolved label rather than for idle.
     */
    private fun captureDocuments(
        horizontalPadding: Dp = 0.dp,
        darkTheme: Boolean = false,
        compact: Boolean = false,
        layoutDirection: LayoutDirection = LayoutDirection.Ltr,
    ) {
        // Built outside the composition: a fresh instance per recomposition restarts the thumbnail's animation.
        val document = FakePdfPageSource(renderedPage = stubbedPdfPage())
        composeTestRule.setContent {
            CompositionLocalProvider(
                LocalPdfPageSource provides document,
                LocalLayoutDirection provides layoutDirection,
            ) {
                ChatTheme(darkTheme = darkTheme) {
                    BarUnderTest(horizontalPadding = horizontalPadding, attachments = documents, compact = compact)
                }
            }
        }
        composeTestRule.waitUntil(timeoutMillis = SETTLE_TIMEOUT_MILLIS) {
            DOCUMENT_EXTENSIONS.all { composeTestRule.onAllNodesWithText(it).fetchSemanticsNodes().isNotEmpty() }
        }
        capture()
    }

    private fun capture() {
        composeTestRule
            .onNodeWithTag("attachment_preview_bar")
            .captureRoboImage(roborazziOptions = screenshotRoborazziOptions)
    }

    /**
     * Hosts the bar the way `UserInput` does, in a [Surface] taking the theme's background.
     *
     * The [Surface] is what makes the dark goldens meaningful: it supplies both the background the fade
     * ends in and the content colour the attachment labels are drawn with. Without it the labels stay the
     * default near-black in either theme.
     */
    @Composable
    private fun BarUnderTest(
        horizontalPadding: Dp = 0.dp,
        attachments: List<Attachment> = Companion.attachments,
        compact: Boolean = false,
    ) {
        Surface(color = ChatTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.fillMaxSize()) {
                AttachmentPreviewBar(
                    attachments = attachments,
                    onAttachmentClick = {},
                    onAttachmentRemoved = {},
                    horizontalPadding = horizontalPadding,
                    compact = compact,
                )
            }
        }
    }

    private companion object {
        val WIDE_INSET = 64.dp

        /**
         * Enough attachments to overflow the expanded window as well as the compact one, all of them
         * images.
         *
         * Images only on purpose. The audio and video fixtures draw a placeholder built outside Coil, which
         * renders differently depending on which test classes shared the JVM first, and these goldens are
         * about the bar's geometry rather than any one thumbnail.
         */
        val attachments: List<Attachment> = PreviewAttachments.getAttachments(12).toList()

        /**
         * An image, a PDF whose first page has rendered, and document tiles, the last with an extension longer
         * than the tile's label is sized for.
         *
         * Audio is among the documents because the bar draws it as one. Video is left out for the reason
         * [attachments] leaves it out.
         */
        val documents: List<Attachment> = listOf(
            PreviewAttachments.getAttachments(1).first(),
            TestAttachment("https://example.com/dummy.pdf", "dummy.pdf", "application/pdf"),
            TestAttachment(
                "https://example.com/report.docx",
                "report.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            ),
            TestAttachment("https://example.com/voice.mp3", "voice.mp3", "audio/mpeg"),
            TestAttachment("https://example.com/notes.txt", "notes.txt", "text/plain"),
            // A MIME type with no registered extension, so the label falls back to the URL's.
            TestAttachment("https://example.com/budget.numbers", "budget.numbers", "application/x-test-numbers"),
        )

        /** Labels the document tiles show once resolved; the PDF draws its page instead of a label. */
        val DOCUMENT_EXTENSIONS = listOf("DOCX", "MP3", "TXT", "NUMBERS")
    }

    private data class TestAttachment(
        override val url: String,
        override val friendlyName: String,
        override val mimeType: String?,
    ) : Attachment
}
