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

package com.nice.cxonechat.internal.serializer

import kotlinx.serialization.Serializable
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

internal class InstantSerializersTest {

    @Serializable
    private data class WithInstantString(
        @Serializable(Default.InstantSerializer::class)
        val ts: Instant,
    )

    @Test
    fun `InstantSerializer deserializes standard uppercase-Z ISO 8601 string`() {
        val json = """{"ts":"2024-03-15T10:30:00Z"}"""
        val result = Default.serializer.decodeFromString(WithInstantString.serializer(), json)
        assertEquals(Instant.parse("2024-03-15T10:30:00Z"), result.ts)
    }

    @Test
    fun `InstantSerializer deserializes legacy lowercase-z UTC suffix`() {
        val json = """{"ts":"2024-03-15T10:30:00z"}"""
        val result = Default.serializer.decodeFromString(WithInstantString.serializer(), json)
        assertEquals(Instant.parse("2024-03-15T10:30:00Z"), result.ts)
    }

    @Test
    fun `InstantSerializer deserializes string with millisecond precision`() {
        val json = """{"ts":"2024-03-15T10:30:00.123Z"}"""
        val result = Default.serializer.decodeFromString(WithInstantString.serializer(), json)
        assertEquals(Instant.parse("2024-03-15T10:30:00.123Z"), result.ts)
    }

    @Test
    fun `InstantSerializer serializes instant with non-zero ms to string with fractional seconds`() {
        val instant = Instant.parse("2024-03-15T10:00:00.123Z")
        val json = Default.serializer.encodeToString(WithInstantString.serializer(), WithInstantString(instant))
        assertEquals("""{"ts":"2024-03-15T10:00:00.123Z"}""", json.replace("\\s".toRegex(), ""))
    }

    @Test
    fun `InstantSerializer serializes instant with zero ms to string without fractional seconds`() {
        val instant = Instant.parse("2024-01-01T00:00:00Z")
        val json = Default.serializer.encodeToString(WithInstantString.serializer(), WithInstantString(instant))
        assertEquals("""{"ts":"2024-01-01T00:00:00Z"}""", json.replace("\\s".toRegex(), ""))
    }

    @Serializable
    private data class WithInstantNumber(
        @Serializable(Default.InstantAsNumberSerializer::class)
        val ts: Instant,
    )

    @Test
    fun `InstantAsNumberSerializer deserializes epoch-milliseconds value as-is`() {
        val instant = Instant.parse("2024-03-15T10:30:00.123Z")
        val epochMs = instant.toEpochMilliseconds()
        val json = """{"ts":$epochMs}"""
        val result = Default.serializer.decodeFromString(WithInstantNumber.serializer(), json)
        assertEquals(instant, result.ts)
    }

    @Test
    fun `InstantAsNumberSerializer deserializes epoch-seconds value by multiplying by 1000`() {
        val instant = Instant.parse("2024-03-15T10:30:00Z")
        val epochSeconds = instant.toEpochMilliseconds() / 1000L
        val json = """{"ts":$epochSeconds}"""
        val result = Default.serializer.decodeFromString(WithInstantNumber.serializer(), json)
        assertEquals(Instant.fromEpochMilliseconds(epochSeconds * 1000L), result.ts)
    }

    @Test
    fun `InstantAsNumberSerializer serializes Instant as epoch milliseconds`() {
        val instant = Instant.parse("2024-03-15T10:30:00.123Z")
        val epochMs = instant.toEpochMilliseconds()
        val json = Default.serializer.encodeToString(WithInstantNumber.serializer(), WithInstantNumber(instant))
        assertEquals("""{"ts":$epochMs}""", json.replace("\\s".toRegex(), ""))
    }

    @Test
    fun `InstantAsNumberSerializer threshold value 9999999999 is treated as epoch seconds`() {
        val epochSeconds = 9_999_999_999L
        val json = """{"ts":$epochSeconds}"""
        val result = Default.serializer.decodeFromString(WithInstantNumber.serializer(), json)
        assertEquals(Instant.fromEpochMilliseconds(epochSeconds * 1000L), result.ts)
    }

    @Test
    fun `InstantAsNumberSerializer threshold value 10000000000 is treated as epoch milliseconds`() {
        val epochMs = 10_000_000_000L
        val json = """{"ts":$epochMs}"""
        val result = Default.serializer.decodeFromString(WithInstantNumber.serializer(), json)
        assertEquals(Instant.fromEpochMilliseconds(epochMs), result.ts)
    }
}
