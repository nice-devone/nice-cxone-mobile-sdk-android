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

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.nice.cxonechat.thread.ChatThreadState
import com.nice.cxonechat.ui.composable.conversation.model.ConversationTopBarState
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalMaterial3Api::class)
class ChatThreadTopBarTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun editCustomFieldsButton_isShown_whenHasQuestionsAndNotArchived() {
        composeTestRule.setContent {
            ChatTheme {
                ChatThreadTopBar(
                    scrollBehavior = null,
                    conversationState = ConversationTopBarState(
                        threadName = flowOf("Test"),
                        isMultiThreaded = false,
                        hasQuestions = true,
                        isLiveChat = false,
                        liveChatAllowTranscript = false,
                        isArchived = MutableStateFlow(false),
                        threadState = MutableStateFlow(ChatThreadState.Ready),
                    ),
                    onEditThreadName = {},
                    onEditThreadValues = {},
                    onEndContact = {},
                    displayEndConversation = {},
                    onSendTranscript = {},
                )
            }
        }
        composeTestRule.onNodeWithTag("edit_thread_custom_values_button").assertIsDisplayed()
    }

    @Test
    fun editCustomFieldsButton_isHidden_whenHasQuestionsAndArchived() {
        composeTestRule.setContent {
            ChatTheme {
                ChatThreadTopBar(
                    scrollBehavior = null,
                    conversationState = ConversationTopBarState(
                        threadName = flowOf("Test"),
                        isMultiThreaded = false,
                        hasQuestions = true,
                        isLiveChat = false,
                        liveChatAllowTranscript = false,
                        isArchived = MutableStateFlow(true),
                        threadState = MutableStateFlow(ChatThreadState.Ready),
                    ),
                    onEditThreadName = {},
                    onEditThreadValues = {},
                    onEndContact = {},
                    displayEndConversation = {},
                    onSendTranscript = {},
                )
            }
        }
        composeTestRule.onNodeWithTag("edit_thread_custom_values_button").assertDoesNotExist()
    }

    @Test
    fun editCustomFieldsMenuItem_isHidden_whenArchivedInOverflowMenu() {
        composeTestRule.setContent {
            ChatTheme {
                ChatThreadTopBar(
                    scrollBehavior = null,
                    conversationState = ConversationTopBarState(
                        threadName = flowOf("Test"),
                        isMultiThreaded = true,
                        hasQuestions = true,
                        isLiveChat = true,
                        liveChatAllowTranscript = false,
                        isArchived = MutableStateFlow(true),
                        threadState = MutableStateFlow(ChatThreadState.Ready),
                    ),
                    onEditThreadName = {},
                    onEditThreadValues = {},
                    onEndContact = {},
                    displayEndConversation = {},
                    onSendTranscript = {},
                )
            }
        }
        composeTestRule.onNodeWithTag("chat_thread_top_bar_menu_button").performClick()
        composeTestRule.onNodeWithTag("edit_thread_custom_values_menu_item").assertDoesNotExist()
    }

    @Test
    fun editCustomFieldsMenuItem_isShown_whenNotArchivedInOverflowMenu() {
        composeTestRule.setContent {
            ChatTheme {
                ChatThreadTopBar(
                    scrollBehavior = null,
                    conversationState = ConversationTopBarState(
                        threadName = flowOf("Test"),
                        isMultiThreaded = true,
                        hasQuestions = true,
                        isLiveChat = true,
                        liveChatAllowTranscript = false,
                        isArchived = MutableStateFlow(false),
                        threadState = MutableStateFlow(ChatThreadState.Ready),
                    ),
                    onEditThreadName = {},
                    onEditThreadValues = {},
                    onEndContact = {},
                    displayEndConversation = {},
                    onSendTranscript = {},
                )
            }
        }
        composeTestRule.onNodeWithTag("chat_thread_top_bar_menu_button").performClick()
        composeTestRule.onNodeWithTag("edit_thread_custom_values_menu_item").assertIsDisplayed()
    }
}
