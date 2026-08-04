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

package com.nice.cxonechat.internal.model

import com.nice.cxonechat.internal.serializer.Default
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AgentModelPiiHidingTest {

    @Test
    fun `missing PII fields deserialize without exception and default to null`() {
        val json = """{"id": 42}"""
        val model = Default.serializer.decodeFromString<AgentModel>(json)
        assertEquals(42, model.id)
        assertNull(model.firstName)
        assertNull(model.surname)
        assertNull(model.imageUrl)
    }

    @Test
    fun `explicit null PII fields deserialize without exception`() {
        val json = """{"id": 42, "firstName": null, "surname": null, "publicImageUrl": null}"""
        val model = Default.serializer.decodeFromString<AgentModel>(json)
        assertNull(model.firstName)
        assertNull(model.surname)
        assertNull(model.imageUrl)
    }

    @Test
    fun `empty string PII fields deserialize and are preserved`() {
        val json = """{"id": 42, "firstName": "", "surname": "", "publicImageUrl": ""}"""
        val model = Default.serializer.decodeFromString<AgentModel>(json)
        assertEquals("", model.firstName)
        assertEquals("", model.surname)
        assertEquals("", model.imageUrl)
    }

    @Test
    fun `toAgent passes through null PII fields as null`() {
        val model = AgentModel(id = 42, firstName = null, surname = null, imageUrl = null)
        val agent = model.toAgent()
        assertNull(agent.firstName)
        assertNull(agent.lastName)
        assertNull(agent.imageUrl)
    }

    @Test
    fun `toMessageAuthor converts null firstName and lastName to empty strings`() {
        val model = AgentModel(id = 42, firstName = null, surname = null, imageUrl = null)
        val author = model.toMessageAuthor()
        assertEquals("", author.firstName)
        assertEquals("", author.lastName)
    }

    @Test
    fun `toMessageAuthor passes through null imageUrl as null`() {
        val model = AgentModel(id = 42, imageUrl = null)
        val author = model.toMessageAuthor()
        assertNull(author.imageUrl)
    }

    @Test
    fun `absent isBotUser and isSurveyUser deserialize to null`() {
        val json = """{"id": 42}"""
        val model = Default.serializer.decodeFromString<AgentModel>(json)
        assertNull(model.isBotUser)
        assertNull(model.isSurveyUser)
    }

    @Test
    fun `explicit null isBotUser and isSurveyUser deserialize to null`() {
        val json = """{"id": 42, "isBotUser": null, "isSurveyUser": null}"""
        val model = Default.serializer.decodeFromString<AgentModel>(json)
        assertNull(model.isBotUser)
        assertNull(model.isSurveyUser)
    }

    @Test
    fun `toAgent passes through null isBotUser and isSurveyUser without defaulting to false`() {
        val model = AgentModel(id = 42, isBotUser = null, isSurveyUser = null)
        val agent = model.toAgent()
        assertNull(agent.isBotUser)
        assertNull(agent.isSurveyUser)
    }

    @Test
    fun `toAgent passes through null firstName and lastName without defaulting to empty string`() {
        val model = AgentModel(id = 42, firstName = null, surname = null)
        val agent = model.toAgent()
        assertNull(agent.firstName)
        assertNull(agent.lastName)
    }

    @Test
    fun `publicImageUrl deserializes into imageUrl and filteredFallbackImageUrl stays null`() {
        val json = """{"id": 42, "publicImageUrl": "https://example.com/public.png"}"""
        val model = Default.serializer.decodeFromString<AgentModel>(json)
        assertEquals("https://example.com/public.png", model.imageUrl)
        assertNull(model.filteredFallbackImageUrl)
    }

    @Test
    fun `imageUrl deserializes into filteredFallbackImageUrl when publicImageUrl is absent`() {
        val json = """{"id": 42, "imageUrl": "https://example.com/filtered.png"}"""
        val model = Default.serializer.decodeFromString<AgentModel>(json)
        assertNull(model.imageUrl)
        assertEquals("https://example.com/filtered.png", model.filteredFallbackImageUrl)
    }

    @Test
    fun `publicImageUrl and imageUrl deserialize independently when both are present`() {
        val json = """
            {"id": 42, "publicImageUrl": "https://example.com/public.png", "imageUrl": "https://example.com/filtered.png"}
        """.trimIndent()
        val model = Default.serializer.decodeFromString<AgentModel>(json)
        assertEquals("https://example.com/public.png", model.imageUrl)
        assertEquals("https://example.com/filtered.png", model.filteredFallbackImageUrl)
    }

    @Test
    fun `missing publicImageUrl and imageUrl deserialize both to null`() {
        val json = """{"id": 42}"""
        val model = Default.serializer.decodeFromString<AgentModel>(json)
        assertNull(model.imageUrl)
        assertNull(model.filteredFallbackImageUrl)
    }

    @Test
    fun `toAgent prefers publicImageUrl over filteredFallbackImageUrl when both are present`() {
        val model = AgentModel(
            id = 42,
            imageUrl = "https://example.com/public.png",
            filteredFallbackImageUrl = "https://example.com/filtered.png",
        )
        val agent = model.toAgent()
        assertEquals("https://example.com/public.png", agent.imageUrl)
    }

    @Test
    fun `toAgent falls back to filteredFallbackImageUrl when publicImageUrl is null`() {
        val model = AgentModel(id = 42, imageUrl = null, filteredFallbackImageUrl = "https://example.com/filtered.png")
        val agent = model.toAgent()
        assertEquals("https://example.com/filtered.png", agent.imageUrl)
    }

    @Test
    fun `toAgent imageUrl is null when both publicImageUrl and filteredFallbackImageUrl are null`() {
        val model = AgentModel(id = 42, imageUrl = null, filteredFallbackImageUrl = null)
        val agent = model.toAgent()
        assertNull(agent.imageUrl)
    }

    @Test
    fun `toMessageAuthor prefers publicImageUrl over filteredFallbackImageUrl when both are present`() {
        val model = AgentModel(
            id = 42,
            imageUrl = "https://example.com/public.png",
            filteredFallbackImageUrl = "https://example.com/filtered.png",
        )
        val author = model.toMessageAuthor()
        assertEquals("https://example.com/public.png", author.imageUrl)
    }

    @Test
    fun `toMessageAuthor falls back to filteredFallbackImageUrl when publicImageUrl is null`() {
        val model = AgentModel(id = 42, imageUrl = null, filteredFallbackImageUrl = "https://example.com/filtered.png")
        val author = model.toMessageAuthor()
        assertEquals("https://example.com/filtered.png", author.imageUrl)
    }

    @Test
    fun `toMessageAuthor imageUrl is null when both publicImageUrl and filteredFallbackImageUrl are null`() {
        val model = AgentModel(id = 42, imageUrl = null, filteredFallbackImageUrl = null)
        val author = model.toMessageAuthor()
        assertNull(author.imageUrl)
    }
}
