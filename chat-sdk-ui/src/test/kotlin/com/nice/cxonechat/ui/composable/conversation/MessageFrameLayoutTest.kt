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
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.dp
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import com.nice.cxonechat.ui.composable.theme.ChatTheme.chatColors
import com.nice.cxonechat.ui.composable.theme.Space
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Pins the gap [MessageFrame] leaves between a message bubble and the action beside it, which for an
 * attachment message is the share button.
 *
 * A bounds assertion rather than a golden: the two candidate gaps are four dp apart on a button that
 * carries no edge of its own to line up against, so a golden of either is a plausible picture of the
 * other.
 *
 * The frame is given a plain box as its content rather than a real message, so the measurement belongs to
 * the frame's own arrangement and not to whatever padding a message type adds inside the bubble. It is
 * drawn as an agent message so that the frame keeps the ambient layout direction; a customer message
 * inverts it, which would swap which edges the gap sits between without changing its width.
 */
@RunWith(RobolectricTestRunner::class)
@Config(
    sdk = [33],
    qualifiers = "w411dp-h891dp-normal-notlong-notround-notnight-xhdpi-finger"
)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
internal class MessageFrameLayoutTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `the share button sits a designed gap away from the bubble`() {
        showFrameWithShareButton()

        val shareButton = shareButtonBounds()

        // Material 3 grows a clickable below its 48dp touch target back up to it. Were that happening here the
        // gap would be measured to transparent margin instead of to the drawn circle, and still read as 12dp.
        assertEquals(
            "the share button's bounds are its ink rather than an inflated touch target",
            BUTTON_SIZE.value,
            (shareButton.right - shareButton.left).value,
            TOLERANCE,
        )
        assertEquals(GAP.value, (shareButton.left - bubbleBounds().right).value, TOLERANCE)
    }

    /** The button is centred on the bubble, so it stays beside the attachments rather than a corner. */
    @Test
    fun `the share button is centred against the bubble`() {
        showFrameWithShareButton()

        val bubble = bubbleBounds()
        val shareButton = shareButtonBounds()

        assertEquals(
            (bubble.top + bubble.bottom).value / 2,
            (shareButton.top + shareButton.bottom).value / 2,
            TOLERANCE,
        )
    }

    private fun showFrameWithShareButton() {
        composeTestRule.setContent {
            ChatTheme {
                MessageFrame(
                    position = MessageItemGroupState.SOLO,
                    messageContentType = ContentType.Attachment,
                    isAgent = true,
                    colors = chatColors.agent,
                    showFrame = true,
                    trailingContent = { ShareAttachmentsIcon(contentDescription = "share") {} },
                ) {
                    Box(modifier = Modifier.size(BUBBLE_SIZE).testTag("bubble_content"))
                }
            }
        }
        composeTestRule.waitForIdle()
    }

    private fun bubbleBounds(): DpRect =
        composeTestRule.onNodeWithTag("bubble_content").getUnclippedBoundsInRoot()

    private fun shareButtonBounds(): DpRect =
        composeTestRule.onNodeWithTag("share_icon_button").getUnclippedBoundsInRoot()

    private companion object {
        /** Stands in for a message's content; large enough that the button cannot be the taller of the two. */
        val BUBBLE_SIZE = 120.dp

        /** What `ShareAttachmentsIcon` asks for, and so what the gap either side of it is measured from. */
        val BUTTON_SIZE = 34.dp

        val GAP = Space().semiLarge
        const val TOLERANCE = 0.5f
    }
}
