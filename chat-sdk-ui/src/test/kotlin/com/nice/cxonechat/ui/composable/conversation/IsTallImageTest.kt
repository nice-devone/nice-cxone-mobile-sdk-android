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

import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Pins the tall/wide classification that drives the landscape rich-link layout: a tall image fills
 * the card width (ContentScale.FillWidth) while a wide or square image keeps its natural size
 * (ContentScale.Fit). The screenshot baselines only assert the resulting pixels; this asserts the
 * decision itself, so a regression fails with a readable message instead of a soft image diff.
 */
internal class IsTallImageTest {

    @Test
    fun `classifies a portrait image as tall`() {
        // Same dimensions the landscape-tall screenshot fixture uses.
        assertTrue(isTallImage(width = 600, height = 1200))
    }

    @Test
    fun `classifies a landscape image as wide`() {
        // Same dimensions the landscape-small screenshot fixture uses.
        assertFalse(isTallImage(width = 200, height = 170))
    }

    @Test
    fun `classifies a square image as wide`() {
        assertFalse(isTallImage(width = 500, height = 500))
    }

    @Test
    fun `classifies an image one pixel taller than wide as tall`() {
        assertTrue(isTallImage(width = 500, height = 501))
    }
}
