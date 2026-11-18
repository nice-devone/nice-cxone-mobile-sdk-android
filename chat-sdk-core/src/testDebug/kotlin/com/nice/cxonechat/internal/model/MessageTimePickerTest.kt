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

import com.nice.cxonechat.internal.model.MessageDirectionModel.ToAgent
import com.nice.cxonechat.internal.model.MessageDirectionModel.ToClient
import com.nice.cxonechat.internal.model.network.MessagePolyContent
import com.nice.cxonechat.internal.model.network.TimeSlotModel
import com.nice.cxonechat.internal.model.network.WrappedText
import com.nice.cxonechat.message.MessageDirection
import com.nice.cxonechat.model.makeMessageModel
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlin.time.Instant

/**
 * Unit tests for [MessageTimePicker] property mapping.
 *
 * **Why these matter:**
 * `MessageTimePicker` is the internal implementation that bridges the `TIME_PICKER` network model
 * to the public `Message.TimePicker` API consumed by integrators.  Incorrect mapping means
 * integrators show wrong titles, the wrong set of time slots, or the AI-authorship indicator on
 * the wrong messages.
 *
 * Key concerns:
 * - `title` and `popupTitle` are distinct fields in the payload and must not be confused.
 * - `isAiGenerated` must be `false` for customer→agent messages regardless of the model flag —
 *   only agent→client messages can carry the AI indicator.
 * - `timeSlots` must expose all slots returned by the server, in order.
 */
internal class MessageTimePickerTest {

    private val slotA = TimeSlotModel(
        id = "slot-a",
        duration = 1800L,
        startTime = Instant.parse("2024-09-10T10:00:00Z"),
    )
    private val slotB = TimeSlotModel(
        id = "slot-b",
        duration = 3600L,
        startTime = Instant.parse("2024-09-10T14:00:00Z"),
    )

    private val timePickerContent = MessagePolyContent.TimePicker(
        fallbackText = "Pick a slot",
        payload = MessagePolyContent.TimePicker.Payload(
            title = WrappedText("Choose a date"),
            event = MessagePolyContent.TimePicker.Payload.Event(
                title = WrappedText("Popup event title"),
                timeSlots = listOf(slotA, slotB),
            ),
        ),
    )

    private fun timePicker(
        direction: MessageDirectionModel = ToAgent,
        isAiGenerated: Boolean = false,
    ) = MessageTimePicker(
        model = makeMessageModel(
            messageContent = timePickerContent,
            direction = direction,
            isAiGenerated = isAiGenerated,
        )
    )

    // ---- title and popupTitle ----

    @Test
    fun `title maps payload title content`() {
        assertEquals("Choose a date", timePicker().title)
    }

    @Test
    fun `popupTitle maps event title content and is distinct from title`() {
        val picker = timePicker()
        assertEquals("Popup event title", picker.popupTitle)
        // Confirm they are different fields — a swapped mapping would make them equal.
        // assertNotEquals rather than assertFalse(a == b) so a failure prints both strings.
        assertNotEquals(picker.title, picker.popupTitle)
    }

    // ---- fallbackText ----

    @Test
    fun `fallbackText maps from content fallbackText field`() {
        assertEquals("Pick a slot", timePicker().fallbackText)
    }

    // ---- timeSlots ----

    @Test
    fun `timeSlots maps all slots preserving order`() {
        val slots = timePicker().timeSlots.toList()
        assertEquals(2, slots.size)
        assertEquals("slot-a", slots[0].id)
        assertEquals(1800L, slots[0].duration)
        assertEquals(slotA.startTime, slots[0].startTime)
        assertEquals("slot-b", slots[1].id)
        assertEquals(3600L, slots[1].duration)
    }

    // ---- direction ----

    @Test
    fun `direction is ToAgent when model direction is ToAgent`() {
        assertEquals(MessageDirection.ToAgent, timePicker(direction = ToAgent).direction)
    }

    @Test
    fun `direction is ToClient when model direction is ToClient`() {
        assertEquals(MessageDirection.ToClient, timePicker(direction = ToClient).direction)
    }

    // ---- isAiGenerated ----

    @Test
    fun `isAiGenerated is false for ToAgent direction even when model flag is true`() {
        // Customer-to-agent messages must never carry the AI indicator, regardless of the model flag.
        assertFalse(timePicker(direction = ToAgent, isAiGenerated = true).isAiGenerated)
    }

    @Test
    fun `isAiGenerated is true for ToClient direction when model flag is true`() {
        assertTrue(timePicker(direction = ToClient, isAiGenerated = true).isAiGenerated)
    }

    @Test
    fun `isAiGenerated is false for ToClient direction when model flag is false`() {
        assertFalse(timePicker(direction = ToClient, isAiGenerated = false).isAiGenerated)
    }

    // ---- toString ----

    @Test
    fun `toString contains title popupTitle and fallbackText`() {
        val result = timePicker().toString()
        assertTrue(result.contains("title='Choose a date'"), "toString should contain title: $result")
        assertTrue(result.contains("popupTitle='Popup event title'"), "toString should contain popupTitle: $result")
        assertTrue(result.contains("fallbackText='Pick a slot'"), "toString should contain fallbackText: $result")
    }
}
