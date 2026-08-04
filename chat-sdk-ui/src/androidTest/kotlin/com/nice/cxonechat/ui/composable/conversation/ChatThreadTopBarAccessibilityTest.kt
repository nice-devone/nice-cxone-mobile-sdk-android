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
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.MediumTest
import com.nice.cxonechat.thread.ChatThreadState
import com.nice.cxonechat.ui.AbstractComponentActivityUiTest
import com.nice.cxonechat.ui.R.string
import com.nice.cxonechat.ui.composable.conversation.model.ConversationTopBarState
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Accessibility tests for [ChatThreadTopBar].
 *
 * Verifies that all interactive icon buttons have proper content descriptions in SingleAction
 * mode, that the title announces changes via liveRegion, and that content descriptions are
 * not duplicated (which would cause TalkBack to read the same text twice).
 */
@OptIn(ExperimentalMaterial3Api::class)
@RunWith(AndroidJUnit4::class)
@LargeTest
class ChatThreadTopBarAccessibilityTest : AbstractComponentActivityUiTest() {

    @Suppress(
        "LongParameterList" // Required for testing
    )
    private fun buildState(
        isMultiThreaded: Boolean = false,
        hasQuestions: Boolean = false,
        isLiveChat: Boolean = false,
        liveChatAllowTranscript: Boolean = false,
        isArchived: Boolean = false,
        threadState: ChatThreadState = ChatThreadState.Ready,
        agentName: String? = null,
        hasAiMessages: StateFlow<Boolean> = MutableStateFlow(false).asStateFlow(),
    ) = ConversationTopBarState(
        threadName = flowOf("Test thread"),
        isMultiThreaded = isMultiThreaded,
        hasQuestions = hasQuestions,
        isLiveChat = isLiveChat,
        liveChatAllowTranscript = liveChatAllowTranscript,
        isArchived = MutableStateFlow(isArchived),
        threadState = MutableStateFlow(threadState),
        agentName = flowOf(agentName),
        hasAiMessages = hasAiMessages,
    )

    private fun setTopBar(state: ConversationTopBarState) {
        composeTestRule.setContent {
            ChatTheme {
                ChatThreadTopBar(
                    scrollBehavior = null,
                    conversationState = state,
                    onEditThreadName = {},
                    onEditThreadValues = {},
                    onEndContact = {},
                    displayEndConversation = {},
                    onSendTranscript = {},
                )
            }
        }
    }

    @Test
    @MediumTest
    fun menuButton_hasContentDescription() {
        setTopBar(buildState(isMultiThreaded = true, isLiveChat = true))

        composeTestRule.onNodeWithTag("chat_thread_top_bar_menu_button")
            .assertContentDescriptionEquals(getString(string.livechat_conversation_options))
            .tryPerformAccessibilityChecks()
    }

