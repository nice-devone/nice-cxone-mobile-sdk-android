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

import androidx.compose.ui.unit.dp
import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Pins the crop decision that drives the portrait rich-link layout: an image whose aspect ratio
 * would overflow the height cap once scaled to the card width switches to ContentScale.Crop instead
 * of the default ContentScale.FillWidth. This asserts the decision itself, independent of the
 * rendered pixels.
 */
internal class ExceedsHeightCapTest {

    @Test
    fun `small image scaled to card width fits within the height cap`() {
        // 200x100 (2:1) scaled to a 300dp-wide card is 150dp tall, under the 200dp cap.
        assertFalse(exceedsHeightCap(imageWidth = 200, imageHeight = 100, maxWidth = 300.dp, maxHeight = 200.dp))
    }

    @Test
    fun `tall image scaled to card width overflows the height cap`() {
        assertTrue(exceedsHeightCap(imageWidth = 600, imageHeight = 1200, maxWidth = 300.dp, maxHeight = 200.dp))
    }

    @Test
    fun `image whose scaled height exactly matches the cap does not overflow`() {
        // 300dp wide card, 2:1 aspect ratio image scales to exactly 150dp tall against a 150dp cap.
        assertFalse(exceedsHeightCap(imageWidth = 600, imageHeight = 300, maxWidth = 300.dp, maxHeight = 150.dp))
    }

    @Test
    fun `image one pixel taller than the exact cap match overflows`() {
        assertTrue(exceedsHeightCap(imageWidth = 600, imageHeight = 301, maxWidth = 300.dp, maxHeight = 150.dp))
    }
}
