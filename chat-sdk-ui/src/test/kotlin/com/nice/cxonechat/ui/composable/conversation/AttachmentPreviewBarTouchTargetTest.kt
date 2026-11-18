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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.click
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nice.cxonechat.message.Attachment
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Pins the remove button's touch target: Material's minimum of 48.dp tall, in the regular and the compact bar alike,
 * around the design's 32.dp circle, yet never reaching the centre of its own attachment, where a tap opens it.
 *
 * Compose lets a small clickable answer taps up to the minimum around it, but the control itself still measures its
 * circle, which is the size accessibility services are given, and that reach is split evenly around the circle, so
 * the side hanging over the next attachment is lost to it. The target stops at the gap on that side instead and
 * reaches over its own attachment only as far as short of its centre, which leaves it narrower than 48.dp there.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], qualifiers = "w411dp-h891dp-normal-notlong-notround-notnight-xhdpi-finger")
internal class AttachmentPreviewBarTouchTargetTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val opened = mutableListOf<Attachment>()
    private val removed = mutableListOf<Attachment>()

    @Test
    fun `the remove button is a 48dp tall touch target`() {
        showBar()

        assertTouchTargetHeight()
    }

    @Test
    @Config(qualifiers = LANDSCAPE_PHONE)
    fun `the compact remove button is a 48dp tall touch target`() {
        showBar(compact = true)

        assertTouchTargetHeight()
    }

    @Test
    fun `a tap below the remove circle over its own attachment removes it`() {
        showBar()

        tapBelowCircle()

        assertEquals(listOf(first), removed)
        assertEquals(emptyList<Attachment>(), opened)
    }

    @Test
    @Config(qualifiers = LANDSCAPE_PHONE)
    fun `a tap below the compact remove circle over its own attachment removes it`() {
        showBar(compact = true)

        tapBelowCircle()

        assertEquals(listOf(first), removed)
        assertEquals(emptyList<Attachment>(), opened)
    }

    @Test
    fun `a tap at the centre of the attachment opens it`() {
        showBar()

        tapTileCentre(tileHeight = REGULAR_TILE_HEIGHT)

        assertEquals(listOf(first), opened)
        assertEquals(emptyList<Attachment>(), removed)
    }

    @Test
    @Config(qualifiers = LANDSCAPE_PHONE)
    fun `a tap at the centre of the compact attachment opens it`() {
        showBar(compact = true)

        tapTileCentre()

        assertEquals(listOf(first), opened)
        assertEquals(emptyList<Attachment>(), removed)
    }

    /** At 45.dp the tile is at its smallest, and its centre lies only 1.5.dp past the circle's inward edge. */
    @Test
    @Config(qualifiers = SMALL_LANDSCAPE_PHONE)
    fun `a tap at the centre of the smallest compact attachment opens it`() {
        showBar(compact = true)

        tapTileCentre()

        assertEquals(listOf(first), opened)
        assertEquals(emptyList<Attachment>(), removed)
    }

    /** Short of the centre is further in than the circle reaches on the smallest tile, yet all of the circle removes. */
    @Test
    @Config(qualifiers = SMALL_LANDSCAPE_PHONE)
    fun `a tap on the inward edge of the remove circle on the smallest compact attachment removes it`() {
        showBar(compact = true)

        val tile = itemBounds(first)
        tapAt(x = tile.right - CIRCLE_LEADING_EDGE_INSIDE_TILE + 1.dp, y = tile.top + CIRCLE_CENTRE_BELOW_TILE_TOP)

        assertEquals(listOf(first), removed)
        assertEquals(emptyList<Attachment>(), opened)
    }

    /**
     * Laid out as it is above the message input, below other content and inside a surface that clips whatever lies past
     * its bounds, every corner of the target the button reports still removes the attachment.
     */
    @Test
    fun `every corner of the remove target removes the attachment inside a clipping surface`() {
        showBar(belowContent = true)

        val target = composeTestRule.onNodeWithTag(CANCEL_TAG).getUnclippedBoundsInRoot()
        val inset = 1.dp
        for (x in listOf(target.left + inset, target.right - inset)) {
            for (y in listOf(target.top + inset, target.bottom - inset)) {
                tapAt(x, y)
            }
        }

        assertEquals(List(4) { first }, removed)
        assertEquals(emptyList<Attachment>(), opened)
    }

    /** The target stops at the gap between attachments, so the next one keeps every tap on it. */
    @Test
    fun `a tap on the next attachment's leading edge opens it`() {
        showBar()

        val next = itemBounds(second)
        tapAt(x = next.left + 1.dp, y = next.top + CIRCLE_CENTRE_BELOW_TILE_TOP)

        assertEquals(listOf(second), opened)
        assertEquals(emptyList<Attachment>(), removed)
    }

    private fun showBar(compact: Boolean = false, belowContent: Boolean = false) {
        composeTestRule.setContent {
            ChatTheme {
                Column(modifier = Modifier.fillMaxSize()) {
                    if (belowContent) Spacer(modifier = Modifier.height(CONTENT_ABOVE))
                    Surface {
                        AttachmentPreviewBar(
                            attachments = listOf(first, second),
                            onAttachmentClick = { opened += it },
                            onAttachmentRemoved = { removed += it },
                            compact = compact,
                        )
                    }
                }
            }
        }
        composeTestRule.waitForIdle()
    }

    private fun assertTouchTargetHeight() {
        composeTestRule.onNodeWithTag(CANCEL_TAG).assertHeightIsEqualTo(MINIMUM_TOUCH_TARGET)
    }

    /** Inside the circle's column but below it, where only the target reaches. */
    private fun tapBelowCircle() {
        val tile = itemBounds(first)
        tapAt(x = tile.right - 10.dp, y = tile.top + CIRCLE_BOTTOM_BELOW_TILE_TOP + 8.dp)
    }

    /** The compact tile is square, so its height defaults to its width; the item is no wider than its tile. */
    private fun tapTileCentre(tileHeight: Dp? = null) {
        val tile = itemBounds(first)
        tapAt(x = (tile.left + tile.right) / 2, y = tile.top + (tileHeight ?: tile.right - tile.left) / 2)
    }

    private fun itemBounds(attachment: Attachment) =
        composeTestRule.onNodeWithTag("preview_item_${attachment.friendlyName}").getUnclippedBoundsInRoot()

    private fun tapAt(x: Dp, y: Dp) {
        composeTestRule.onRoot().performTouchInput {
            click(Offset(x.toPx(), y.toPx()))
        }
        composeTestRule.waitForIdle()
    }

    private companion object {
        const val LANDSCAPE_PHONE = "w852dp-h412dp-normal-notlong-notround-notnight-xhdpi-finger"

        /** A 360dp shorter edge, which makes the compact tile 45.dp. */
        const val SMALL_LANDSCAPE_PHONE = "w800dp-h360dp-normal-notlong-notround-notnight-xhdpi-finger"

        val MINIMUM_TOUCH_TARGET = 48.dp

        /** Anything above the bar, into which a target reaching past the bar's top would stray. */
        val CONTENT_ABOVE = 100.dp

        /** The 32.dp circle hangs 11.dp past the tile's top trailing corner, so 21.dp of it lies over the tile. */
        val CIRCLE_LEADING_EDGE_INSIDE_TILE = 21.dp

        /** 16.dp down from its top, which is 11.dp above the tile's. */
        val CIRCLE_CENTRE_BELOW_TILE_TOP = 5.dp

        val CIRCLE_BOTTOM_BELOW_TILE_TOP = 21.dp

        val REGULAR_TILE_HEIGHT = 64.dp

        val first: Attachment = PreviewAttachments.txt
        val second: Attachment = PreviewAttachments.evil

        val CANCEL_TAG = "preview_item_cancel_${first.friendlyName}"
    }
}
