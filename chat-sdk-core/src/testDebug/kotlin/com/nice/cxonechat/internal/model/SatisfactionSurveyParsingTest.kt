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

import com.nice.cxonechat.Popup
import com.nice.cxonechat.internal.model.network.MessagePolyContent.Plugin.PluginElement.SimpleElement.IframeButtonElement
import com.nice.cxonechat.internal.model.network.MessagePolyContent.Plugin.PluginElement.SimpleElement.TextElement
import com.nice.cxonechat.internal.model.network.MessagePolyContent.Plugin.PluginElement.StructuredElements.SatisfactionSurveyPlugin
import com.nice.cxonechat.internal.serializer.Default
import com.nice.cxonechat.model.makeMessageModel
import com.nice.cxonechat.server.ServerResponse
import com.nice.cxonechat.tool.serialize
import kotlinx.serialization.SerializationException
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull

/**
 * Parsing coverage for the SATISFACTION_SURVEY plugin message (DE-174245), mirroring the existing
 * INACTIVITY_POPUP parsing structure.
 */
internal class SatisfactionSurveyParsingTest {

    @Test
    fun `SATISFACTION_SURVEY plugin message decodes to Popup SatisfactionSurvey`() {
        val threadId = UUID.randomUUID()
        val json = ServerResponse.Message.SatisfactionSurvey(
            threadId = threadId,
            text = "How was your experience?",
            surveyUrl = "https://survey.example.com/s/xyz789",
        ).serialize()

        val model = Default.serializer.decodeFromString<MessageModel>(json)
        val popup = assertIs<Popup.SatisfactionSurvey>(model.toPopup())

        assertEquals("How was your experience?", popup.text)
        assertEquals("https://survey.example.com/s/xyz789", popup.surveyUrl)
    }

    @Test
    fun `SATISFACTION_SURVEY plugin message is excluded from the regular message list`() {
        val threadId = UUID.randomUUID()
        val json = ServerResponse.Message.SatisfactionSurvey(threadId).serialize()

        val model = Default.serializer.decodeFromString<MessageModel>(json)

        assertNull(model.toMessage())
    }

    @Test
    fun `toString does not leak the survey url`() {
        val threadId = UUID.randomUUID()
        val json = ServerResponse.Message.SatisfactionSurvey(
            threadId = threadId,
            surveyUrl = "https://survey.example.com/s/customer-session-xyz789",
        ).serialize()
        val model = Default.serializer.decodeFromString<MessageModel>(json)
        val popup = assertIs<Popup.SatisfactionSurvey>(model.toPopup())

        assertFalse(popup.toString().contains("customer-session-xyz789"))
    }

    @Test
    fun `missing IFRAME_BUTTON element throws SerializationException`() {
        val model = makeMessageModel()
        val malformedElement = SatisfactionSurveyPlugin(
            elements = listOf(TextElement(text = "prompt only, no survey link")),
        )

        assertFailsWith<SerializationException> {
            SatisfactionSurveyInternal(model, malformedElement)
        }
    }

    @Test
    fun `non-HTTPS survey URL throws SerializationException`() {
        val model = makeMessageModel()
        val nonHttpsElement = SatisfactionSurveyPlugin(
            elements = listOf(
                TextElement(text = "How was your experience?"),
                IframeButtonElement(text = "Rate", url = "http://survey.example.com/s/xyz789"),
            ),
        )

        assertFailsWith<SerializationException> {
            SatisfactionSurveyInternal(model, nonHttpsElement)
        }
    }

    @Test
    fun `malformed survey URL throws SerializationException`() {
        val model = makeMessageModel()
        val malformedUrlElement = SatisfactionSurveyPlugin(
            elements = listOf(
                TextElement(text = "How was your experience?"),
                IframeButtonElement(text = "Rate", url = "javascript:alert(1)"),
            ),
        )

        assertFailsWith<SerializationException> {
            SatisfactionSurveyInternal(model, malformedUrlElement)
        }
    }

    @Test
    fun `IFRAME_BUTTON element with a missing url decodes successfully and fails only in validateSurveyUrl`() {
        // No "url" key at all - distinct from an empty/malformed string value, and the case that would
        // previously throw MissingFieldException while decoding the whole MessageModel, before
        // SatisfactionSurveyInternal (and the popupOrLogAndDrop that can catch it) ever ran.
        val element = Default.serializer.decodeFromString<IframeButtonElement>("""{"id":"1","text":"Take survey"}""")
        assertEquals("", element.url)

        val model = makeMessageModel()
        val malformedUrlElement = SatisfactionSurveyPlugin(
            elements = listOf(TextElement(text = "How was your experience?"), element),
        )

        assertFailsWith<SerializationException> {
            SatisfactionSurveyInternal(model, malformedUrlElement)
        }
    }

    @Test
    fun `SATISFACTION_SURVEY plugin with no elements key decodes successfully and fails only in checkStructure`() {
        // No "elements" key at all - the same decode-time-vs-validation-time distinction as the
        // IframeButtonElement test above, now for the plugin's own elements list.
        val plugin = Default.serializer.decodeFromString<SatisfactionSurveyPlugin>("{}")
        assertEquals(emptyList(), plugin.elements)

        val model = makeMessageModel()
        assertFailsWith<SerializationException> {
            SatisfactionSurveyInternal(model, plugin)
        }
    }
}
