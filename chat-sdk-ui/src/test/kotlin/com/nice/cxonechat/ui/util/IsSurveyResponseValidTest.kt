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

package com.nice.cxonechat.ui.util

import com.nice.cxonechat.prechat.PreChatSurvey
import com.nice.cxonechat.prechat.PreChatSurveyResponse
import com.nice.cxonechat.state.FieldDefinition
import io.mockk.every
import io.mockk.mockk
import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for [isSurveyResponseValid].
 *
 * This function gates the pre-chat submit button, and it is not the last line of defence: core
 * re-checks required fields in `ChatThreadsHandlerImpl.create` via `checkRequired`, which throws
 * `MissingPreChatCustomFieldsException`. An incorrect accept therefore surfaces as an exception out
 * of thread creation rather than as a field error, and an incorrect reject blocks the user from
 * starting a conversation at all. Both directions have direct user impact, and neither is signalled
 * here — this function only ever returns a boolean.
 */
internal class IsSurveyResponseValidTest {

    // ── helpers ───────────────────────────────────────────────────────────────

    private fun mockTextField(required: Boolean): FieldDefinition.Text = mockk {
        every { isRequired } returns required
    }

    private fun mockSurvey(fields: List<FieldDefinition>): PreChatSurvey = mockk {
        every { this@mockk.fields } returns fields.asSequence()
    }

    private fun textResponse(question: FieldDefinition.Text): PreChatSurveyResponse<out FieldDefinition, out Any> =
        PreChatSurveyResponse.Text(question, "answer")

    // ── valid response scenarios ──────────────────────────────────────────────

    @Test
    fun `returns true when the single required field has a response`() {
        val required = mockTextField(required = true)
        val survey = mockSurvey(listOf(required))
        assertTrue(isSurveyResponseValid(survey, listOf(textResponse(required))))
    }

    @Test
    fun `returns true when all required fields have responses`() {
        val req1 = mockTextField(required = true)
        val req2 = mockTextField(required = true)
        val survey = mockSurvey(listOf(req1, req2))
        assertTrue(isSurveyResponseValid(survey, listOf(textResponse(req1), textResponse(req2))))
    }

    @Test
    fun `returns true when there are no required fields and no responses`() {
        val optional = mockTextField(required = false)
        val survey = mockSurvey(listOf(optional))
        assertTrue(isSurveyResponseValid(survey, emptyList()))
    }

    @Test
    fun `returns true when the survey has no fields at all`() {
        val survey = mockSurvey(emptyList())
        assertTrue(isSurveyResponseValid(survey, emptyList()))
    }

    @Test
    fun `returns true when required and optional fields are all answered`() {
        val required = mockTextField(required = true)
        val optional = mockTextField(required = false)
        val survey = mockSurvey(listOf(required, optional))
        assertTrue(isSurveyResponseValid(survey, listOf(textResponse(required), textResponse(optional))))
    }

    // ── invalid response scenarios ────────────────────────────────────────────

    @Test
    fun `returns false when the single required field has no response`() {
        val required = mockTextField(required = true)
        val survey = mockSurvey(listOf(required))
        assertFalse(isSurveyResponseValid(survey, emptyList()))
    }

    @Test
    fun `returns false when one of two required fields is unanswered`() {
        val req1 = mockTextField(required = true)
        val req2 = mockTextField(required = true)
        val survey = mockSurvey(listOf(req1, req2))
        assertFalse(isSurveyResponseValid(survey, listOf(textResponse(req1))))
    }

    @Test
    fun `returns false when only the optional field is answered but a required field exists`() {
        val required = mockTextField(required = true)
        val optional = mockTextField(required = false)
        val survey = mockSurvey(listOf(required, optional))
        // answering only the optional field must not satisfy the required one
        assertFalse(isSurveyResponseValid(survey, listOf(textResponse(optional))))
    }

    // ── caller contract ───────────────────────────────────────────────────────

    @Test
    fun `accepts an empty answer because it inspects only the question`() {
        // Reads only the question, never the value. Production never sends this: ifEmpty and mapNotNull drop it first.
        val required = mockTextField(required = true)
        val survey = mockSurvey(listOf(required))

        assertTrue(isSurveyResponseValid(survey, listOf(PreChatSurveyResponse.Text(required, ""))))
    }

    @Test
    fun `accepts a whitespace-only answer, which the caller's ifEmpty does not normalize`() {
        // Reachable, unlike the empty case above: ifEmpty fires only at length 0, so core is what rejects it.
        val required = mockTextField(required = true)
        val survey = mockSurvey(listOf(required))

        assertTrue(isSurveyResponseValid(survey, listOf(PreChatSurveyResponse.Text(required, "   "))))
    }
}
