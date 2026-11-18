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

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.view.View
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import androidx.test.core.app.ApplicationProvider
import coil3.ColorImage
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.annotation.DelicateCoilApi
import coil3.test.FakeImageLoaderEngine
import com.nice.cxonechat.message.Attachment
import com.nice.cxonechat.ui.composable.generic.scrollbar.ScrollbarBarBoundsKey
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import com.nice.cxonechat.ui.composable.theme.Space
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Pins the geometry [AttachmentPreviewBar] takes from the design, and the properties which let its trailing
 * inset be a plain constant rather than an animated one.
 *
 * These assertions read measurement and layout, which Robolectric performs. It never runs Compose's draw
 * phase on its own, so [barLeft] — the one reading geometry the scrollbar publishes from that phase — forces
 * a frame itself. Appearance is covered by `AttachmentPreviewBarScreenshotTest` instead.
 */
@RunWith(RobolectricTestRunner::class)
@Config(
    sdk = [33],
    qualifiers = "w411dp-h891dp-normal-notlong-notround-notnight-xhdpi-finger"
)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
internal class AttachmentPreviewBarLayoutTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    /** Attachment thumbnails load through Coil, which would otherwise reach the network. */
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

    @OptIn(DelicateCoilApi::class)
    @After
    fun resetImageLoader() {
        SingletonImageLoader.reset()
    }

    @Test
    fun `the bar reserves a scrollbar strip below the attachments`() {
        showBar()

        composeTestRule.onNodeWithTag(ROW_TAG).assertHeightIsEqualTo(ROW_HEIGHT)
    }

    @Test
    fun `the first attachment starts at the compact content inset`() {
        showBar()

        assertEquals(COMPACT_INSET.value, firstAttachmentLeft().value, TOLERANCE)
    }

    @Test
    fun `a wide window insets the first attachment without adding to the compact inset`() {
        showBar(horizontalPadding = WIDE_INSET)

        assertEquals(WIDE_INSET.value, firstAttachmentLeft().value, TOLERANCE)
    }

    @Test
    fun `attachments are spaced by the designed gap`() {
        showBar()

        val first = composeTestRule.onNodeWithTag("attachment_preview_item_0").getUnclippedBoundsInRoot()
        val second = composeTestRule.onNodeWithTag("attachment_preview_item_1").getUnclippedBoundsInRoot()

        assertEquals(8.dp.value, (second.left - first.right).value, TOLERANCE)
    }

    @Test
    fun `the trailing inset matches the leading one once the row reaches its end`() {
        val attachments = manyAttachments()
        showBar(attachments = attachments)

        scrollToEnd(attachments)

        assertEquals(COMPACT_INSET.value, trailingGap(attachments).value, TOLERANCE)
    }

    /**
     * The trailing inset has to cover the remove icon's overhang, or the row clips the last icon on its
     * main axis.
     *
     * Asserted on the tokens rather than on measured bounds, because a semantics node reports the position
     * its parent placed it at, before the `Modifier.offset` that produces the overhang — so the overhang is
     * invisible to any bounds assertion. `AttachmentPreviewBarScreenshotTest` covers the drawn result.
     */
    @Test
    fun `the content inset covers the remove icon's overhang`() {
        assertTrue(
            "a trailing inset below the overhang lets the row clip the last remove icon",
            Space().semiLarge.value >= Space().attachmentUploadRemoveOverhang.value
        )
    }

    /**
     * Guards the trailing inset against feeding back into the overflow verdict it depends on.
     *
     * The viewport is what `canScrollForward` is measured against, so a trailing inset applied as a layout
     * inset could never settle: at the end of travel the verdict is false, the inset grows, the shrunken
     * viewport puts the scroll range past the offset the row already sits at, the verdict flips true, the
     * inset collapses, the offset is clamped back to the end — and the verdict flips false again, animating
     * between the two forever. Content padding cannot, because it leaves the viewport width alone.
     */
    @Test
    fun `the row settles once scrolled to its end`() {
        val attachments = manyAttachments()
        showBar(attachments = attachments)
        scrollToEnd(attachments)

        val settled = trailingGap(attachments)
        composeTestRule.waitForIdle()

        assertEquals(settled.value, trailingGap(attachments).value, TOLERANCE)
    }

    /**
     * The fade stands for the attachments the row is hiding, so it has to span them however tall text
     * scaling makes them. A fixed height leaves the grown part of the row unfaded.
     */
    @Test
    fun `the trailing fade spans the attachments at a large text scale`() {
        showBar(attachments = manyAttachments(), fontScale = LARGE_TEXT_SCALE)

        val row = rowBounds()
        val fade = composeTestRule.onNodeWithTag("overflow_effect").getUnclippedBoundsInRoot()

        assertTrue(
            "the row never grew past its floor, so the fade's height went untested",
            row.height.value > ROW_HEIGHT.value
        )
        assertEquals((row.height - SCROLLBAR_STRIP).value, fade.height.value, TOLERANCE)
    }

    /**
     * The gap below the attachments is content padding rather than slack left inside the row's floor, so it
     * holds however tall text scaling makes the labels. Slack is the first thing they grow out of, and the
     * scrollbar ends up all but touching them.
     */
    @Test
    fun `the attachments keep clear of the scrollbar strip at a large text scale`() {
        showBar(fontScale = LARGE_TEXT_SCALE)

        val attachmentBottom = composeTestRule
            .onNodeWithTag("attachment_preview_item_0")
            .getUnclippedBoundsInRoot()
            .bottom

        assertEquals(
            (SCROLLBAR_STRIP + Space().attachmentPreviewBarContentBottomPadding).value,
            (rowBounds().bottom - attachmentBottom).value,
            TOLERANCE,
        )
    }

    /**
     * The scrollbar stands for the attachments, so its track ends where they do rather than running the
     * full viewport — the bar starts under the first attachment instead of 4.dp short of it.
     */
    @Test
    fun `the scrollbar track starts where the attachments do`() {
        showBar(attachments = manyAttachments())

        assertEquals(firstAttachmentLeft().value, barLeft().value, TOLERANCE)
    }

    /** The same alignment has to follow the inset out to a wide window, not just hold at the compact one. */
    @Test
    fun `the scrollbar track follows the attachments to the wide inset`() {
        showBar(attachments = manyAttachments(), horizontalPadding = WIDE_INSET)

        assertEquals(WIDE_INSET.value, barLeft().value, TOLERANCE)
    }

    @Test
    @Config(qualifiers = LANDSCAPE_PHONE)
    fun `a compact bar sizes its attachments from the window's shorter edge`() {
        showBar(compact = true)

        val item = composeTestRule.onNodeWithTag("attachment_preview_item_0").getUnclippedBoundsInRoot()
        assertEquals(COMPACT_TILE.value, item.width.value, TOLERANCE)
        assertEquals(COMPACT_TILE.value, item.height.value, TOLERANCE)
        composeTestRule.onNodeWithTag(ROW_TAG).assertHeightIsEqualTo(COMPACT_ROW_HEIGHT)
    }

    /** On a shorter window the tile is smaller than the remove button's touch target, which must not grow it. */
    @Test
    @Config(qualifiers = SHORT_LANDSCAPE_PHONE)
    fun `a compact attachment smaller than the remove touch target keeps its size`() {
        showBar(compact = true)

        val item = composeTestRule.onNodeWithTag("attachment_preview_item_0").getUnclippedBoundsInRoot()
        assertEquals(SHORT_COMPACT_TILE.value, item.width.value, TOLERANCE)
        assertEquals(SHORT_COMPACT_TILE.value, item.height.value, TOLERANCE)
    }

    /** With no names to grow, the text scale has nothing to push the compact row past its height with. */
    @Test
    @Config(qualifiers = LANDSCAPE_PHONE)
    fun `a compact bar keeps its height at a large text scale`() {
        showBar(compact = true, fontScale = LARGE_TEXT_SCALE)

        composeTestRule.onNodeWithTag(ROW_TAG).assertHeightIsEqualTo(COMPACT_ROW_HEIGHT)
    }

    @Test
    @Config(qualifiers = LANDSCAPE_PHONE)
    fun `a regular bar shows the attachment names`() {
        showBar(attachments = listOf(PreviewAttachments.pdf))

        assertEquals(1, nameNodeCount(PreviewAttachments.pdf))
    }

    @Test
    @Config(qualifiers = LANDSCAPE_PHONE)
    fun `a compact bar shows no attachment names`() {
        showBar(attachments = listOf(PreviewAttachments.pdf), compact = true)

        assertEquals(0, nameNodeCount(PreviewAttachments.pdf))
    }

    /**
     * On its way to the compact layout the row passes through heights between the two, rather than snapping.
     *
     * Records every frame until the row settles instead of sampling one moment, because how many frames pass before
     * the transition starts differs from run to run.
     */
    @Test
    @Config(qualifiers = LANDSCAPE_PHONE)
    fun `turning compact animates the row height`() {
        var compact by mutableStateOf(false)
        composeTestRule.setContent {
            ChatTheme {
                Box(modifier = Modifier.fillMaxSize()) {
                    AttachmentPreviewBar(
                        attachments = PreviewAttachments.choices,
                        onAttachmentClick = {},
                        onAttachmentRemoved = {},
                        compact = compact,
                    )
                }
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.mainClock.autoAdvance = false

        composeTestRule.runOnIdle { compact = true }
        val heights = generateSequence {
            composeTestRule.mainClock.advanceTimeByFrame()
            rowBounds().height
        }.take(MAX_TRANSITION_FRAMES).takeWhile { it != COMPACT_ROW_HEIGHT }.toList()

        assertTrue(
            "No frame between the two layouts in $heights",
            heights.any { it < ROW_HEIGHT && it > COMPACT_ROW_HEIGHT },
        )
        composeTestRule.onNodeWithTag(ROW_TAG).assertHeightIsEqualTo(COMPACT_ROW_HEIGHT)
    }

    private fun showBar(
        attachments: List<Attachment> = PreviewAttachments.choices,
        horizontalPadding: Dp = 0.dp,
        fontScale: Float = 1f,
        compact: Boolean = false,
    ) {
        composeTestRule.setContent {
            val scaled = Density(LocalDensity.current.density, fontScale)
            CompositionLocalProvider(LocalDensity provides scaled) {
                ChatTheme {
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
        }
        composeTestRule.waitForIdle()
    }

    private fun rowBounds() = composeTestRule.onNodeWithTag(ROW_TAG).getUnclippedBoundsInRoot()

    /** The name sits under the item's merged semantics, so it is only found in the unmerged tree. */
    private fun nameNodeCount(attachment: Attachment): Int = composeTestRule
        .onAllNodesWithText(attachment.friendlyName, useUnmergedTree = true)
        .fetchSemanticsNodes()
        .size

    /**
     * Leading edge of the scrollbar's track, in the root's coordinates so it compares with the attachments.
     *
     * A frame has to be drawn first: the scrollbar measures its geometry in the draw phase, which
     * Robolectric does not run on its own, so the semantics would otherwise report an empty rectangle. The
     * host view is drawn into a throwaway bitmap rather than through an image-capture helper, which under
     * Robolectric waits for a redraw signal that never arrives.
     */
    private fun barLeft(): Dp {
        composeTestRule.waitForIdle()
        val view = composeTestRule.onRoot().fetchSemanticsNode().root as View
        val bitmap = Bitmap.createBitmap(
            view.width.coerceAtLeast(1),
            view.height.coerceAtLeast(1),
            Bitmap.Config.ARGB_8888,
        )
        view.draw(Canvas(bitmap))
        bitmap.recycle()

        val bar = composeTestRule.onNodeWithTag(ROW_TAG).fetchSemanticsNode().config[ScrollbarBarBoundsKey]
        return rowBounds().left + with(composeTestRule.density) { bar.left.toDp() }
    }

    private fun firstAttachmentLeft(): Dp =
        composeTestRule.onNodeWithTag("attachment_preview_item_0").getUnclippedBoundsInRoot().left

    /** Distance from the last attachment's trailing edge to the row's, once the row is scrolled to its end. */
    private fun trailingGap(attachments: List<Attachment>): Dp {
        val last = composeTestRule
            .onNodeWithTag("attachment_preview_item_${attachments.lastIndex}")
            .getUnclippedBoundsInRoot()
        return rowBounds().right - last.right
    }

    private fun scrollToEnd(attachments: List<Attachment>) {
        composeTestRule.onNodeWithTag(ROW_TAG).performScrollToIndex(attachments.lastIndex)
        composeTestRule.waitForIdle()
    }

    /** Enough attachments to overflow both the compact and the wide window. */
    private fun manyAttachments(): List<Attachment> = PreviewAttachments.with(12).toList()

    private companion object {
        const val ROW_TAG = "attachment_preview_row"
        val COMPACT_INSET = 12.dp
        val WIDE_INSET = 64.dp

        /** The design's row plus the scrollbar footprint below it. */
        val ROW_HEIGHT = 112.dp

        /** The scrollbar's footprint, which the fade leaves uncovered. */
        val SCROLLBAR_STRIP = 12.dp

        /** The window the bar is compact in: a phone held in landscape. */
        const val LANDSCAPE_PHONE = "w852dp-h412dp-normal-notlong-notround-notnight-xhdpi-finger"

        /** An eighth of that window's shorter edge. */
        val COMPACT_TILE = 51.5.dp

        /** The tile inset by the design's 10.dp at both ends, plus the scrollbar footprint. */
        val COMPACT_ROW_HEIGHT = 10.dp + COMPACT_TILE + 10.dp + SCROLLBAR_STRIP

        /** A shorter phone in landscape, whose compact tile is smaller than a touch target. */
        const val SHORT_LANDSCAPE_PHONE = "w780dp-h360dp-normal-notlong-notround-notnight-xhdpi-finger"

        /** An eighth of that window's shorter edge. */
        val SHORT_COMPACT_TILE = 45.dp

        /** A second of 16ms frames: several times the default `tween` the transition uses, so it has settled by then. */
        const val MAX_TRANSITION_FRAMES = 60

        /** Enough to grow the item labels past the row's floor. */
        const val LARGE_TEXT_SCALE = 2f
        const val TOLERANCE = 0.5f
    }
}
