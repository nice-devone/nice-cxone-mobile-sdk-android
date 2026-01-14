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

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented test for TypingIndicator composable.
 * Verifies that TypingIndicator is visible and animated.
 *
 * Doesn't verify specific animation frames, just that the image changes over time.
 */
@RunWith(AndroidJUnit4::class)
class TypingIndicatorTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun typingIndicator_isVisible_andAnimated() {
        // Pause animations
        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.setContent {
            ChatTheme {
                Surface(
                    modifier = Modifier
                        .systemBarsPadding()
                        .fillMaxSize()
                ) {
                    TypingIndicator()
                }
            }
        }

        // Assert TypingIndicator is visible
        composeTestRule.onNodeWithTag("TypingIndicator").assertIsDisplayed()

        // Capture initial image of the TypingIndicator
        val initialImage = composeTestRule.onNodeWithTag("TypingIndicator").captureToImage()

        // Wait for animation to progress
        composeTestRule.mainClock.advanceTimeBy(500) // Wait for animation to change (animationDuration is 250ms)

        // Capture image after animation
        val animatedImage = composeTestRule.onNodeWithTag("TypingIndicator").captureToImage()
        // Assert that the images are different, indicating animation
        assertFalse(
            "TypingIndicator should be animated (images should differ)",
            initialImage.toPixelMap().buffer.contentEquals(animatedImage.toPixelMap().buffer)
        )
    }
}

