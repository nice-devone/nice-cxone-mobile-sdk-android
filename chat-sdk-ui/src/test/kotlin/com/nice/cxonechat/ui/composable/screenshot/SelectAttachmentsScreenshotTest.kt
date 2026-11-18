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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.core.app.ApplicationProvider
import coil3.ColorImage
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.annotation.DelicateCoilApi
import coil3.test.FakeImageLoaderEngine
import com.github.takahirom.roborazzi.captureRoboImage
import com.nice.cxonechat.message.Attachment
import com.nice.cxonechat.ui.composable.conversation.PreviewAttachments
import com.nice.cxonechat.ui.composable.conversation.attachments.AttachmentsView
import com.nice.cxonechat.ui.composable.conversation.attachments.PreviewType
import com.nice.cxonechat.ui.composable.conversation.attachments.SelectionViewModel
import com.nice.cxonechat.ui.composable.conversation.attachments.toPreviewType
import com.nice.cxonechat.ui.composable.generic.ThumbnailSize
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import com.nice.cxonechat.ui.composable.theme.WideScreenModalBottomSheet
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
 * Verifies the drawn appearance of the attachment selection sheet's content: its grid, the header above it
 * and the action bar below it.
 *
 * The expanded qualifier matches `AttachmentPreviewBarScreenshotTest`, so both agree on what a wide window
 * is.
 *
 * The content is composed inside [ChatTheme.WideScreenModalBottomSheet], the sheet
 * [com.nice.cxonechat.ui.composable.conversation.attachments.SelectAttachmentsView] builds, so a golden
 * shows the grid against the drag handle and the sheet width the screen really has rather than against a
 * stand-in host. Every window here is one the sheet spans: wider than it is tall in landscape, and
 * narrower than the Material cap in portrait — see
 * [com.nice.cxonechat.ui.composable.theme.wideScreenSheetMaxWidth].
 *
 * The sheet's own content is reached through `AttachmentsView` rather than `SelectAttachmentsView`, which
 * owns its [SelectionViewModel] and so cannot be put into selecting mode from a test.
 */
