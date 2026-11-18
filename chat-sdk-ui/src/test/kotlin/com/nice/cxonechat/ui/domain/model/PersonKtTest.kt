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

import com.nice.cxonechat.thread.Agent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class PersonKtTest {

    @Test
    fun `personFromResolvedName derives single-letter monogram from a one-word nickname over a two-word full name`() {
        // Mirrors the iOS reference (cxone-mobile-sdk-ios PR #786): nickname "Bombardak" over full name "John Doe" yields "B".
        val person = personFromResolvedName(name = "Bombardak", imageUrl = null)

        assertEquals("B", person.monogram)
    }

    @Test
    fun `personFromResolvedName derives two-letter monogram from a two-word resolved name`() {
        val person = personFromResolvedName(name = "John Doe", imageUrl = null)

        assertEquals("JD", person.monogram)
    }

    @Test
    fun `personFromResolvedName derives monogram from first and last word when resolved name has more than two words`() {
        val person = personFromResolvedName(name = "Mary Jane Watson", imageUrl = null)

        assertEquals("MW", person.monogram)
    }

    @Test
    fun `personFromResolvedName monogram is null when resolved name is null`() {
        val person = personFromResolvedName(name = null, imageUrl = null)

        assertNull(person.monogram)
    }

    @Test
    fun `personFromResolvedName passes the image url through unchanged`() {
        val person = personFromResolvedName(name = "John Doe", imageUrl = "https://example.com/avatar.png")

        assertEquals("https://example.com/avatar.png", person.imageUrl)
    }

    @Test
    fun removeDefaultImageUrl_removes_expected_urls() {
        assertNull(
            removeDefaultImageUrl(
                """https://assets-qa.brandembassy.com/platform/static/public/img/user/l.png"""
            )
        )
        assertNull(
            removeDefaultImageUrl(
                """https://app-de-na1.niceincontact.com/img/user/t.png"""
            )
        )
        assertNull(
            removeDefaultImageUrl(
                """https://assets-qa.brandembassy.com/platform/static/public/img/user-default.png"""
            )
        )
    }

    @Test
    fun removeDefaultImageUrl_returns_null_for_blank_strings() {
        assertNull(removeDefaultImageUrl(""))
        assertNull(removeDefaultImageUrl("   "))
    }

    @Test
    fun removeDefaultImageUrl_keeps_other_urls() {
        assertNotNull(
            removeDefaultImageUrl(
                """https://brand-embassy-avatars-qa.s3.eu-west-1.amazonaws.com/82d4eeac-4eb0-4ef2-8884-928c632c2275.jpg"""
            )
        )
        assertNotNull(removeDefaultImageUrl("""https://nice.com/my-image.png"""))
        assertNotNull(removeDefaultImageUrl("""https://nice.com/image/t.png"""))
        assertNotNull(removeDefaultImageUrl("""https://app-de-na1.niceincontact.com/user/user-default.png"""))
    }

    // --- Agent.asPerson with PII hiding ---
    // The backend can null firstName, lastName and imageUrl when agent PII is hidden;
    // asPerson must convert null names to "" so the downstream Person model stays consistent.

    @Test
    fun `asPerson converts null firstName to empty string when PII is hidden`() {
        val agent = agentWith(firstName = null, lastName = null)

        val person = agent.asPerson

        assertEquals("", person.firstName)
    }

    @Test
    fun `asPerson converts null lastName to empty string when PII is hidden`() {
        val agent = agentWith(firstName = null, lastName = null)

        val person = agent.asPerson

        assertEquals("", person.lastName)
    }

    @Test
    fun `asPerson produces null imageUrl when agent imageUrl is null`() {
        val agent = agentWith(firstName = null, lastName = null)

        val person = agent.asPerson

        assertNull(person.imageUrl)
    }

    @Test
    fun `asPerson produces null monogram when both firstName and lastName are null`() {
        val agent = agentWith(firstName = null, lastName = null)

        val person = agent.asPerson

        assertNull(person.monogram)
        assertNull(person.fullName)
    }

    @Test
    fun `asPerson produces correct monogram from non-null name fields`() {
        val agent = agentWith(firstName = "Alice", lastName = "Smith")

        val person = agent.asPerson

        assertEquals("AS", person.monogram)
    }

    @Test
    fun `asPerson with only firstName produces single-letter monogram and firstName as fullName`() {
        val agent = agentWith(firstName = "Alice", lastName = null)

        val person = agent.asPerson

        assertEquals("A", person.monogram)
        assertEquals("Alice", person.fullName)
    }

    @Test
    fun `asPerson with only lastName produces single-letter monogram and lastName as fullName`() {
        val agent = agentWith(firstName = null, lastName = "Smith")

        val person = agent.asPerson

        assertEquals("S", person.monogram)
        assertEquals("Smith", person.fullName)
    }

    @Test
    fun `asPerson with non-null names produces the full name`() {
        val agent = agentWith(firstName = "Alice", lastName = "Smith")

        val person = agent.asPerson

        assertEquals("Alice Smith", person.fullName)
    }

    @Test
    fun `asPerson strips default server avatar URL`() {
        val defaultUrl = "https://assets-qa.brandembassy.com/platform/static/public/img/user/a.png"
        val agent = agentWith(firstName = "Alice", lastName = "Smith", imageUrl = defaultUrl)

        assertNull(agent.asPerson.imageUrl)
    }

    @Test
    fun `asPerson preserves a real custom imageUrl`() {
        val customUrl = "https://brand-embassy-avatars-qa.s3.eu-west-1.amazonaws.com/avatar.jpg"
        val agent = agentWith(firstName = "Alice", lastName = "Smith", imageUrl = customUrl)

        assertEquals(customUrl, agent.asPerson.imageUrl)
    }

    // --- personFromResolvedName with blank name ---

    @Test
    fun `personFromResolvedName produces null monogram for blank whitespace-only name`() {
        val person = personFromResolvedName(name = "   ", imageUrl = null)

        assertNull(person.monogram)
    }

    // --- Helpers ---

    private fun agentWith(
        firstName: String?,
        lastName: String?,
        imageUrl: String? = null,
    ): Agent = object : Agent() {
        override val id = 1
        override val firstName = firstName
        override val lastName = lastName
        override val nickname: String? = null
        override val isBotUser: Boolean? = null
        override val isSurveyUser: Boolean? = null
        override val imageUrl = imageUrl
        override val isTyping = false
    }
}
