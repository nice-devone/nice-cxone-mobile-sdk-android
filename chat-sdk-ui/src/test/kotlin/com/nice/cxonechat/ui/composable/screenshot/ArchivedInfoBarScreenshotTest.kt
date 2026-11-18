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
 * AN "AS IS" BASIS. NICE HEREBY DISCLAIMS ALL WARRANTIES AND CONDITIONS, EXPRESS
 * OR IMPLIED, INCLUDING (WITHOUT LIMITATION) WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE, NON-INFRINGEMENT, AND TITLE.
 */

package com.nice.cxonechat.ui.composable.screenshot

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import com.nice.cxonechat.ui.composable.conversation.ChatConversation
import com.nice.cxonechat.ui.composable.conversation.previewAudioState
import com.nice.cxonechat.ui.composable.conversation.previewUiState
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(
    sdk = [33],
    qualifiers = "w411dp-h891dp-normal-notlong-notround-notnight-xhdpi-finger"
)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ArchivedInfoBarScreenshotTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `archived bar messaging thread light`() {
        composeTestRule.setContent {
            ChatTheme {
                ChatConversation(
                    conversationState = previewUiState(isArchived = true, isLiveChat = false),
                    audioRecordingState = previewAudioState(),
                    onAttachmentTypeSelection = {},
                    onError = {},
                    showMessageProcessing = false,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        composeTestRule.onRoot().captureRoboImage(roborazziOptions = screenshotRoborazziOptions)
    }

    @Test
    fun `archived bar live chat thread light`() {
        composeTestRule.setContent {
            ChatTheme {
                ChatConversation(
                    conversationState = previewUiState(isArchived = true, isLiveChat = true),
                    audioRecordingState = previewAudioState(),
                    onAttachmentTypeSelection = {},
                    onError = {},
                    showMessageProcessing = false,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        composeTestRule.onRoot().captureRoboImage(roborazziOptions = screenshotRoborazziOptions)
    }

    @Test
    @Config(qualifiers = "w411dp-h891dp-normal-notlong-notround-night-xhdpi-finger")
    fun `archived bar messaging thread dark`() {
        composeTestRule.setContent {
            ChatTheme {
                ChatConversation(
                    conversationState = previewUiState(isArchived = true, isLiveChat = false),
                    audioRecordingState = previewAudioState(),
                    onAttachmentTypeSelection = {},
                    onError = {},
                    showMessageProcessing = false,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        composeTestRule.onRoot().captureRoboImage(roborazziOptions = screenshotRoborazziOptions)
    }

    @Test
    @Config(qualifiers = "w411dp-h891dp-normal-notlong-notround-night-xhdpi-finger")
    fun `archived bar live chat thread dark`() {
        composeTestRule.setContent {
            ChatTheme {
                ChatConversation(
                    conversationState = previewUiState(isArchived = true, isLiveChat = true),
                    audioRecordingState = previewAudioState(),
                    onAttachmentTypeSelection = {},
                    onError = {},
                    showMessageProcessing = false,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        composeTestRule.onRoot().captureRoboImage(roborazziOptions = screenshotRoborazziOptions)
    }

    @Test
    @Config(qualifiers = "w891dp-h411dp-normal-notlong-notround-notnight-xhdpi-finger")
    fun `archived bar messaging thread landscape light`() {
        composeTestRule.setContent {
            ChatTheme {
                ChatConversation(
                    conversationState = previewUiState(isArchived = true, isLiveChat = false),
                    audioRecordingState = previewAudioState(),
                    onAttachmentTypeSelection = {},
                    onError = {},
                    showMessageProcessing = false,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        composeTestRule.onRoot().captureRoboImage(roborazziOptions = screenshotRoborazziOptions)
    }

    @Test
    @Config(qualifiers = "w891dp-h411dp-normal-notlong-notround-notnight-xhdpi-finger")
    fun `archived bar live chat thread landscape light`() {
        composeTestRule.setContent {
            ChatTheme {
                ChatConversation(
                    conversationState = previewUiState(isArchived = true, isLiveChat = true),
                    audioRecordingState = previewAudioState(),
                    onAttachmentTypeSelection = {},
                    onError = {},
                    showMessageProcessing = false,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        composeTestRule.onRoot().captureRoboImage(roborazziOptions = screenshotRoborazziOptions)
    }

    @Test
    @Config(qualifiers = "w891dp-h411dp-normal-notlong-notround-night-xhdpi-finger")
    fun `archived bar messaging thread landscape dark`() {
        composeTestRule.setContent {
            ChatTheme {
                ChatConversation(
                    conversationState = previewUiState(isArchived = true, isLiveChat = false),
                    audioRecordingState = previewAudioState(),
                    onAttachmentTypeSelection = {},
                    onError = {},
                    showMessageProcessing = false,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        composeTestRule.onRoot().captureRoboImage(roborazziOptions = screenshotRoborazziOptions)
    }

    @Test
    @Config(qualifiers = "w891dp-h411dp-normal-notlong-notround-night-xhdpi-finger")
    fun `archived bar live chat thread landscape dark`() {
        composeTestRule.setContent {
            ChatTheme {
                ChatConversation(
                    conversationState = previewUiState(isArchived = true, isLiveChat = true),
                    audioRecordingState = previewAudioState(),
                    onAttachmentTypeSelection = {},
                    onError = {},
                    showMessageProcessing = false,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        composeTestRule.onRoot().captureRoboImage(roborazziOptions = screenshotRoborazziOptions)
    }
}
