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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteractionCollection
import androidx.compose.ui.test.assertContentDescriptionContains
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.nice.cxonechat.message.MessageAuthor
import com.nice.cxonechat.message.MessageDirection.ToAgent
import com.nice.cxonechat.message.MessageDirection.ToClient
import com.nice.cxonechat.ui.AbstractComponentActivityUiTest
import com.nice.cxonechat.ui.R
import com.nice.cxonechat.ui.composable.conversation.model.Message
import com.nice.cxonechat.ui.composable.conversation.model.PreviewMessageProvider
import com.nice.cxonechat.ui.composable.conversation.model.Section
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import com.nice.cxonechat.ui.domain.model.Person
import com.nice.cxonechat.ui.util.preview.message.UiSdkText
import org.junit.Test

class MessagesUiTest : AbstractComponentActivityUiTest() {
    private fun mockSections(): List<Section> {
        val resources = InstrumentationRegistry.getInstrumentation().targetContext.resources
        return PreviewMessageProvider()
            .values
            .take(3)
            .groupBy { it.createdAtDate(resources) }
            .entries
            .map(::Section)
    }

    @Test
    fun messages_displaysMessageItems() {
        val sections = mockSections()
        composeTestRule.setContent {
            Column {
                Messages(
                    scrollState = rememberLazyListState(),
                    groupedMessages = sections,
                    loadMore = {},
                    canLoadMore = false,
                    agentIsTyping = false,
                    agentDetails = null,
                    onAttachmentClicked = {},
                    onMoreClicked = {},
                    onShare = {},
                    modifier = Modifier,
                    snackBarHostState = SnackbarHostState(),
                    onTimePickerSelected = { _, _ -> },
                )
            }
        }
        composeTestRule.onNodeWithTag("messages").assertIsDisplayed()
        composeTestRule.onAllNodesWithTagPrefix("message_item_")[0].assertExists()
    }

    @Test
    fun messages_agentTyping_showsTypingIndicator() {
        val sections = mockSections()
        val agent = Person(id = "agent1", firstName = "Agent", lastName = "Bot", imageUrl = null)
        composeTestRule.setContent {
            Column {
                Messages(
                    scrollState = rememberLazyListState(),
                    groupedMessages = sections,
                    loadMore = {},
                    canLoadMore = false,
                    agentIsTyping = true,
                    agentDetails = agent,
                    onAttachmentClicked = {},
                    onMoreClicked = {},
                    onShare = {},
                    modifier = Modifier,
                    snackBarHostState = SnackbarHostState(),
                    onTimePickerSelected = { _, _ -> },
                )
            }
        }
        composeTestRule.onNodeWithTag("TypingIndicator", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun messages_canLoadMore_showsLoadMore() {
        val sections = mockSections()
        composeTestRule.setContent {
            Column {
                Messages(
                    scrollState = rememberLazyListState(),
                    groupedMessages = sections,
                    loadMore = {},
                    canLoadMore = true,
                    agentIsTyping = false,
                    agentDetails = null,
                    onAttachmentClicked = {},
                    onMoreClicked = {},
                    onShare = {},
                    modifier = Modifier,
                    snackBarHostState = SnackbarHostState(),
                    onTimePickerSelected = { _, _ -> },
                )
            }
        }
        composeTestRule.onNodeWithTag("Load_More", useUnmergedTree = true).assertExists()
    }

    @Test
    fun textMessage_fromUser_hasAccessibilityDescription() {
        val testMessage = "Hello, this is a test message"
        val message = Message.Text(
            UiSdkText(
                text = testMessage,
                direction = ToAgent
            )
        )

        composeTestRule.setContent {
            ChatTheme {
                TextMessage(message = message, modifier = Modifier)
            }
        }

        val node = composeTestRule.onNodeWithTag("text_message")
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val expectedPrefix = context.getString(R.string.accessibility_message_you).substringBefore($$"%1$s")
        node.assertExists()
        node.assertContentDescriptionContains(value = expectedPrefix, substring = true)
        node.assertContentDescriptionContains(value = testMessage, substring = true)
    }

    @Test
    fun textMessage_fromAgent_hasAccessibilityDescription() {
        val testMessage = "Hello from agent"
        val agentName = "Agent Smith"
        val message = Message.Text(
            UiSdkText(
                text = testMessage,
                direction = ToClient,
                author = object : MessageAuthor() {
                    override val id = "agent1"
                    override val firstName = "Agent"
                    override val lastName = "Smith"
                    override val imageUrl: String? = null
                }
            )
        )

        composeTestRule.setContent {
            ChatTheme {
                TextMessage(message = message, modifier = Modifier)
            }
        }

        val node = composeTestRule.onNodeWithTag("text_message")
        node.assertExists()
        node.assertContentDescriptionContains(value = testMessage, substring = true)
        node.assertContentDescriptionContains(value = agentName, substring = true)
    }

    @Test
    fun emojiMessage_fromUser_hasAccessibilityDescription() {
        val testEmoji = "😎📱🇨🇿"
        val message = Message.EmojiText(
            UiSdkText(
                text = testEmoji,
                direction = ToAgent
            )
        )

        composeTestRule.setContent {
            ChatTheme {
                EmojiMessage(message = message, paddingValues = PaddingValues(8.dp))
            }
        }

        val node = composeTestRule.onNodeWithTag("emoji_message")
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val expectedPrefix = context.getString(R.string.accessibility_message_you).substringBefore($$"%1$s")
        node.assertExists()
        node.assertContentDescriptionContains(value = expectedPrefix, substring = true)
        node.assertContentDescriptionContains(value = testEmoji, substring = true)
    }

    @Test
    fun emojiMessage_fromAgent_hasAccessibilityDescription() {
        val testEmoji = "👍😊"
        val agentName = "Support Agent"
        val message = Message.EmojiText(
            UiSdkText(
                text = testEmoji,
                direction = ToClient,
                author = object : MessageAuthor() {
                    override val id = "agent2"
                    override val firstName = "Support"
                    override val lastName = "Agent"
                    override val imageUrl: String? = null
                }
            )
        )

        composeTestRule.setContent {
            ChatTheme {
                EmojiMessage(message = message, paddingValues = PaddingValues(8.dp))
            }
        }

        val node = composeTestRule.onNodeWithTag("emoji_message")
        node.assertExists()
        node.assertContentDescriptionContains(value = testEmoji, substring = true)
        node.assertContentDescriptionContains(value = agentName, substring = true)
    }

    @Test
    fun textMessage_hasTimestamp_inAccessibilityDescription() {
        val testMessage = "Check the time"
        val message = Message.Text(
            UiSdkText(
                text = testMessage,
                direction = ToAgent
            )
        )

        composeTestRule.setContent {
            ChatTheme {
                TextMessage(message = message, modifier = Modifier)
            }
        }

        val node = composeTestRule.onNodeWithTag("text_message")
        val resources = InstrumentationRegistry.getInstrumentation().targetContext.resources
        val expectedTimestamp = message.createdAtDate(resources)

        node.assertContentDescriptionContains(value = testMessage, substring = true)
        node.assertContentDescriptionContains(value = expectedTimestamp, substring = true)
    }

    private fun ComposeTestRule.onAllNodesWithTagPrefix(prefix: String): SemanticsNodeInteractionCollection {
        return onAllNodes(hasTestTagStartingWith(prefix), useUnmergedTree = true)
    }

    private fun hasTestTagStartingWith(prefix: String): SemanticsMatcher =
        SemanticsMatcher("TestTag starts with $prefix") { node ->
            node.config.getOrNull(SemanticsProperties.TestTag)?.startsWith(prefix) == true
        }
}
