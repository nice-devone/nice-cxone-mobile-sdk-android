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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.github.takahirom.roborazzi.captureRoboImage
import com.nice.cxonechat.Popup
import com.nice.cxonechat.ui.composable.conversation.LocalSurveyWebViewContent
import com.nice.cxonechat.ui.composable.conversation.SatisfactionSurveyPopup
import com.nice.cxonechat.ui.composable.conversation.createPreviewSurveyPopup
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.annotation.GraphicsMode.Mode

@RunWith(RobolectricTestRunner::class)
@Config(
    sdk = [33],
    qualifiers = "w411dp-h891dp-normal-notlong-notround-notnight-xhdpi-finger"
)
@GraphicsMode(Mode.NATIVE)
class SatisfactionSurveyScreenshotTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // A real WebView doesn't render actual web content under Robolectric, so these screenshots
    // only cover the native chrome around it - substitute a solid-color fake page in its place.
    private fun fakeSurveyPage(content: @Composable () -> Unit) {
        composeTestRule.setContent {
            CompositionLocalProvider(
                LocalSurveyWebViewContent provides { _, modifier ->
                    Box(
                        modifier = modifier
                            .fillMaxSize()
                            .background(Color(0xFFB0BEC5))
                            .testTag("satisfaction_survey_web_view"),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("Fake survey page")
                    }
                },
            ) {
                content()
            }
        }
    }

    @Test
    fun `satisfaction survey dialog chrome`() {
        fakeSurveyPage {
            ChatTheme {
                SatisfactionSurveyPopup(popup = createPreviewSurveyPopup(), onDismiss = {})
            }
        }
        composeTestRule.onNode(isDialog()).captureRoboImage()
    }

    @Test
    @Config(qualifiers = "w852dp-h400dp-normal-notlong-notround-notnight-xhdpi-finger")
    fun `satisfaction survey dialog chrome landscape`() {
        fakeSurveyPage {
            ChatTheme {
                SatisfactionSurveyPopup(popup = createPreviewSurveyPopup(), onDismiss = {})
            }
        }
        composeTestRule.onNode(isDialog()).captureRoboImage()
    }

    @Test
    fun `satisfaction survey dialog chrome with a long backend prompt`() {
        val popup = object : Popup.SatisfactionSurvey {
            override val text: String = "Thank you for chatting with us today - please rate your experience."
            override val surveyUrl: String = "https://survey.example.com/s/preview-long"
        }
        fakeSurveyPage {
            ChatTheme {
                SatisfactionSurveyPopup(popup = popup, onDismiss = {})
            }
        }
        composeTestRule.onNode(isDialog()).captureRoboImage()
    }
}
