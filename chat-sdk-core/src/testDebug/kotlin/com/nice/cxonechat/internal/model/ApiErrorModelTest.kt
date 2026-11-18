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
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * Tests for [ApiErrorModel] JSON deserialization.
 *
 * The model is used to parse non-2xx HTTP responses from the backend.
 * Key concerns:
 * - All known fields deserialize correctly.
 * - Unknown fields are silently ignored ([Default.serializer] uses ignoreUnknownKeys = true).
 * - All fields are optional and default to null when absent.
 * - Missing or null top-level `error` object is represented as null.
 */
internal class ApiErrorModelTest {

    @Test
    fun `deserializes all known fields from full JSON`() {
        val json = """
            {
                "error": {
                    "errorCode": "AuthorizationFailed",
                    "errorMessage": "Invalid token",
                    "transactionId": "tx-abc-123"
                }
            }
        """.trimIndent()

        val model = decode(json)

        assertNotNull(model.error)
        assertEquals("AuthorizationFailed", model.error.errorCode)
        assertEquals("Invalid token", model.error.errorMessage)
        assertEquals("tx-abc-123", model.error.transactionId)
    }

    @Test
    fun `missing transactionId field defaults to null`() {
        val json = """
            {
                "error": {
                    "errorCode": "InternalError",
                    "errorMessage": "Something failed"
                }
            }
        """.trimIndent()

        val model = decode(json)

        assertNotNull(model.error)
        assertNull(model.error.transactionId)
        assertEquals("InternalError", model.error.errorCode)
        assertEquals("Something failed", model.error.errorMessage)
    }

    @Test
    fun `unknown fields in error object are silently ignored`() {
        val json = """
            {
                "error": {
                    "errorCode": "SomeCode",
                    "errorMessage": "Some message",
                    "transactionId": "tx-1",
                    "newFieldFromFutureBackend": "value",
                    "anotherField": 42
                }
            }
        """.trimIndent()

        // Must not throw; known fields should still parse correctly.
        val model = decode(json)

        assertNotNull(model.error)
        assertEquals("SomeCode", model.error.errorCode)
        assertEquals("Some message", model.error.errorMessage)
        assertEquals("tx-1", model.error.transactionId)
    }

    @Test
    fun `unknown top-level fields are silently ignored`() {
        val json = """
            {
                "error": {
                    "errorCode": "SomeCode",
                    "errorMessage": "Some message"
                },
                "topLevelUnknownField": "ignored"
            }
        """.trimIndent()

        val model = decode(json)

        assertNotNull(model.error)
        assertEquals("SomeCode", model.error.errorCode)
    }

    @Test
    fun `missing error object results in null error property`() {
        val json = """{}"""

        val model = decode(json)

        assertNull(model.error)
    }

    @Test
    fun `explicit null error object results in null error property`() {
        val json = """{"error": null}"""

        val model = decode(json)

        assertNull(model.error)
    }

    @Test
    fun `empty error object yields all-null fields`() {
        val json = """{"error": {}}"""

        val model = decode(json)

        assertNotNull(model.error)
        assertNull(model.error.errorCode)
        assertNull(model.error.errorMessage)
        assertNull(model.error.transactionId)
    }

    // ---

    private fun decode(json: String): ApiErrorModel =
        Default.serializer.decodeFromString(json)
}
