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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteractionCollection
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.platform.app.InstrumentationRegistry
import com.nice.cxonechat.ui.composable.conversation.model.PreviewMessageProvider
import com.nice.cxonechat.ui.composable.conversation.model.Section
import com.nice.cxonechat.ui.domain.model.Person
import org.junit.Rule
import org.junit.Test

class MessagesUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun mockSections(): List<Section> {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        return PreviewMessageProvider()
            .values
            .take(3)
            .groupBy { it.createdAtDate(context) }
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
                )
            }
        }
        composeTestRule.onNodeWithContentDescription("Agent is typing…").assertExists()
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
                )
            }
        }
        composeTestRule.onNodeWithTag("Load_More").assertExists()
    }

    private fun ComposeTestRule.onAllNodesWithTagPrefix(prefix: String): SemanticsNodeInteractionCollection {
        return onAllNodes(hasTestTagStartingWith(prefix))
    }

    private fun hasTestTagStartingWith(prefix: String): SemanticsMatcher =
        SemanticsMatcher("TestTag starts with $prefix") { node ->
            node.config.getOrNull(SemanticsProperties.TestTag)?.startsWith(prefix) == true
        }
}
