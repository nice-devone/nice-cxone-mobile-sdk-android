/*
 * Copyright (c) 2021-2025. NICE Ltd. All rights reserved.
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

import android.content.Context
import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.emoji2.bundled.BundledEmojiCompatConfig
import androidx.emoji2.text.EmojiCompat
import androidx.test.core.app.ApplicationProvider
import com.nice.cxonechat.ui.composable.conversation.model.PreviewMessageProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class ChatConversationUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Before
    fun initEmojiCompat() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        EmojiCompat.init(BundledEmojiCompatConfig(context, Dispatchers.IO.asExecutor()))
    }

    @Test
    fun chatConversation_displaysMessageListAndInput() {
        val messages = PreviewMessageProvider().messages.toList()
        composeTestRule.setContent {
            ChatConversation(
                conversationState = previewUiState(messages, positionInQueue = 1),
                audioRecordingState = previewAudioState(),
                onAttachmentTypeSelection = {},
                showMessageProcessing = false,
                onError = {},
                modifier = Modifier,
                snackBarHostState = SnackbarHostState()
            )
        }
        composeTestRule.onNodeWithTag("chat_conversation_column").assertIsDisplayed()
        composeTestRule.onNodeWithTag("user_input").assertIsDisplayed()
    }

    @Test
    fun chatConversation_archived_showsArchivedInfo() {
        composeTestRule.setContent {
            ChatConversation(
                conversationState = previewUiState(isArchived = true),
                audioRecordingState = previewAudioState(),
                onAttachmentTypeSelection = {},
                onError = {},
                showMessageProcessing = false,
                modifier = Modifier,
                snackBarHostState = SnackbarHostState()
            )
        }
        composeTestRule.onNodeWithTag("archived_info_box").assertIsDisplayed()
        composeTestRule.onNodeWithTag("archive_icon").assertIsDisplayed()
        composeTestRule.onNodeWithTag("archived_info_row").assertIsDisplayed()
    }
}