    @Test
    fun topBar_titleIsLiveRegion() {
        setTopBar(buildState())

        // MediumTopAppBar renders the title composable in two slots (collapsed + expanded),
        // so both nodes have liveRegion set.
        composeTestRule.onAllNodes(
            SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite),
            useUnmergedTree = true,
        ).apply { assertCountEquals(2) }
    }

    /**
     * Verifies the title node's contentDescription is not duplicated.
     * When both a Text value and a contentDescription are present on the same semantics node,
     * TalkBack may announce the label twice. This test ensures the ContentDescription list
     * contains exactly one entry per title slot so TalkBack reads the title only once per slot.
     *
     * Note: MediumTopAppBar renders the title twice (collapsed + expanded), producing 2 nodes.
     */
    @Test
    fun topBar_titleContentDescriptionIsNotDuplicated() {
        setTopBar(buildState())

        val titleNodes = composeTestRule.onAllNodes(
            SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite),
            useUnmergedTree = true,
        ).fetchSemanticsNodes()

        assertTrue("Expected title nodes with liveRegion", titleNodes.isNotEmpty())
        titleNodes.forEach { node ->
            val descriptions = if (node.config.contains(SemanticsProperties.ContentDescription)) {
                node.config[SemanticsProperties.ContentDescription]
            } else {
                emptyList()
            }
            assertEquals(
                "Each title slot should have exactly one contentDescription entry",
                1,
                descriptions.size,
            )
        }
    }

    // Each SingleAction variant is covered by its own dedicated test above.
    // The full-bar accessibility check runs for one representative scenario here,
    // since setContent can only be called once per test instance.

    @Test
    @MediumTest
    fun editThreadNameBar_passesAccessibilityChecks() {
        setTopBar(buildState(isMultiThreaded = true))
        composeTestRule.onNodeWithTag("chat_thread_top_bar").tryPerformAccessibilityChecks()
    }

    @Test
    fun editCustomFieldsBar_passesAccessibilityChecks() {
        setTopBar(buildState(hasQuestions = true))
        composeTestRule.onNodeWithTag("chat_thread_top_bar").tryPerformAccessibilityChecks()
    }

    @Test
    fun endConversationBar_passesAccessibilityChecks() {
        setTopBar(buildState(isLiveChat = true))
        composeTestRule.onNodeWithTag("chat_thread_top_bar").tryPerformAccessibilityChecks()
    }

    @Test
    fun showArchivedBar_passesAccessibilityChecks() {
        setTopBar(buildState(isLiveChat = true, isArchived = true))
        composeTestRule.onNodeWithTag("chat_thread_top_bar").tryPerformAccessibilityChecks()
    }

    @Test
    fun multiThreadTitle_hasFormattedContentDescription() {
        setTopBar(buildState(isMultiThreaded = true))

        val titleNodes = composeTestRule.onAllNodes(
            SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite),
            useUnmergedTree = true,
        ).fetchSemanticsNodes()

        assertTrue("Expected title nodes with liveRegion", titleNodes.isNotEmpty())
        val noAgent = getString(string.state_description_no_agent_assigned)
        val threadDescription = getString(string.content_description_conversation_name, "Test thread")
        val expected = "$noAgent $threadDescription"
        titleNodes.forEach { node ->
            val descriptions = node.config[SemanticsProperties.ContentDescription]
            assertEquals("Each title slot should have exactly one contentDescription entry", 1, descriptions.size)
            assertEquals(
                expected,
                descriptions.first(),
            )
        }
        composeTestRule.onNodeWithTag("chat_thread_top_bar").tryPerformAccessibilityChecks()
    }

    @Test
    fun multiThreadTitle_withAgent_includesAgentName() {
        setTopBar(buildState(isMultiThreaded = true, agentName = "Peter Parker"))

        val titleNodes = composeTestRule.onAllNodes(
            SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite),
            useUnmergedTree = true,
        ).fetchSemanticsNodes()

        assertTrue("Expected title nodes with liveRegion", titleNodes.isNotEmpty())
        val agentAssigned = getString(string.state_description_agent_assigned)
        val threadDescription = getString(string.content_description_conversation_with_agent, "Test thread", "Peter Parker")
        val expected = "$agentAssigned $threadDescription"
        titleNodes.forEach { node ->
            val descriptions = node.config[SemanticsProperties.ContentDescription]
            assertEquals("Each title slot should have exactly one contentDescription entry", 1, descriptions.size)
            assertEquals(
                expected,
                descriptions.first(),
            )
        }
        composeTestRule.onNodeWithTag("chat_thread_top_bar").tryPerformAccessibilityChecks()
    }

    @Test
    fun liveChatTitle_hasPlainContentDescription() {
        setTopBar(buildState(isLiveChat = true, agentName = "John Doe"))

        val titleNodes = composeTestRule.onAllNodes(
            SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite),
            useUnmergedTree = true,
        ).fetchSemanticsNodes()

        assertTrue("Expected title nodes with liveRegion", titleNodes.isNotEmpty())
        titleNodes.forEach { node ->
            val descriptions = node.config[SemanticsProperties.ContentDescription]
            assertEquals("Each title slot should have exactly one contentDescription entry", 1, descriptions.size)
            assertEquals("Test thread", descriptions.first())
        }
        composeTestRule.onNodeWithTag("chat_thread_top_bar").tryPerformAccessibilityChecks()
    }

    @Test
    fun aiBadge_hasAiMessages_isPresentAndPassesAccessibilityChecks() {
        setTopBar(buildState(hasAiMessages = MutableStateFlow(true)))

        val aiLabel = getString(string.ai_assistant_label)
        // MediumTopAppBar renders the title slot twice (collapsed + expanded), so each badge appears twice.
        composeTestRule.onAllNodesWithContentDescription(aiLabel, useUnmergedTree = true)
            .assertCountEquals(2)
        composeTestRule.onNodeWithTag("chat_thread_top_bar").tryPerformAccessibilityChecks()
    }

    @Test
    fun aiBadge_hasAiMessages_contentDescriptionNotDuplicatedPerSlot() {
        setTopBar(buildState(hasAiMessages = MutableStateFlow(true)))

        val aiLabel = getString(string.ai_assistant_label)
        // Each badge node must announce the label exactly once — mergeDescendants = true on
        // AiAuthorshipBadge ensures the inner Text node is merged rather than announced separately.
        val badgeNodes = composeTestRule.onAllNodesWithContentDescription(aiLabel, useUnmergedTree = true)
            .fetchSemanticsNodes()
        assertTrue("Expected AI badge nodes to be present", badgeNodes.isNotEmpty())
        badgeNodes.forEach { node ->
            val descriptions = node.config[SemanticsProperties.ContentDescription]
            assertEquals("AI badge should announce label exactly once per slot", 1, descriptions.size)
            assertEquals(aiLabel, descriptions.first())
        }
    }

    @Test
    fun aiBadge_noAiMessages_isAbsent() {
        setTopBar(buildState(hasAiMessages = MutableStateFlow(false)))

        val aiLabel = getString(string.ai_assistant_label)
        composeTestRule.onAllNodesWithContentDescription(aiLabel, useUnmergedTree = true)
            .assertCountEquals(0)
    }
}
