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

package com.nice.cxonechat.internal.model.network

import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Clock
import org.junit.Test

internal class AccessTokenTest {

    @Test
    fun `isExpired returns false for a token that expires far in the future`() {
        val token = AccessToken(token = "abc", expiresIn = 3600L) // 1 hour
        assertFalse(token.isExpired)
    }

    @Test
    fun `isExpired returns true for a token with negative expiresIn`() {
        val token = AccessToken(token = "abc", expiresIn = -1L) // already expired
        assertTrue(token.isExpired)
    }

    @Test
    fun `expiresAt is not in the future for a token with zero expiresIn`() {
        val before = Clock.System.now()
        val token = AccessToken(token = "abc", expiresIn = 0L)
        val after = Clock.System.now()

        assertTrue(token.expiresAt >= before)
        assertTrue(token.expiresAt <= after)
    }

    @Test
    fun `expiresAt is createdAt plus expiresIn seconds`() {
        val expiresIn = 7200L
        val before = System.currentTimeMillis()
        val token = AccessToken(token = "abc", expiresIn = expiresIn)
        val after = System.currentTimeMillis()

        val expiresAtMs = token.expiresAt.toEpochMilliseconds()
        assertTrue(expiresAtMs >= before + expiresIn * 1000L)
        assertTrue(expiresAtMs <= after + expiresIn * 1000L)
    }
}
