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

package com.nice.cxonechat.ui.viewmodel

import com.nice.cxonechat.thread.Agent
import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests the mapping contract used by agentNameFlow and hasAgentFlow:
 * - agentNameFlow: agent?.nickname?.ifBlank { null } ?: agent?.fullName?.ifBlank { null }
 * - hasAgentFlow: agent != null
 *
 * Key invariant: when PII is hidden, agentName is null AND hasAgent is true simultaneously.
 */
class AgentNameSignalTest {

    @Test
    fun `hidden PII agent produces null agentName and true hasAgent`() {
        val hiddenAgent: Agent? = agentWithBlankName()

        val agentName = computeAgentName(hiddenAgent)
        val hasAgent = hiddenAgent != null

        assertNull(agentName, "agentNameFlow should emit null for hidden PII agent")
        assertTrue(hasAgent, "hasAgentFlow should emit true when agent is assigned")
    }

    @Test
    fun `null agent produces null agentName and false hasAgent`() {
        val noAgent: Agent? = null

        val agentName = computeAgentName(noAgent)
        val hasAgent = noAgent != null

        assertNull(agentName)
        assertFalse(hasAgent)
    }

    @Test
    fun `named agent produces non-null agentName and true hasAgent`() {
        val namedAgent: Agent? = agentWithName(firstName = "Alice", lastName = "Smith")

        val agentName = computeAgentName(namedAgent)
        val hasAgent = namedAgent != null

        assertTrue(agentName == "Alice Smith", "expected full name, got: $agentName")
        assertTrue(hasAgent)
    }

    @Test
    fun `agent with nickname prefers nickname over fullName`() {
        val agent: Agent? = agentWithNickname(nickname = "Alice", firstName = "A", lastName = "B")

        val agentName = computeAgentName(agent)

        assertTrue(agentName == "Alice", "expected nickname, got: $agentName")
    }

    private fun computeAgentName(agent: Agent?): String? =
        agent?.nickname?.ifBlank { null } ?: agent?.fullName?.ifBlank { null }

    private fun agentWithBlankName(): Agent = object : Agent() {
        override val id = 1
        override val firstName: String? = null
        override val lastName: String? = null
        override val nickname: String? = null
        override val isBotUser: Boolean? = null
        override val isSurveyUser: Boolean? = null
        override val imageUrl: String? = null
        override val isTyping = false
    }

    private fun agentWithName(firstName: String, lastName: String): Agent = object : Agent() {
        override val id = 1
        override val firstName = firstName
        override val lastName = lastName
        override val nickname: String? = null
        override val isBotUser = false
        override val isSurveyUser = false
        override val imageUrl = ""
        override val isTyping = false
    }

    private fun agentWithNickname(nickname: String, firstName: String, lastName: String): Agent = object : Agent() {
        override val id = 1
        override val firstName = firstName
        override val lastName = lastName
        override val nickname: String = nickname
        override val isBotUser = false
        override val isSurveyUser = false
        override val imageUrl = ""
        override val isTyping = false
    }
}
