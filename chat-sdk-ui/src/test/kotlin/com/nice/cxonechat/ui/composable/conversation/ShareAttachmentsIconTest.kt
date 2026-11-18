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

import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Pins the share glyph on an attachment in a conversation to its design size. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
internal class ShareAttachmentsIconTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    /**
     * An icon defaults to 24dp, which crowds the design's 34dp button.
     *
     * Measured on the glyph, not the button: the button's bounds report its 48dp touch target either way.
     */
    @Test
    fun `the share glyph is drawn well inside its button`() {
        composeTestRule.setContent {
            ChatTheme {
                ShareAttachmentsIcon(contentDescription = DESCRIPTION) {}
            }
        }

        val glyph = composeTestRule
            .onNodeWithContentDescription(DESCRIPTION, useUnmergedTree = true)
            .getUnclippedBoundsInRoot()

        assertEquals(16f, glyph.width.value, TOLERANCE)
        assertEquals(16f, glyph.height.value, TOLERANCE)
    }

    private companion object {
        const val DESCRIPTION = "share"
        const val TOLERANCE = 0.5f
    }
}
