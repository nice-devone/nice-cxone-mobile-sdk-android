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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.width
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import com.nice.cxonechat.ui.composable.theme.ChatTheme.chatColors
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Pins the widest a framed message bubble may grow: 60% of the window, after the 64dp wide-screen inset is taken
 * from each side. The content is a box that asks for all the width it can get, so what is measured is the frame's
 * own cap and not any message type's padding.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], qualifiers = "w411dp-h891dp-normal-notlong-notround-notnight-xhdpi-finger")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
internal class MessageBubbleMaxWidthTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `a compact window caps the bubble at 60 percent of the window`() {
        assertEquals(246.6f, greedyBubbleWidth(), TOLERANCE)
    }

    @Test
    @Config(qualifiers = "w852dp-h412dp-normal-notlong-notround-notnight-xhdpi-finger")
    fun `a landscape window caps the bubble at 60 percent of the window without the wide-screen inset`() {
        assertEquals(434.4f, greedyBubbleWidth(), TOLERANCE)
    }

    @Test
    @Config(qualifiers = "w1280dp-h800dp-normal-notlong-notround-notnight-xhdpi-finger")
    fun `an expanded window caps the bubble at 60 percent of the window without the wide-screen inset`() {
        assertEquals(691.2f, greedyBubbleWidth(), TOLERANCE)
    }

    private fun greedyBubbleWidth(): Float {
        composeTestRule.setContent {
            ChatTheme {
                MessageFrame(
                    position = MessageItemGroupState.SOLO,
                    messageContentType = ContentType.Text,
                    isAgent = true,
                    colors = chatColors.agent,
                    showFrame = true,
                ) {
                    Box(modifier = Modifier.fillMaxWidth().height(40.dp).testTag("bubble_content"))
                }
            }
        }
        composeTestRule.waitForIdle()
        return composeTestRule.onNodeWithTag("bubble_content").getUnclippedBoundsInRoot().width.value
    }

    private companion object {
        const val TOLERANCE = 0.5f
    }
}
