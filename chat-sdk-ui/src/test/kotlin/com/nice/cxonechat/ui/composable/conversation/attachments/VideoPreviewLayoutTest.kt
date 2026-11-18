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

package com.nice.cxonechat.ui.composable.conversation.attachments

import android.content.Context
import android.graphics.Color
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import androidx.test.core.app.ApplicationProvider
import coil3.ColorImage
import coil3.ImageLoader
import coil3.annotation.ExperimentalCoilApi
import coil3.test.FakeImageLoaderEngine
import com.nice.cxonechat.ui.composable.conversation.PreviewAttachments
import com.nice.cxonechat.ui.composable.generic.ThumbnailSize
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Pins how [VideoPreview] sizes its thumbnail. Appearance is covered by `VideoPreviewScreenshotTest` instead. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], qualifiers = "w852dp-h412dp-normal-notlong-notround-notnight-xhdpi-finger")
internal class VideoPreviewLayoutTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    /** Answers every request with the same colour, so no frame is ever decoded. */
    @OptIn(ExperimentalCoilApi::class)
    private val stubbedImageLoader: ImageLoader by lazy {
        val engine = FakeImageLoaderEngine.Builder().default(ColorImage(Color.CYAN)).build()
        ImageLoader.Builder(ApplicationProvider.getApplicationContext<Context>())
            .components { add(engine) }
            .build()
    }

    /** The staged attachment row animates the tile between its sizes, so the thumbnail has to follow the tile. */
    @Test
    fun `a compact thumbnail fills the slot it is given`() {
        composeTestRule.setContent {
            ChatTheme {
                Box(modifier = Modifier.size(SLOT)) {
                    VideoPreview(
                        attachment = PreviewAttachments.movie,
                        messageId = null,
                        playIndicator = PlayIndicator.STANDARD,
                        isGroupAttachment = true,
                        imageLoader = stubbedImageLoader,
                        thumbnailSize = ThumbnailSize.COMPACT,
                        contentDescription = DESCRIPTION,
                    )
                }
            }
        }

        val thumbnail = composeTestRule
            .onNodeWithContentDescription(DESCRIPTION, useUnmergedTree = true)
            .getUnclippedBoundsInRoot()
        assertEquals(SLOT.value, thumbnail.width.value, TOLERANCE)
        assertEquals(SLOT.value, thumbnail.height.value, TOLERANCE)
    }

    private companion object {
        /** Any size other than an eighth of the window's shorter edge, which a compact tile rests at. */
        val SLOT = 80.dp
        const val DESCRIPTION = "video"
        const val TOLERANCE = 0.5f
    }
}