@RunWith(RobolectricTestRunner::class)
@Config(
    sdk = [33],
    qualifiers = "w411dp-h891dp-normal-notlong-notround-notnight-xhdpi-finger"
)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SelectAttachmentsScreenshotTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    /** Thumbnails load through Coil, which would otherwise record whatever the network returned. */
    @OptIn(DelicateCoilApi::class)
    @Before
    fun stubImageLoader() {
        val engine = FakeImageLoaderEngine.Builder().default(ColorImage(Color.CYAN)).build()
        SingletonImageLoader.setUnsafe(
            ImageLoader.Builder(ApplicationProvider.getApplicationContext<Context>())
                .components { add(engine) }
                // Resolve on the caller's thread, so the thumbnails are drawn during composition rather
                // than after a dispatcher hop no capture below awaits.
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
    fun `attachment selection portrait`() {
        captureSheetContent()
    }

    @Test
    @Config(qualifiers = "w411dp-h891dp-normal-notlong-notround-night-xhdpi-finger")
    fun `attachment selection portrait dark`() {
        captureSheetContent(darkTheme = true)
    }

    @Test
    @Config(qualifiers = "w852dp-h412dp-normal-notlong-notround-notnight-xhdpi-finger")
    fun `attachment selection landscape`() {
        captureSheetContent()
    }

    @Test
    @Config(qualifiers = "w852dp-h412dp-normal-notlong-notround-night-xhdpi-finger")
    fun `attachment selection landscape dark`() {
        captureSheetContent(darkTheme = true)
    }

    /**
     * Right to left, where a content inset applied to one physical side rather than mirrored would show up
     * as the grid drifting off centre.
     */
    @Test
    @Config(qualifiers = "w852dp-h412dp-normal-notlong-notround-notnight-xhdpi-finger")
    fun `attachment selection landscape right to left`() {
        captureSheetContent(layoutDirection = LayoutDirection.Rtl)
    }

    /**
     * Selecting mode, which is the only state that draws the action bar's select all, none and count
     * controls; the resting state shows a lone share icon.
     */
    @Test
    @Config(qualifiers = "w852dp-h412dp-normal-notlong-notround-notnight-xhdpi-finger")
    fun `attachment selection while selecting`() {
        captureSheetContent(selecting = true)
    }

    /**
     * A non-previewable file leading the grid, which is drawn as a document glyph carrying the file's
     * extension instead of a thumbnail. Both themes, because the glyph is tinted for neither of the sheet's
     * backgrounds: it takes the colour that belongs on the brand fill, which is near-white in light and
     * near-black in dark, while the sheet behind it is the opposite of each. This is the only coverage of
     * that tile at this size anywhere in the module, and it is recorded before the fix so the change to it
     * can be read as a diff.
     */
    @Test
    fun `attachment selection with a file portrait`() {
        captureSheetContent(attachmentList = attachmentsLedByAFile)
    }

    @Test
    @Config(qualifiers = "w411dp-h891dp-normal-notlong-notround-night-xhdpi-finger")
    fun `attachment selection with a file portrait dark`() {
        captureSheetContent(darkTheme = true, attachmentList = attachmentsLedByAFile)
    }

    @OptIn(ExperimentalMaterial3Api::class)
    private fun captureSheetContent(
        darkTheme: Boolean = false,
        selecting: Boolean = false,
        layoutDirection: LayoutDirection = LayoutDirection.Ltr,
        attachmentList: List<Attachment> = attachments,
    ) {
        val viewModel = SelectionViewModel(
            attachments = attachmentList,
            onAttachmentTapped = {},
            onShare = {},
        ).apply { if (selecting) toggleSelecting(attachmentList.first()) }

        composeTestRule.setContent {
            ChatTheme(darkTheme = darkTheme) {
                // The sheet is drawn over this window rather than inside it, so the host needs a themed
                // surface of its own for the sheet's scrim to fall on.
                Surface(modifier = Modifier.fillMaxSize()) {
                    ChatTheme.WideScreenModalBottomSheet(
                        onDismissRequest = {},
                        // The two arguments SelectAttachmentsView overrides that change what is drawn.
                        containerColor = ChatTheme.chatColors.token.background.default,
                        dragHandle = {
                            BottomSheetDefaults.DragHandle(Modifier.minimumInteractiveComponentSize())
                        },
                    ) {
                        // Provided around the content rather than around the sheet: the sheet gets a window
                        // of its own, which does not inherit the layout direction from the composition that
                        // opened it, so an outer provider would leave the content laid out left to right.
                        CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                            AttachmentsView(viewModel)
                        }
                    }
                }
            }
        }

        // The thumbnails arrive through Coil's asynchronous pipeline even from a stubbed loader, so the
        // first cell has to be present before capturing or the grid records empty frames.
        composeTestRule.waitUntil {
            composeTestRule.onAllNodesWithTag(FIRST_CELL_TAG).fetchSemanticsNodes().isNotEmpty()
        }
        // A file tile resolves its extension off the main thread and draws the "N/A" placeholder until it
        // lands, so idleness alone would let that placeholder into the golden. Read off the fixture rather
        // than named by the caller, so a case cannot pass a file and skip the wait: that failure records a
        // wrong golden instead of going red. Counted rather than merely present, because two files sharing
        // an extension would otherwise let the first to resolve satisfy the wait for both. The budget
        // matches the hop onto Dispatchers.IO, which on a cold class also pays the Robolectric warm-up.
        attachmentList.mapNotNull { it.expectedExtensionLabel() }
            .groupingBy { label -> label }
            .eachCount()
            .forEach { (label, tiles) ->
                composeTestRule.waitUntil(timeoutMillis = SETTLE_TIMEOUT_MILLIS) {
                    composeTestRule.onAllNodesWithText(label).fetchSemanticsNodes().size == tiles
                }
            }
        // The sheet slides in, and unlike its width its content's position changes while it does, so an
        // unsettled frame would record the grid mid-travel.
        composeTestRule.waitForIdle()
        // A cell's node is laid out before its thumbnail resolves, so the tag above proves composition
        // only. Each cell keeps a spinner while its own load is outstanding, so no spinner anywhere is
        // what proves all of them are drawn.
        assertTrue(
            "Not every thumbnail loaded before capture; refusing to record a spinner.",
            composeTestRule.onAllNodesWithTag("loading", useUnmergedTree = true).fetchSemanticsNodes().isEmpty(),
        )
        composeTestRule.onRoot().captureRoboImage(roborazziOptions = screenshotRoborazziOptions)
    }

    private companion object {
        const val FIRST_CELL_TAG = "attachment_preview_0"

        /**
         * Enough attachments to spill past the first row at either column count, so the grid overflows a
         * short landscape window and the vertical arrangement stays visible in portrait.
         *
         * Images only on purpose: the audio and video fixtures draw a placeholder built outside Coil, which
         * renders differently depending on which test classes shared the JVM first.
         */
        val attachments: List<Attachment> = PreviewAttachments.getAttachments(8).toList()

        /**
         * The same grid with a non-previewable file in the leading cell, so [FIRST_CELL_TAG] is the file's
         * own tile and the wait for the first cell covers it.
         */
        val attachmentsLedByAFile: List<Attachment> = listOf(
            object : Attachment {
                override val url: String = "https://example.com/report.txt"
                override val friendlyName: String = "report.txt"
                override val mimeType: String = "text/plain"
            }
        ) + attachments.drop(1)

        /**
         * The label this attachment's tile draws once resolved, or null where its tile draws none.
         *
         * Asks production's own router rather than restating the rule: a video, an audio file and a PDF all
         * reach a tile that carries no label, and the answer depends on the size, so it is asked at
         * [ThumbnailSize.LARGE] — the size this sheet's grid draws at. A fixture with no mime type is a
         * placeholder and draws none either, so it cannot leave a test waiting.
         *
         * The extension is read off the file name, where production resolves it from the mime type and falls
         * back to the URL. The fixtures keep all three in step; a fixture where they disagree wants the
         * production helper instead of this one.
         */
        fun Attachment.expectedExtensionLabel(): String? =
            if (mimeType.toPreviewType(ThumbnailSize.LARGE) != PreviewType.FILE) {
                null
            } else {
                friendlyName.substringAfterLast('.').uppercase()
            }
    }
}
