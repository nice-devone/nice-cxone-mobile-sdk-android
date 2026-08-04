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

package com.nice.cxonechat

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for the [OAuthToken] companion factory.
 *
 * Security invariant: [OAuthToken.toString] must **never** expose the raw access token,
 * because ViewModel and SDK log statements routinely call `toString()` on objects they hold.
 */
internal class OAuthTokenTest {

    // ── factory ───────────────────────────────────────────────────────────────

    @Test
    fun `factory sets accessToken correctly`() {
        val token = OAuthToken(accessToken = "my-secret-jwt")

        assertEquals("my-secret-jwt", token.accessToken)
    }

    @Test
    fun `factory sets expiryDateMillis when provided`() {
        val expiry = 1_000_000L
        val token = OAuthToken(accessToken = "jwt", expiryDateMillis = expiry)

        assertEquals(expiry, token.expiryDateMillis)
    }

    @Test
    fun `factory defaults expiryDateMillis to null`() {
        val token = OAuthToken(accessToken = "jwt")

        assertNull(token.expiryDateMillis)
    }

    // ── toString security ─────────────────────────────────────────────────────

    @Test
    fun `toString does not contain the raw access token`() {
        val rawToken = "super-secret-access-token"
        val token = OAuthToken(accessToken = rawToken, expiryDateMillis = 99L)

        val str = token.toString()

        assertFalse(
            str.contains(rawToken),
            "toString() must NOT expose the raw access token, but got: $str"
        )
    }

    @Test
    fun `toString contains redacted marker`() {
        val token = OAuthToken(accessToken = "secret", expiryDateMillis = 42L)

        val str = token.toString()

        assertTrue(
            str.contains("[redacted]"),
            "toString() should include '[redacted]' to signal the token is hidden, but got: $str"
        )
    }

    @Test
    fun `toString includes expiryDateMillis`() {
        val expiry = 123_456_789L
        val token = OAuthToken(accessToken = "secret", expiryDateMillis = expiry)

        val str = token.toString()

        assertTrue(
            str.contains(expiry.toString()),
            "toString() should include expiryDateMillis=$expiry, but got: $str"
        )
    }

    @Test
    fun `toString with null expiry is safe`() {
        val token = OAuthToken(accessToken = "secret")

        val str = token.toString()

        assertFalse(str.contains("secret"))
    }
}
