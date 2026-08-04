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
 *
 */

package com.nice.cxonechat.ui.composable.conversation

import android.net.Uri
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.test.assertContentDescriptionContains
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import com.nice.cxonechat.state.Configuration
import com.nice.cxonechat.ui.AbstractComponentActivityUiTest
import com.nice.cxonechat.ui.R
import com.nice.cxonechat.ui.composable.conversation.InputState.Audio
import com.nice.cxonechat.ui.composable.conversation.InputState.None
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import com.nice.cxonechat.ui.data.RequestResult
import com.nice.cxonechat.ui.util.KoinTestRule
import com.nice.cxonechat.ui.util.fakeConfiguration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import org.junit.Rule
import org.junit.Test
import org.koin.dsl.module
import kotlin.time.Duration

@Suppress(
    "LargeClass"
)
class UserInputSelectorTest : AbstractComponentActivityUiTest() {

    @get:Rule
    val koinTestRule = KoinTestRule(
        modules = listOf(
            module {
                single<Configuration?> { _ -> fakeConfiguration(isAttachmentsEnabled = true) }
            }
        )
    )

    @Test
    fun userInputSelector_displaysInDefaultState() {
        composeTestRule.setContent {
            UserInputSelector(
                onSelectorChange = {},
                sendMessageEnabled = remember { mutableStateOf(true) },
                showSendButton = remember { mutableStateOf(false) },
                onMessageSent = {},
                audioRecordingUiState = createTestAudioRecordingUiState(),
                currentInputSelector = None,
                focusRequester = remember { FocusRequester() },
                showMessageProcessing = false,
                onError = {}
            ) {
                OutlinedTextField(
                    value = "",
                    onValueChange = {},
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Should display attachment button
        composeTestRule
            .onNodeWithContentDescription(composeTestRule.activity.getString(R.string.title_attachment_picker))
            .assertIsDisplayed()

        // Should display mic (audio recorder) button
        composeTestRule
            .onNodeWithContentDescription(composeTestRule.activity.getString(R.string.record_audio_start_content_description))
            .assertIsDisplayed()
    }

    @Test
    fun userInputSelector_showsSendButtonWhenEnabled() {
        composeTestRule.setContent {
            ChatTheme {
                Surface { // Theme and surface are required to render the button with sufficient contrast, this is standard setup
                    UserInputSelector(
                        onSelectorChange = {},
                        sendMessageEnabled = remember { mutableStateOf(true) },
                        showSendButton = remember { mutableStateOf(true) },
                        onMessageSent = {},
                        audioRecordingUiState = createTestAudioRecordingUiState(),
                        currentInputSelector = None,
                        focusRequester = remember { FocusRequester() },
                        showMessageProcessing = false,
                        onError = {}
                    ) {
                        OutlinedTextField(
                            value = "Test message",
                            onValueChange = {},
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Should display send button (no audio recorder button)
        composeTestRule
            .onNodeWithContentDescription(composeTestRule.activity.getString(R.string.text_send))
            .assertIsDisplayed()
            .assertIsEnabled()
            .performClick() // Click triggers the accessibility check
    }

    @Test
    fun userInputSelector_sendButtonDisabledWhenSendingNotAllowed() {
        composeTestRule.setContent {
            ChatTheme {
                Surface { // Theme and surface are required to render the button with sufficient contrast, this is standard setup
                    UserInputSelector(
                        onSelectorChange = {},
                        sendMessageEnabled = remember { mutableStateOf(false) },
                        showSendButton = remember { mutableStateOf(true) },
                        onMessageSent = {},
                        audioRecordingUiState = createTestAudioRecordingUiState(),
                        currentInputSelector = None,
                        focusRequester = remember { FocusRequester() },
                        showMessageProcessing = false,
                        onError = {}
                    ) {
                        OutlinedTextField(
                            value = "Test message",
                            onValueChange = {},
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        composeTestRule
            .onNodeWithContentDescription(composeTestRule.activity.getString(R.string.text_send))
            .assertIsDisplayed()
            .assertIsNotEnabled()
            .tryPerformAccessibilityChecks()
    }

    @Test
    fun userInputSelector_showsSpinnerWhenProcessingMessage() {
        composeTestRule.setContent {
            ChatTheme {
                Surface { // Theme and surface are required to render the button with sufficient contrast, this is standard setup
                    UserInputSelector(
                        onSelectorChange = {},
                        sendMessageEnabled = remember { mutableStateOf(true) },
                        showSendButton = remember { mutableStateOf(true) },
                        onMessageSent = {},
                        audioRecordingUiState = createTestAudioRecordingUiState(),
                        currentInputSelector = None,
                        focusRequester = remember { FocusRequester() },
                        showMessageProcessing = true,
                        onError = {}
                    ) {
                        OutlinedTextField(
                            value = "Test message",
                            onValueChange = {},
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Should display attachment upload spinner
        composeTestRule
            .onNodeWithTag("attachment_upload_spinner")
            .assertIsDisplayed()
            .assertContentDescriptionContains(composeTestRule.activity.getString(R.string.loading))
            .tryPerformAccessibilityChecks()
    }

    @Test
    fun userInputSelector_transitionsToAttachmentState() {
        var currentState by mutableStateOf(None)
        composeTestRule.setContent {
            ChatTheme {
                Surface { // Theme and surface are required to render the button with sufficient contrast, this is standard setup
                    UserInputSelector(
                        onSelectorChange = { currentState = it },
                        sendMessageEnabled = remember { mutableStateOf(true) },
                        showSendButton = remember { mutableStateOf(false) },
                        onMessageSent = {},
                        audioRecordingUiState = createTestAudioRecordingUiState(),
                        currentInputSelector = currentState,
                        focusRequester = remember { FocusRequester() },
                        showMessageProcessing = false,
                        onError = {}
                    ) {
                        OutlinedTextField(
                            value = "",
                            onValueChange = {},
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Click attachment button
        composeTestRule
            .onNodeWithContentDescription(composeTestRule.activity.getString(R.string.title_attachment_picker))
            .performClick()
            .tryPerformAccessibilityChecks()

        // Verify state changed to Attachment
        assert(currentState == InputState.Attachment) {
            "Expected state to be Attachment but was $currentState"
        }
    }

    @Test
    fun userInputSelector_transitionsToAudioState() {
        var currentState = None
        composeTestRule.setContent {
            UserInputSelector(
                onSelectorChange = { currentState = it },
                sendMessageEnabled = remember { mutableStateOf(true) },
                showSendButton = remember { mutableStateOf(false) },
                onMessageSent = {},
                audioRecordingUiState = createTestAudioRecordingUiState(onToggleResult = true),
                currentInputSelector = currentState,
                focusRequester = remember { FocusRequester() },
                showMessageProcessing = false,
                onError = {}
            ) {
                OutlinedTextField(
                    value = "",
                    onValueChange = {},
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Click microphone button
        composeTestRule
            .onNodeWithContentDescription(composeTestRule.activity.getString(R.string.record_audio_start_content_description))
            .performClick()

        // Wait for async operation
        composeTestRule.waitForIdle()
        // Verify state changed to Audio
        assert(currentState == Audio) {
            "Expected state to be Audio but was $currentState"
        }
        // Perform accessibility checks on the audio input row
        composeTestRule.onNodeWithTag("audio_input_row").tryPerformAccessibilityChecks()
    }

    @Test
    fun userInputSelector_handlesAudioRecordingFailure() {
        var currentState = None
        var errorReceived: String? = null

        composeTestRule.setContent {
            UserInputSelector(
                onSelectorChange = { currentState = it },
                sendMessageEnabled = remember { mutableStateOf(true) },
                showSendButton = remember { mutableStateOf(false) },
                onMessageSent = {},
                audioRecordingUiState = createTestAudioRecordingUiState(onToggleResult = false),
                currentInputSelector = currentState,
                focusRequester = remember { FocusRequester() },
                showMessageProcessing = false,
                onError = { errorReceived = it }
            ) {
                OutlinedTextField(
                    value = "",
                    onValueChange = {},
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Click microphone button
        composeTestRule
            .onNodeWithContentDescription(composeTestRule.activity.getString(R.string.record_audio_start_content_description))
            .performClick()

        // Wait for async operation
        composeTestRule.waitForIdle()

        // Verify state remains None
        assert(currentState == None) {
            "Expected state to remain None but was $currentState"
        }

        // Verify error callback was called
        assert(errorReceived != null) {
            "Expected error callback to be invoked"
        }
        composeTestRule.onRoot().tryPerformAccessibilityChecks()
    }

    @Test
    fun userInputSelector_callsOnMessageSentWhenSendButtonClicked() {
        var messageSent = false
        composeTestRule.setContent {
            ChatTheme {
                Surface { // Theme and surface are required to render the button with sufficient contrast, this is standard setup
                    UserInputSelector(
                        onSelectorChange = {},
                        sendMessageEnabled = remember { mutableStateOf(true) },
                        showSendButton = remember { mutableStateOf(true) },
                        onMessageSent = { messageSent = true },
                        audioRecordingUiState = createTestAudioRecordingUiState(),
                        currentInputSelector = None,
                        focusRequester = remember { FocusRequester() },
                        showMessageProcessing = false,
                        onError = {}
                    ) {
                        OutlinedTextField(
                            value = "Test message",
                            onValueChange = {},
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Click send button
        composeTestRule
            .onNodeWithContentDescription(composeTestRule.activity.getString(R.string.text_send))
            .performClick()

        // Verify callback was invoked
        assert(messageSent) {
            "Expected onMessageSent callback to be invoked"
        }
    }

    @Test
    fun userInputSelector_maintainsStateAcrossRecomposition() {
        var currentState = None
        composeTestRule.setContent {
            var showSend by remember { mutableStateOf(false) }

            UserInputSelector(
                onSelectorChange = { currentState = it },
                sendMessageEnabled = remember { mutableStateOf(true) },
                showSendButton = remember { mutableStateOf(showSend) },
                onMessageSent = {},
                audioRecordingUiState = createTestAudioRecordingUiState(),
                currentInputSelector = currentState,
                focusRequester = remember { FocusRequester() },
                showMessageProcessing = false,
                onError = {}
            ) {
                OutlinedTextField(
                    value = if (showSend) "Message" else "",
                    onValueChange = { showSend = it.isNotEmpty() },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Type message") }
                )
            }
        }

        // Initial state - should show attachment and mic buttons
        composeTestRule
            .onNodeWithContentDescription(composeTestRule.activity.getString(R.string.title_attachment_picker))
            .assertIsDisplayed()
            .tryPerformAccessibilityChecks()

        composeTestRule
            .onNodeWithContentDescription(composeTestRule.activity.getString(R.string.record_audio_start_content_description))
            .assertIsDisplayed()
            .tryPerformAccessibilityChecks()

        assert(currentState == None) {
            "Expected initial state to be None"
        }
    }

    @Test
    fun userInputSelector_transitionsFromAttachmentBackToNone() {
        var currentState = InputState.Attachment
        composeTestRule.setContent {
            UserInputSelector(
                onSelectorChange = { currentState = it },
                sendMessageEnabled = remember { mutableStateOf(true) },
                showSendButton = remember { mutableStateOf(false) },
                onMessageSent = {},
                audioRecordingUiState = createTestAudioRecordingUiState(),
                currentInputSelector = currentState,
                focusRequester = remember { FocusRequester() },
                showMessageProcessing = false,
                onError = {}
            ) {
                OutlinedTextField(
                    value = "",
                    onValueChange = {},
                    modifier = Modifier.weight(1f)
                )
            }
        }

        assert(currentState == InputState.Attachment) {
            "Expected initial state to be Attachment"
        }

        // Verify attachment button is displayed in Attachment state
        composeTestRule
            .onNodeWithContentDescription(composeTestRule.activity.getString(R.string.title_attachment_picker))
            .assertIsDisplayed()
            .tryPerformAccessibilityChecks()

        // Update state back to None
        composeTestRule.runOnUiThread {
            currentState = None
        }

        // Verify mic button appears when back to None state
        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithContentDescription(composeTestRule.activity.getString(R.string.record_audio_start_content_description))
            .assertIsDisplayed()
            .tryPerformAccessibilityChecks()
    }

    // Helper function to create mock AudioRecordingUiState for tests
    private fun createTestAudioRecordingUiState(
        isRecording: Boolean = false,
        duration: Duration = Duration.ZERO,
        onToggleResult: Boolean = true,
    ): AudioRecordingUiState {
        return AudioRecordingUiState(
            isRecordingAllowedFlow = flowOf(true),
            uriFlow = MutableStateFlow(Uri.EMPTY),
            isRecordingFlow = MutableStateFlow(isRecording),
            durationFlow = MutableStateFlow(duration),
            onDismiss = {},
            onApprove = {},
            onAudioRecordToggle = { if (onToggleResult) RequestResult.SUCCESS else RequestResult.FAILURE },
        )
    }
}
