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
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.nice.cxonechat.thread.ChatThreadState
import com.nice.cxonechat.ui.AbstractComponentActivityUiTest
import com.nice.cxonechat.ui.R.string
import com.nice.cxonechat.ui.composable.conversation.model.ConversationTopBarState
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalMaterial3Api::class)
class ChatThreadTopBarActionsTest : AbstractComponentActivityUiTest() {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Suppress(
        "LongParameterList" // Test helper
    )
    private fun createConversationState(
        isMultiThreaded: Boolean = false,
        hasQuestions: Boolean = false,
        isLiveChat: Boolean = false,
        liveChatAllowTranscript: Boolean = false,
        isArchived: Boolean = false,
        threadState: ChatThreadState = ChatThreadState.Ready,
    ) = ConversationTopBarState(
        threadName = flowOf("Test Thread"),
        isMultiThreaded = isMultiThreaded,
        hasQuestions = hasQuestions,
        isLiveChat = isLiveChat,
        liveChatAllowTranscript = liveChatAllowTranscript,
        isArchived = MutableStateFlow(isArchived),
        threadState = MutableStateFlow(threadState),
    )

    @Test
    fun multipleActions_showsMenuButton_whenMultipleOptionsAvailable() {
        composeTestRule.setContent {
            ChatTheme {
                ChatThreadTopBar(
                    scrollBehavior = null,
                    conversationState = createConversationState(
                        isMultiThreaded = true,
                        hasQuestions = true
                    ),
                    onEditThreadName = {},
                    onEditThreadValues = {},
                    onEndContact = {},
                    displayEndConversation = {},
                    onSendTranscript = {},
                )
            }
        }

        val expectedContentDescription = context.getString(string.livechat_conversation_options)
        composeTestRule
            .onNodeWithTag("chat_thread_top_bar_menu_button")
            .assertIsDisplayed()
            .assertHasClickAction()

        composeTestRule
            .onNodeWithContentDescription(expectedContentDescription)
            .assertIsDisplayed()
    }

    @Test
    fun multipleActions_menuButton_opensDropdownMenu() {
        composeTestRule.setContent {
            ChatTheme {
                ChatThreadTopBar(
                    scrollBehavior = null,
                    conversationState = createConversationState(
                        isMultiThreaded = true,
                        hasQuestions = true
                    ),
                    onEditThreadName = {},
                    onEditThreadValues = {},
                    onEndContact = {},
                    displayEndConversation = {},
                    onSendTranscript = {},
                )
            }
        }

        // Click menu button
        composeTestRule
            .onNodeWithTag("chat_thread_top_bar_menu_button")
            .performClick()

        // Verify menu is displayed
        composeTestRule
            .onNodeWithTag("chat_thread_top_bar_menu")
            .assertIsDisplayed()
    }

    @Test
    fun multipleActions_showsChangeThreadNameMenu_whenMultiThreaded() {
        composeTestRule.setContent {
            ChatTheme {
                ChatThreadTopBar(
                    scrollBehavior = null,
                    conversationState = createConversationState(
                        isMultiThreaded = true,
                        hasQuestions = true
                    ),
                    onEditThreadName = {},
                    onEditThreadValues = {},
                    onEndContact = {},
                    displayEndConversation = {},
                    onSendTranscript = {},
                )
            }
        }

        // Open menu
        composeTestRule
            .onNodeWithTag("chat_thread_top_bar_menu_button")
            .performClick()

        // Verify menu item is displayed
        composeTestRule
            .onNodeWithTag("change_thread_name_menu_item")
            .assertIsDisplayed()
            .assertHasClickAction()
    }

    @Test
    fun multipleActions_showsEditThreadValuesMenu_whenHasQuestions() {
        composeTestRule.setContent {
            ChatTheme {
                ChatThreadTopBar(
                    scrollBehavior = null,
                    conversationState = createConversationState(
                        isMultiThreaded = true,
                        hasQuestions = true
                    ),
                    onEditThreadName = {},
                    onEditThreadValues = {},
                    onEndContact = {},
                    displayEndConversation = {},
                    onSendTranscript = {},
                )
            }
        }

        // Open menu
        composeTestRule
            .onNodeWithTag("chat_thread_top_bar_menu_button")
            .performClick()

        // Verify menu item is displayed
        composeTestRule
            .onNodeWithTag("edit_thread_custom_values_menu_item")
            .assertIsDisplayed()
            .assertHasClickAction()
    }

    @Test
    fun multipleActions_showsSendTranscriptMenu_whenLiveChatAllowsTranscript() {
        composeTestRule.setContent {
            ChatTheme {
                ChatThreadTopBar(
                    scrollBehavior = null,
                    conversationState = createConversationState(
                        isMultiThreaded = true,
                        liveChatAllowTranscript = true
                    ),
                    onEditThreadName = {},
                    onEditThreadValues = {},
                    onEndContact = {},
                    displayEndConversation = {},
                    onSendTranscript = {},
                )
            }
        }

        // Open menu
        composeTestRule
            .onNodeWithTag("chat_thread_top_bar_menu_button")
            .performClick()

        // Verify menu item is displayed
        composeTestRule
            .onNodeWithTag("send_transcript_menu_item")
            .assertIsDisplayed()
            .assertHasClickAction()
    }

