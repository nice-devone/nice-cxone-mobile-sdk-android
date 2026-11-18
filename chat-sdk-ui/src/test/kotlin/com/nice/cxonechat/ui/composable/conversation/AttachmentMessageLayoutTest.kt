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

package com.nice.cxonechat.ui.composable.conversation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.WindowSize
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.LayoutDirection.Ltr
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import com.nice.cxonechat.message.Attachment
import com.nice.cxonechat.ui.composable.conversation.model.Message.WithAttachments
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import com.nice.cxonechat.ui.composable.theme.ChatTheme.chatColors
import com.nice.cxonechat.ui.composable.theme.Space
import com.nice.cxonechat.ui.util.preview.message.UiSdkText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Pins the geometry of an attachment message: the size of a preview tile, the grid the tiles sit in and
 * the point at which the overflow badge takes over.
 *
 * Measurement and layout decide all of it, and Robolectric runs both phases. The tile size is the reason
 * these are bounds assertions rather than goldens: a tile is a rounded square either way, so a golden
 * cannot say which size it was given. `AttachmentMessageScreenshotTest` covers the appearance these
 * numbers produce.
 *
 * Files are used throughout rather than images, so a tile draws a local icon instead of reaching Coil,
 * and the measured bounds belong to the tile rather than to a decoded bitmap.
 */
