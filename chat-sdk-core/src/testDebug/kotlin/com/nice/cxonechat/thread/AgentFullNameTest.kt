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

package com.nice.cxonechat.thread

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AgentFullNameTest {

    @Test
    fun `fullName returns null when both firstName and lastName are null`() {
        val agent = agent(firstName = null, lastName = null)
        assertNull(agent.fullName)
    }

    @Test
    fun `fullName returns null when both firstName and lastName are blank`() {
        val agent = agent(firstName = "", lastName = "")
        assertNull(agent.fullName)
    }

    @Test
    fun `fullName returns null when both name parts are whitespace only`() {
        val agent = agent(firstName = "  ", lastName = "  ")
        assertNull(agent.fullName)
    }

    @Test
    fun `fullName returns non-null when firstName is present`() {
        val agent = agent(firstName = "Alice", lastName = "")
        assertEquals("Alice", agent.fullName)
    }

    @Test
    fun `fullName returns non-null when lastName is present`() {
        val agent = agent(firstName = "", lastName = "Smith")
        assertEquals("Smith", agent.fullName)
    }

    @Test
    fun `fullName returns trimmed full name when both parts present`() {
        val agent = agent(firstName = "Alice", lastName = "Smith")
        assertEquals("Alice Smith", agent.fullName)
    }

    private fun agent(firstName: String?, lastName: String?): Agent = object : Agent() {
        override val id = 1
        override val firstName = firstName
        override val lastName = lastName
        override val nickname: String? = null
        override val isBotUser: Boolean? = null
        override val isSurveyUser: Boolean? = null
        override val imageUrl: String? = null
        override val isTyping = false
    }
}
