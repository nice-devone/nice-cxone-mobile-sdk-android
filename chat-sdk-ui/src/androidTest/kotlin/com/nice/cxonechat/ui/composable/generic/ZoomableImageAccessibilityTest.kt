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

package com.nice.cxonechat.ui.composable.generic

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nice.cxonechat.ui.AbstractComponentActivityUiTest
import com.nice.cxonechat.ui.R.string
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Accessibility tests for [ZoomableImage].
 */
@RunWith(AndroidJUnit4::class)
class ZoomableImageAccessibilityTest : AbstractComponentActivityUiTest() {

    /**
     * Test that the full-screen image viewer announces zoom gesture instructions to TalkBack.
     *
     * Passes null as the image model so no image loading occurs —
     * the stateDescription semantic is set at composition time regardless.
     */
    @Test
    fun zoomableImage_hasZoomHintStateDescription() {
        composeTestRule.setContent {
            ChatTheme {
                ZoomableImage(
                    image = null,
                    contentDescription = "Test image",
                )
            }
        }

        val expectedHint = getString(string.content_description_zoomable_image_hint)
        val matcher = SemanticsMatcher("has stateDescription '$expectedHint'") { node ->
            runCatching { node.config[SemanticsProperties.StateDescription] }.getOrNull() == expectedHint
        }
        composeTestRule.onNode(matcher).assertExists()
            .tryPerformAccessibilityChecks()
    }
}
