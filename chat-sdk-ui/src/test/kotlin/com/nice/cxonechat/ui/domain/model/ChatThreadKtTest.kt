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

package com.nice.cxonechat.ui.domain.model

import com.nice.cxonechat.message.Message
import com.nice.cxonechat.thread.Agent
import com.nice.cxonechat.thread.ChatThread
import com.nice.cxonechat.thread.ChatThreadState
import com.nice.cxonechat.thread.CustomField
import org.junit.Test
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Unit tests for ChatThreadKt extension functions, specifically testing
 * the threadOrAgentName() function's nickname preference logic.
 */
class ChatThreadKtTest {

    /**
     * Test that thread name takes priority when multi-thread is enabled,
     * even when agent has a nickname.
     */
    @Test
    fun threadOrAgentName_multiThreadEnabledWithThreadName_returnsThreadName() {
        val agent = createAgent(
            firstName = TEST_FIRST_NAME,
            lastName = TEST_LAST_NAME,
            nickname = TEST_NICKNAME
        )
        val thread = createChatThread(
            threadName = TEST_THREAD_NAME,
            agent = agent
        )

        val result = thread.threadOrAgentName(isMultiThreadEnabled = true)

        assertEquals(TEST_THREAD_NAME, result)
    }

    /**
     * Test that agent's nickname is preferred over full name when
     * multi-thread is disabled.
     */
    @Test
    fun threadOrAgentName_multiThreadDisabledWithNickname_returnsNickname() {
        val agent = createAgent(
            firstName = TEST_FIRST_NAME,
            lastName = TEST_LAST_NAME,
            nickname = TEST_NICKNAME
        )
        val thread = createChatThread(
            threadName = TEST_THREAD_NAME,
            agent = agent
        )

        val result = thread.threadOrAgentName(isMultiThreadEnabled = false)

        assertEquals(TEST_NICKNAME, result)
    }

    /**
     * Test that when nickname is null, function falls back to full name.
     */
    @Test
    fun threadOrAgentName_nullNickname_returnsFullName() {
        val agent = createAgent(
            firstName = TEST_FIRST_NAME,
            lastName = TEST_LAST_NAME,
            nickname = null
        )
        val thread = createChatThread(
            threadName = "",
            agent = agent
        )

        val result = thread.threadOrAgentName(isMultiThreadEnabled = false)

        assertEquals(TEST_FULL_NAME, result)
    }

    /**
     * Test that when nickname is blank (whitespace only), function falls back to full name.
     */
    @Test
    fun threadOrAgentName_blankNickname_returnsFullName() {
        val agent = createAgent(
            firstName = TEST_FIRST_NAME,
            lastName = TEST_LAST_NAME,
            nickname = "   "
        )
        val thread = createChatThread(
            threadName = "",
            agent = agent
        )

        val result = thread.threadOrAgentName(isMultiThreadEnabled = false)

        assertEquals(TEST_FULL_NAME, result)
    }

    /**
     * Test that when nickname is empty string, function falls back to full name.
     */
    @Test
    fun threadOrAgentName_emptyNickname_returnsFullName() {
        val agent = createAgent(
            firstName = TEST_FIRST_NAME,
            lastName = TEST_LAST_NAME,
            nickname = ""
        )
        val thread = createChatThread(
            threadName = "",
            agent = agent
        )

        val result = thread.threadOrAgentName(isMultiThreadEnabled = false)

        assertEquals(TEST_FULL_NAME, result)
    }

    /**
     * Test that nickname is preferred over full name when both are available.
     */
    @Test
    fun threadOrAgentName_nicknamePresent_prefersNicknameOverFullName() {
        val agent = createAgent(
            firstName = TEST_FIRST_NAME,
            lastName = TEST_LAST_NAME,
            nickname = TEST_NICKNAME
        )
        val thread = createChatThread(
            threadName = "",
            agent = agent
        )

        val result = thread.threadOrAgentName(isMultiThreadEnabled = false)

        assertEquals(TEST_NICKNAME, result)
    }

    /**
     * Test that when agent PII is hidden (null firstName, lastName, no nickname),
     * fullName returns null and threadOrAgentName returns null.
     */
    @Test
    fun threadOrAgentName_agentWithHiddenPii_returnsNull() {
        val agent = createAgent(firstName = null, lastName = null, nickname = null)
        val thread = createChatThread(threadName = "", agent = agent)

        val result = thread.threadOrAgentName(isMultiThreadEnabled = false)

        assertNull(result)
    }

    /**
     * Test that when agent PII is hidden but the thread has a name in multi-thread mode,
     * the thread name is returned (not agent fallback).
     */
    @Test
    fun threadOrAgentName_hiddenPiiAgentWithThreadName_returnsThreadName() {
        val agent = createAgent(firstName = null, lastName = null, nickname = null)
        val thread = createChatThread(threadName = TEST_THREAD_NAME, agent = agent)

        val result = thread.threadOrAgentName(isMultiThreadEnabled = true)

        assertEquals(TEST_THREAD_NAME, result)
    }

    /**
     * Test that when no agent is assigned, function returns null.
     */
    @Test
    fun threadOrAgentName_noAgent_returnsNull() {
        val thread = createChatThread(
            threadName = "",
            agent = null
        )

        val result = thread.threadOrAgentName(isMultiThreadEnabled = false)

        assertNull(result)
    }

    /**
     * Test that when thread name is blank in multi-thread mode,
     * it falls through to nickname/fullName.
     */
    @Test
    fun threadOrAgentName_blankThreadNameMultiThreadEnabled_returnsNickname() {
        val agent = createAgent(
            firstName = TEST_FIRST_NAME,
            lastName = TEST_LAST_NAME,
            nickname = TEST_NICKNAME
        )
        val thread = createChatThread(
            threadName = "   ",
            agent = agent
        )

        val result = thread.threadOrAgentName(isMultiThreadEnabled = true)

        assertEquals(TEST_NICKNAME, result)
    }

    // Helper functions to create minimal test doubles

    private fun createAgent(
        firstName: String?,
        lastName: String?,
        nickname: String?,
    ): Agent {
        return object : Agent() {
            override val id: Int = 1
            override val firstName: String? = firstName
            override val lastName: String? = lastName
            override val nickname: String? = nickname
            override val isBotUser: Boolean? = null
            override val isSurveyUser: Boolean? = null
            override val imageUrl: String? = null
            override val isTyping: Boolean = false
        }
    }

    private fun createChatThread(
        threadName: String,
        agent: Agent?,
    ): ChatThread {
        return object : ChatThread() {
            override val id: UUID = UUID.randomUUID()
            override val threadName: String? = threadName.takeIf { it.isNotEmpty() }
            override val messages: List<Message> = emptyList()
            override val threadAgent: Agent? = agent
            override val canAddMoreMessages: Boolean = true
            override val scrollToken: String = ""
            override val threadState: ChatThreadState = ChatThreadState.Ready
            override val fields: List<CustomField> = emptyList()
            override val positionInQueue: Int? = null
            override val hasOnlineAgent: Boolean = false
            override val contactId: String? = null
        }
    }

    companion object {
        private const val TEST_FIRST_NAME = "John"
        private const val TEST_LAST_NAME = "Doe"
        private const val TEST_NICKNAME = "Johnny"
        private const val TEST_THREAD_NAME = "Support"
        private const val TEST_FULL_NAME = "John Doe"
    }
}
