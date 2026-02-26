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

package com.nice.cxonechat.enums

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class AuthenticationTypeTest {
    private val json = Json { encodeDefaults = true }

    @Test
    fun `serializes to correct json values`() {
        assertEquals("\"anonymous\"", json.encodeToString(AuthenticationType.Anonymous))
        assertEquals("\"securedCookie\"", json.encodeToString(AuthenticationType.SecuredCookie))
        assertEquals("\"thirdPartyOAuth\"", json.encodeToString(AuthenticationType.ThirdPartyOAuth))
    }

    @Test
    fun `deserializes from correct json values`() {
        assertEquals(
            AuthenticationType.Anonymous,
            runCatching {
                json.decodeFromString<AuthenticationType>("\"anonymous\"")
            }.getOrNull()
        )
        assertEquals(
            AuthenticationType.SecuredCookie,
            runCatching {
                json.decodeFromString<AuthenticationType>("\"securedCookie\"")
            }.getOrNull()
        )
        assertEquals(
            AuthenticationType.ThirdPartyOAuth,
            runCatching {
                json.decodeFromString<AuthenticationType>("\"thirdPartyOAuth\"")
            }.getOrNull()
        )
    }

    @Test
    fun `deserializes invalid json string throws exception`() {
        assertThrows(SerializationException::class.java) {
            json.decodeFromString<AuthenticationType>("\"invalidType\"")
        }
    }

    @Test
    fun `bidirectional serialization round trip`() {
        val types = listOf(
            AuthenticationType.Anonymous,
            AuthenticationType.SecuredCookie,
            AuthenticationType.ThirdPartyOAuth
        )

        types.forEach { type ->
            val serialized = json.encodeToString(type)
            val deserialized = runCatching {
                json.decodeFromString<AuthenticationType>(serialized)
            }.getOrNull()
            assertEquals(type, deserialized)
        }
    }
}