    @Test
    fun multipleActions_showsEndConversationMenu_whenLiveChat_notArchived() {
        composeTestRule.setContent {
            ChatTheme {
                ChatThreadTopBar(
                    scrollBehavior = null,
                    conversationState = createConversationState(
                        isMultiThreaded = true,
                        isLiveChat = true,
                        isArchived = false,
                        threadState = ChatThreadState.Ready
                    ),
                    onEditThreadName = {},
                    onEditThreadValues = {},
                    onEndContact = {},
                    displayEndConversation = {},
                    onSendTranscript = {},
                )
            }
        }

        // Open menu
        composeTestRule
            .onNodeWithTag("chat_thread_top_bar_menu_button")
            .performClick()

        // Verify menu item is displayed
        composeTestRule
            .onNodeWithTag("end_conversation_menu_item")
            .assertIsDisplayed()
            .assertHasClickAction()
    }

    @Test
    fun multipleActions_showsArchivedThreadMenu_whenLiveChat_archived() {
        composeTestRule.setContent {
            ChatTheme {
                ChatThreadTopBar(
                    scrollBehavior = null,
                    conversationState = createConversationState(
                        isMultiThreaded = true,
                        isLiveChat = true,
                        isArchived = true
                    ),
                    onEditThreadName = {},
                    onEditThreadValues = {},
                    onEndContact = {},
                    displayEndConversation = {},
                    onSendTranscript = {},
                )
            }
        }

        // Open menu
        composeTestRule
            .onNodeWithTag("chat_thread_top_bar_menu_button")
            .performClick()

        // Verify menu item is displayed
        composeTestRule
            .onNodeWithTag("show_archived_thread_menu_item")
            .assertIsDisplayed()
            .assertHasClickAction()
    }

    @Test
    fun multipleActions_menuItems_triggerCallbacks_andCloseMenu() {
        var editThreadNameClicked = false
        composeTestRule.setContent {
            ChatTheme {
                ChatThreadTopBar(
                    scrollBehavior = null,
                    conversationState = createConversationState(
                        isMultiThreaded = true,
                        hasQuestions = true
                    ),
                    onEditThreadName = { editThreadNameClicked = true },
                    onEditThreadValues = {},
                    onEndContact = {},
                    displayEndConversation = {},
                    onSendTranscript = {},
                )
            }
        }

        // Open menu
        composeTestRule
            .onNodeWithTag("chat_thread_top_bar_menu_button")
            .performClick()

        // Click menu item
        composeTestRule
            .onNodeWithTag("change_thread_name_menu_item")
            .performClick()

        // Verify callback was triggered
        assertTrue(editThreadNameClicked)

        // Menu should be closed - verify it's not displayed
        composeTestRule
            .onNodeWithTag("change_thread_name_menu_item")
            .assertDoesNotExist()
    }

    @Test
    fun accessibility_allMenuItems_haveProperTextLabels() {
        composeTestRule.setContent {
            ChatTheme {
                ChatThreadTopBar(
                    scrollBehavior = null,
                    conversationState = createConversationState(
                        isMultiThreaded = true,
                        hasQuestions = true,
                        isLiveChat = true,
                        liveChatAllowTranscript = true,
                        threadState = ChatThreadState.Ready
                    ),
                    onEditThreadName = {},
                    onEditThreadValues = {},
                    onEndContact = {},
                    displayEndConversation = {},
                    onSendTranscript = {},
                )
            }
        }

        // Open menu
        composeTestRule
            .onNodeWithTag("chat_thread_top_bar_menu_button")
            .performClick()

        // Verify all menu items have accessible text labels
        val changeThreadNameText = context.getString(string.change_thread_name)
        val changeDetailsText = context.getString(string.change_details_label)
        val sendTranscriptText = context.getString(string.send_transcript)
        val endConversationText = context.getString(string.action_end_conversation)

        // All text should be readable by screen readers
        composeTestRule.onNodeWithText(changeThreadNameText).assertIsDisplayed()
        composeTestRule.onNodeWithText(changeDetailsText).assertIsDisplayed()
        composeTestRule.onNodeWithText(sendTranscriptText).assertIsDisplayed()
        composeTestRule.onNodeWithText(endConversationText).assertIsDisplayed()
    }

    @Test
    fun accessibility_endConversationMenuItem_announcesDisabledState() {
        composeTestRule.setContent {
            ChatTheme {
                ChatThreadTopBar(
                    scrollBehavior = null,
                    conversationState = createConversationState(
                        isLiveChat = true,
                        threadState = ChatThreadState.Pending
                    ),
                    onEditThreadName = {},
                    onEditThreadValues = {},
                    onEndContact = {},
                    displayEndConversation = {},
                    onSendTranscript = {},
                )
            }
        }

        // Open menu
        composeTestRule
            .onNodeWithTag("chat_thread_top_bar_menu_button")
            .performClick()

        // Menu item should be visible but disabled
        composeTestRule
            .onNodeWithTag("end_conversation_menu_item")
            .assertIsDisplayed()
            .assertIsNotEnabled()
    }
}
