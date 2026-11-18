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

import androidx.compose.foundation.layout.Row
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.WindowSize
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.width
import com.nice.cxonechat.ui.composable.conversation.model.Message.QuickReply
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import com.nice.cxonechat.ui.composable.theme.Space
import com.nice.cxonechat.ui.util.preview.message.manyOptionsQuickReply
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Pins the width cap a quick reply's options row is given in a landscape window: the same
 * [Space.messageBubbleMaxWidthFraction] share of the content column the message bubble above it is capped
 * at, regardless of how narrow that bubble's own text made it, and left unconstrained in portrait.
 *
 * Enough options are given that the row would need far more than either window's width if laid out on a
 * single line, so a capped measurement can only be explained by the cap engaging, not by the options
 * happening to fit on their own.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
internal class QuickReplyOptionSubFrameLayoutTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `the options row is capped at the message bubble's own max width in a landscape window`() {
        showOptions(WIDE_LANDSCAPE_WINDOW)

        val expectedCap = bubbleMaxWidth(WIDE_LANDSCAPE_WINDOW)

        // widthIn(max = ...) is a hard constraint, so this could only be exceeded by a bug that skipped
        // applying it, never by generous chip packing.
        assertTrue(
            "the row's width (${optionsBounds().width}) does not exceed the bubble's own cap ($expectedCap)",
            optionsBounds().width <= expectedCap + TOLERANCE,
        )
    }

    @Test
    fun `the options row is not capped in a portrait window of the same width`() {
        showOptions(WIDE_PORTRAIT_WINDOW)

        // Compact windows apply no wide-screen inset, so the landscape formula run against a portrait
        // window of the same width is the cap the row would have if it were wrongly still applying there.
        val landscapeCapAtThisWidth = WIDE_PORTRAIT_WINDOW.width * Space().messageBubbleMaxWidthFraction

        // With enough options to need more than the whole window's width, an uncapped row grows past
        // where the landscape cap would have stopped it, showing portrait is left as it was.
        assertTrue(
            "the row's width (${optionsBounds().width}) is no wider than the landscape cap " +
                "($landscapeCapAtThisWidth) would allow, so the cap may be wrongly applying in portrait",
            optionsBounds().width > landscapeCapAtThisWidth + TOLERANCE,
        )
    }

    /**
     * The message bubble's own cap for a landscape [windowSize]: [Space.messageBubbleMaxWidthFraction] of
     * the content column, i.e. the window minus the wide-screen inset ([Space.landscapeHorizontalPadding])
     * on both edges, mirroring how the message bubble itself is capped.
     */
    private fun bubbleMaxWidth(windowSize: DpSize): Dp {
        val space = Space()
        val contentWidth = windowSize.width - space.landscapeHorizontalPadding * 2
        return contentWidth * space.messageBubbleMaxWidthFraction
    }

    private fun showOptions(windowSize: DpSize) {
        composeTestRule.setContent {
            ChatTheme {
                DeviceConfigurationOverride(DeviceConfigurationOverride.WindowSize(windowSize)) {
                    // A Row, as in real usage where MessageFrame's own outer Row sits above this composable:
                    // unlike a bare composition root, Row gives its non-weighted child loose (min 0) width
                    // constraints, so widthIn's cap can actually shrink it below the window's own forced size.
                    Row {
                        QuickReplyOptionSubFrame(
                            message = QuickReply(manyOptionsQuickReply(MANY_OPTIONS)) {},
                            onClick = {},
                        )
                    }
                }
            }
        }
        composeTestRule.waitForIdle()
    }

    private fun optionsBounds(): DpRect =
        composeTestRule.onNodeWithTag("quick_reply_options").getUnclippedBoundsInRoot()

    private companion object {
        /** Enough short options that their combined width on one line vastly exceeds either window below. */
        const val MANY_OPTIONS = 40

        /** A landscape window wide enough that the message bubble's own cap is still a large, easily measured target. */
        val WIDE_LANDSCAPE_WINDOW = DpSize(1000.dp, 500.dp)

        /** The same width as [WIDE_LANDSCAPE_WINDOW], but taller than wide, so the window is portrait. */
        val WIDE_PORTRAIT_WINDOW = DpSize(1000.dp, 1400.dp)

        val TOLERANCE = 1.dp
    }
}
