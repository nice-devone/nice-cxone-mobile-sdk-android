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

package com.nice.cxonechat.internal

import com.nice.cxonechat.internal.model.MessageDirectionModel.ToAgent
import com.nice.cxonechat.internal.model.MessageDirectionModel.ToClient
import com.nice.cxonechat.internal.model.MessageModel
import com.nice.cxonechat.internal.model.network.MediaModel
import com.nice.cxonechat.internal.model.network.MessagePolyContent
import com.nice.cxonechat.internal.model.network.WrappedText
import com.nice.cxonechat.internal.serializer.Default
import com.nice.cxonechat.model.makeMessageModel
import com.nice.cxonechat.model.makeMessageContent
import com.nice.cxonechat.model.makeUnsupportedMessageContent
import com.nice.cxonechat.tool.serialize
import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

internal class MessageAiGeneratedTest {

    @Test
    fun `isAiGenerated defaults to false when not set`() {
        val model = makeMessageModel(direction = ToClient)
        assertFalse(model.isAiGenerated)
    }

    @Test
    fun `isAiGenerated is true when explicitly set to true`() {
        val model = makeMessageModel(direction = ToClient, isAiGenerated = true)
        assertTrue(model.isAiGenerated)
    }

    @Test
    fun `toMessage maps isAiGenerated false from model to domain Message`() {
        val message = makeMessageModel(direction = ToClient, isAiGenerated = false).toMessage()
        assertNotNull(message)
        assertFalse(message.isAiGenerated)
    }

    @Test
    fun `toMessage maps isAiGenerated true from model to domain Message`() {
        val message = makeMessageModel(direction = ToClient, isAiGenerated = true).toMessage()
        assertNotNull(message)
        assertTrue(message.isAiGenerated)
    }

    @Test
    fun `MessageModel JSON roundtrip preserves isAiGenerated true`() {
        val original = makeMessageModel(direction = ToClient, isAiGenerated = true)
        val json = original.serialize()
        val decoded = Default.serializer.decodeFromString<MessageModel>(json)
        assertTrue(decoded.isAiGenerated)
    }

    @Test
    fun `MessageModel decodes isAiGenerated as false when field absent from JSON`() {
        // Default.serializer uses encodeDefaults = true, so the field is always written even when
        // false — stripping it here genuinely tests absent-field deserialization, not a no-op.
        val json = makeMessageModel(direction = ToClient).serialize()
            .replace(Regex("""(?:,\s*"isAiGenerated"\s*:\s*\w+|"isAiGenerated"\s*:\s*\w+\s*,\s*)"""), "")
        val decoded = Default.serializer.decodeFromString<MessageModel>(json)
        assertFalse(decoded.isAiGenerated)
    }

    @Test
    fun `isAiGenerated propagates to domain Message for all concrete message types`() {
        allConcreteContentTypes().forEach { content ->
            val message = makeMessageModel(
                direction = ToClient,
                messageContent = content,
                isAiGenerated = true,
            ).toMessage()
            assertNotNull(message, "Expected non-null Message for ${content::class.simpleName}")
            assertTrue(
                message.isAiGenerated,
                "Expected isAiGenerated=true for ${content::class.simpleName}",
            )
        }
    }

    /**
     * Verifies that the direction guard works for all concrete message types.
     *
     * Even when the backend DTO has `isAiGenerated=true`, a `ToAgent` message (customer → agent)
     * must never surface as AI-generated in the domain layer.
     * This combination is not supported and should not be propagated.
     */
    @Test
    fun `isAiGenerated is false for ToAgent messages even when model flag is true`() {
        allConcreteContentTypes().forEach { content ->
            val message = makeMessageModel(
                direction = ToAgent,
                messageContent = content,
                isAiGenerated = true,
            ).toMessage()
            assertNotNull(message, "Expected non-null Message for ${content::class.simpleName}")
            assertFalse(
                message.isAiGenerated,
                "Expected isAiGenerated=false for ToAgent message of type ${content::class.simpleName}",
            )
        }
    }

    private fun allConcreteContentTypes() = listOf(
        makeMessageContent(),
        makeUnsupportedMessageContent(),
        MessagePolyContent.QuickReplies(
            fallbackText = "fallback",
            payload = MessagePolyContent.QuickReplies.Payload(
                text = WrappedText("text"),
                actions = emptyList(),
            ),
        ),
        MessagePolyContent.ListPicker(
            fallbackText = "fallback",
            payload = MessagePolyContent.ListPicker.Payload(
                title = WrappedText("title"),
                text = WrappedText("text"),
                actions = emptyList(),
            ),
        ),
        MessagePolyContent.RichLink(
            fallbackText = "fallback",
            payload = MessagePolyContent.RichLink.Payload(
                media = MediaModel(fileName = "img.jpg", url = "https://example.com/img.jpg", mimeType = "image/jpeg"),
                title = WrappedText("title"),
                url = "https://example.com",
            ),
        ),
        MessagePolyContent.TimePicker(
            fallbackText = "fallback",
            payload = MessagePolyContent.TimePicker.Payload(
                title = WrappedText("title"),
                event = MessagePolyContent.TimePicker.Payload.Event(
                    title = WrappedText("event"),
                    timeSlots = emptyList(),
                ),
            ),
        ),
    )
}