@RunWith(RobolectricTestRunner::class)
@Config(
    sdk = [33],
    qualifiers = "w411dp-h891dp-normal-notlong-notround-notnight-xhdpi-finger"
)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
internal class AttachmentMessageLayoutTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    /** Window the message was composed in, as composition itself saw it. */
    private var windowWidth: Dp = Dp.Unspecified

    @Test
    fun `a single attachment is previewed as a square tile`() {
        showMessage(files(1))

        val preview = singlePreviewBounds()

        assertEquals(TILE_SIZE.value, preview.width.value, TOLERANCE)
        assertEquals(TILE_SIZE.value, preview.height.value, TOLERANCE)
    }

    @Test
    fun `four attachments are previewed as a two by two grid with the designed gap`() {
        showMessage(files(4))

        val topLeft = previewBounds(0)
        val topRight = previewBounds(1)
        val bottomLeft = previewBounds(2)

        assertEquals(GAP.value, (topRight.left - topLeft.right).value, TOLERANCE)
        assertEquals(GAP.value, (bottomLeft.top - topLeft.bottom).value, TOLERANCE)
        assertEquals("the second row starts a new column", topLeft.left.value, bottomLeft.left.value, TOLERANCE)
    }

    /** The group is the grid and nothing more, so the bubble around it wraps the tiles rather than the window. */
    @Test
    fun `the group is as wide as the two tiles and the gap between them`() {
        showMessage(files(4))

        assertEquals((TILE_SIZE * 2 + GAP).value, groupBounds().width.value, TOLERANCE)
    }

    /**
     * A portrait bubble is narrower than two window-sized tiles, so the tiles shrink to the bubble rather than
     * the bubble squeezing the second column.
     */
    @Test
    fun `four attachments in a bubble fit inside it in equal columns`() {
        composeTestRule.setContent {
            ChatTheme {
                MessageFrame(
                    position = MessageItemGroupState.SOLO,
                    messageContentType = ContentType.Attachment,
                    isAgent = true,
                    colors = chatColors.agent,
                    showFrame = true,
                ) {
                    AttachmentMessageUnderTest(files(4), Modifier.padding(Space().attachmentMessagePadding))
                }
            }
        }
        composeTestRule.waitForIdle()

        val tiles = List(4, ::previewBounds)
        tiles.forEachIndexed { index, tile ->
            assertEquals("tile $index width", tiles[0].width.value, tile.width.value, TOLERANCE)
        }
        val padding = Space().attachmentMessagePadding
        val gridWithPadding = tiles[1].right - tiles[0].left +
            padding.calculateLeftPadding(Ltr) + padding.calculateRightPadding(Ltr)
        assertTrue(
            "grid $gridWithPadding is wider than the bubble $BUBBLE_MAX_WIDTH",
            gridWithPadding.value <= BUBBLE_MAX_WIDTH.value + TOLERANCE,
        )
    }

    /** A fifth attachment has nowhere to go in a four-tile grid, so the last tile carries the remainder. */
    @Test
    fun `a fifth attachment is counted on the overflow badge instead of adding a tile`() {
        showMessage(files(5))

        composeTestRule.onNodeWithTag("attachment_preview_3").assertIsDisplayed()
        composeTestRule.onNodeWithTag("attachment_overflow").assertIsDisplayed()
        composeTestRule.onNodeWithText("+1").assertIsDisplayed()
        // The badge alone would also be drawn by a grid which had grown a fifth tile to put it on.
        composeTestRule.onNodeWithTag("attachment_preview_4").assertDoesNotExist()
    }

    /**
     * Rotating the device does not resize the tile, because the shorter edge of the window is the same
     * edge either way round.
     */
    @Test
    @Config(qualifiers = ROTATED_QUALIFIERS)
    fun `a landscape window previews an attachment at the same tile size as a portrait one`() {
        showMessage(files(1))

        // The tile is a fraction of the shorter edge, which rotating does not change, so without this the
        // assertion below would also hold if the qualifier had never reached the composition at all.
        assertEquals(
            "the rotated qualifier did not reach the composition",
            ROTATED_WINDOW_WIDTH.value,
            windowWidth.value,
            TOLERANCE,
        )
        assertEquals(TILE_SIZE.value, singlePreviewBounds().width.value, TOLERANCE)
    }

    /**
     * The tile is a fraction of the window rather than of the display, so a window smaller than the
     * display gets a tile scaled for the space it actually has.
     *
     * A window this much narrower than its display is what split-screen, freeform and a foldable folded
     * shut all look like to the layout. Rotation cannot stand in for any of them, since the shorter edge
     * of a display is the same edge whichever way round it is held.
     */
    @Test
    fun `a window narrower than the display previews the attachment at the window's size`() {
        showMessage(files(1), windowSize = SPLIT_SCREEN_WINDOW)

        // Separates the override failing to shrink the window from the layout ignoring a window that did
        // shrink: either one leaves a display-sized tile, and only the first is a fault in this test.
        assertEquals(
            "the overridden window did not reach the composition",
            SPLIT_SCREEN_WINDOW.width.value,
            windowWidth.value,
            TOLERANCE,
        )
        assertEquals(SPLIT_SCREEN_TILE.value, singlePreviewBounds().width.value, TOLERANCE)
    }

    private fun showMessage(attachments: List<Attachment>, windowSize: DpSize? = null) {
        composeTestRule.setContent {
            ChatTheme {
                Box(modifier = Modifier.fillMaxSize()) {
                    WindowSized(windowSize) {
                        windowWidth = LocalWindowInfo.current.containerDpSize.width
                        AttachmentMessageUnderTest(attachments)
                    }
                }
            }
        }
        composeTestRule.waitForIdle()
    }

    private fun singlePreviewBounds(): DpRect =
        composeTestRule.onNodeWithTag("attachment_preview_${files(1).single().url}").getUnclippedBoundsInRoot()

    private fun previewBounds(index: Int): DpRect =
        composeTestRule.onNodeWithTag("attachment_preview_$index").getUnclippedBoundsInRoot()

    private fun groupBounds(): DpRect =
        composeTestRule.onNodeWithTag("attachment_preview_group").getUnclippedBoundsInRoot()

    private companion object {
        /**
         * The class qualifier rotated: the same display held the other way round, and still wide enough
         * that the wide-screen layout applies.
         *
         * A rotation rather than a second device, so the shorter edge is 411dp in both and one expected
         * tile size covers both. `AttachmentMessageScreenshotTest` captures a wide phone instead, whose
         * display differs from this one by a dp — too little to see, and it has no arithmetic to keep true.
         */
        const val ROTATED_QUALIFIERS = "w891dp-h411dp-normal-notlong-notround-notnight-xhdpi-finger"

        /** The longer edge of [ROTATED_QUALIFIERS], which is the edge the tile is deliberately not sized from. */
        val ROTATED_WINDOW_WIDTH = 891.dp

        /** Narrower than the display in either orientation, as a split-screen or freeform window is. */
        val SPLIT_SCREEN_WINDOW = DpSize(320.dp, 480.dp)

        /** The shorter edge of the qualifier's window, which the tile is a fraction of, either way round. */
        val TILE_SIZE = 411.dp * Space().attachmentPreviewRegularWidthPercentage

        /** The shorter edge of [SPLIT_SCREEN_WINDOW], derived the way the code is so a wider window stays right. */
        val SPLIT_SCREEN_TILE = minOf(SPLIT_SCREEN_WINDOW.width, SPLIT_SCREEN_WINDOW.height) *
            Space().attachmentPreviewRegularWidthPercentage

        val GAP = Space().semiLarge

        /** The widest a bubble may grow in the qualifier's compact window, which has no wide-screen inset. */
        val BUBBLE_MAX_WIDTH = 411.dp * Space().messageBubbleMaxWidthFraction
        const val TOLERANCE = 0.5f

        fun files(count: Int): List<Attachment> = List(count) { index ->
            object : Attachment {
                override val url: String = "https://example.com/report_$index.txt"
                override val friendlyName: String = "report_$index.txt"
                override val mimeType: String = "text/plain"
            }
        }
    }
}

/** Draws [content] in a window of its own, or in the qualifier's window when [windowSize] is null. */
@Composable
private fun WindowSized(windowSize: DpSize?, content: @Composable () -> Unit) {
    if (windowSize == null) {
        content()
    } else {
        DeviceConfigurationOverride(DeviceConfigurationOverride.WindowSize(windowSize)) { content() }
    }
}

@Composable
private fun AttachmentMessageUnderTest(attachments: List<Attachment>, modifier: Modifier = Modifier) {
    AttachmentMessage(
        message = WithAttachments(
            message = UiSdkText(text = "", attachments = attachments),
            attachments = attachments,
        ),
        modifier = modifier,
        onShowFrame = {},
        onAttachmentClicked = {},
        onMoreClicked = {},
        onShare = {},
    )
}
