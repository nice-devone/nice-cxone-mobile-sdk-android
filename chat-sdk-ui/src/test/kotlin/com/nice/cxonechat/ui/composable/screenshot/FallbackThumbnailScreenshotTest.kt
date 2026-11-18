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

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
import com.nice.cxonechat.ui.composable.generic.FallbackThumbnail
import com.nice.cxonechat.ui.composable.generic.ThumbnailSize
import com.nice.cxonechat.ui.composable.theme.ChatColors
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import com.nice.cxonechat.ui.composable.theme.LocalMessageColors
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Appearance of the file tile at the two sizes a message and the picker sheet draw it, on its own rather than
 * inside either.
 *
 * Each size is captured against the backdrop its own caller paints, and the two differ: the regular size sits
 * on the conversation's default background, which is what a message frame drawn transparent reveals, while the
 * large one sits on the subtler surface the picker sheet nests its grid inside rather than on the sheet's own
 * container.
 *
 * Both themes for the cases with no message, because every tone involved is a theme token and the tile's own
 * inverts between them.
 *
 * The regular size reads [LocalMessageColors] for its backdrop alone — its frame and glyph take the tile's own
 * design token — and the large size reads nothing from the message at all, which is what capturing it both
 * with and without one pins. That size is where the picker's grid draws, and what a PDF shows until its first
 * page has rendered, which can be a message whose bubble is painted as easily as one drawn transparent. A page
 * which fails outright closes, and a closed page measures zero height, so it is the wait rather than the
 * failure that puts this tile in front of anyone.
 */
@RunWith(RobolectricTestRunner::class)
@Config(
    sdk = [33],
    qualifiers = "w411dp-h891dp-normal-notlong-notround-notnight-xhdpi-finger"
)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class FallbackThumbnailScreenshotTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    /**
     * The unrendered-PDF case. Indistinguishable from the picker's capture below, which is the point: the
     * large tile takes the same tint either way, reading nothing from the message around it.
     */
    @Test
    fun `large file thumbnail in a customer message`() {
        captureThumbnail(ThumbnailSize.LARGE, senderColors = { ChatTheme.chatColors.customer })
    }

    /** The picker's case, which has no message around it at all. */
    @Test
    fun `large file thumbnail with no message`() {
        captureThumbnail(ThumbnailSize.LARGE, senderColors = { null })
    }

    @Test
    @Config(qualifiers = DARK_QUALIFIERS)
    fun `large file thumbnail with no message dark`() {
        captureThumbnail(ThumbnailSize.LARGE, senderColors = { null })
    }

    @Test
    fun `regular file thumbnail in a customer message`() {
        captureThumbnail(ThumbnailSize.REGULAR, senderColors = { ChatTheme.chatColors.customer })
    }

    /**
     * The size a message's own tile draws, with nothing behind it. No message means no attachment, so only the
     * Studio preview reaches this pairing, and there the tile is merely faint. In this capture it vanishes: the
     * backdrop is the very tone frame and glyph take. The design draws no neutral regular tile — that is the
     * large size, a component of its own.
     */
    @Test
    fun `regular file thumbnail with no message`() {
        captureThumbnail(ThumbnailSize.REGULAR, senderColors = { null })
    }

    @Test
    @Config(qualifiers = DARK_QUALIFIERS)
    fun `regular file thumbnail with no message dark`() {
        captureThumbnail(ThumbnailSize.REGULAR, senderColors = { null })
    }

    private fun captureThumbnail(
        thumbnailSize: ThumbnailSize,
        senderColors: @Composable () -> ChatColors.ColorPair?,
    ) {
        composeTestRule.setContent {
            ChatTheme {
                Surface(
                    // What each size is actually drawn over. The picker nests its grid in a surface of its
                    // own inside the sheet, so the large tile never meets the sheet's container; the regular
                    // one meets the conversation's background wherever the frame is drawn transparent.
                    color = when (thumbnailSize) {
                        ThumbnailSize.LARGE -> ChatTheme.chatColors.token.background.surface.subtle
                        else -> ChatTheme.chatColors.token.background.default
                    },
                    modifier = Modifier.testTag(TILE_TAG),
                ) {
                    CompositionLocalProvider(LocalMessageColors provides senderColors()) {
                        FallbackThumbnail(
                            uri = FILE_URI,
                            contentDescription = null,
                            modifier = Modifier.padding(SURROUND),
                            mimeType = FILE_MIME_TYPE,
                            thumbnailSize = thumbnailSize,
                        )
                    }
                }
            }
        }

        // The extension resolves off the main thread and the tile draws a placeholder until it lands, so
        // idleness alone would capture that placeholder.
        composeTestRule.waitUntil(timeoutMillis = SETTLE_TIMEOUT_MILLIS) {
            composeTestRule.onAllNodesWithText(FILE_EXTENSION_LABEL).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithTag(TILE_TAG).captureRoboImage(roborazziOptions = screenshotRoborazziOptions)
    }

    private companion object {
        const val DARK_QUALIFIERS = "w411dp-h891dp-normal-notlong-notround-night-xhdpi-finger"

        const val TILE_TAG = "fallback_thumbnail_under_test"

        const val FILE_URI = "https://example.com/report.txt"

        const val FILE_MIME_TYPE = "text/plain"

        /** What the mime type above resolves to, drawn on the tile. */
        const val FILE_EXTENSION_LABEL = "TXT"

        /** Keeps the tile's own edges off the capture's border, so its backdrop is visible as an area. */
        val SURROUND = 8.dp
    }
}
