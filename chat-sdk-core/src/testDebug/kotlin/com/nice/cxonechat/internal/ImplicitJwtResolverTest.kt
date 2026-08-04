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

import com.nice.cxonechat.OAuthToken
import com.nice.cxonechat.TokenDelegateListener
import com.nice.cxonechat.TokenRequestReason
import com.nice.cxonechat.exceptions.RuntimeChatException.TokenDelegationFailedException
import io.mockk.every
import io.mockk.mockk
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

internal class ImplicitJwtResolverTest {

    private fun chatWith(delegate: TokenDelegateListener?): ChatWithParameters = mockk(relaxed = true) {
        every { tokenDelegateListener } returns delegate
    }

    // ── success paths ────────────────────────────────────────────────────────

    @Test
    fun `requestImplicitToken returns success with JWT from delegate`() {
        val jwt = "valid-jwt-token"
        val chat = chatWith(TokenDelegateListener { OAuthToken(jwt) })

        val result = chat.requestImplicitToken()

        assertTrue(result.isSuccess)
        assertEquals(jwt, result.getOrNull()?.accessToken)
    }

    @Test
    fun `requestImplicitToken forwards TOKEN_INVALID reason to delegate`() {
        var capturedReason: TokenRequestReason? = null
        val chat = chatWith(TokenDelegateListener { reason ->
            capturedReason = reason
            OAuthToken("jwt")
        })

        chat.requestImplicitToken(reason = TokenRequestReason.TOKEN_INVALID)

        assertEquals(TokenRequestReason.TOKEN_INVALID, capturedReason)
    }

    @Test
    fun `requestImplicitToken forwards TOKEN_EXPIRED reason to delegate`() {
        var capturedReason: TokenRequestReason? = null
        val chat = chatWith(TokenDelegateListener { reason ->
            capturedReason = reason
            OAuthToken("jwt")
        })

        chat.requestImplicitToken(reason = TokenRequestReason.TOKEN_EXPIRED)

        assertEquals(TokenRequestReason.TOKEN_EXPIRED, capturedReason)
    }

    @Test
    fun `requestImplicitToken uses TOKEN_EXPIRED as default reason`() {
        var capturedReason: TokenRequestReason? = null
        val chat = chatWith(TokenDelegateListener { reason ->
            capturedReason = reason
            OAuthToken("jwt")
        })

        chat.requestImplicitToken() // no explicit reason argument

        assertEquals(TokenRequestReason.TOKEN_EXPIRED, capturedReason)
    }

    // ── failure — no delegate ─────────────────────────────────────────────────

    @Test
    fun `requestImplicitToken returns failure when tokenDelegateListener is null`() {
        val chat = chatWith(delegate = null)

        val result = chat.requestImplicitToken()

        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertNotNull(exception)
        assertTrue(exception is TokenDelegationFailedException)
    }

    // ── failure — delegate throws TokenDelegationFailedException ─────────────

    @Test
    fun `requestImplicitToken returns failure when delegate throws TokenDelegationFailedException`() {
        val cause = TokenDelegationFailedException("user cancelled")
        val chat = chatWith(TokenDelegateListener { throw cause })

        val result = chat.requestImplicitToken()

        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue(exception is TokenDelegationFailedException)
        assertEquals("user cancelled", exception.message)
    }

    // ── failure — delegate throws generic exception ───────────────────────────

    @Test
    fun `requestImplicitToken wraps generic exception into TokenDelegationFailedException`() {
        val originalCause = IllegalStateException("network timeout")
        val chat = chatWith(TokenDelegateListener { throw originalCause })

        val result = chat.requestImplicitToken()

        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue(exception is TokenDelegationFailedException)
        assertEquals(originalCause, exception.cause)
    }

    @Test
    fun `requestImplicitToken preserves message when wrapping generic exception`() {
        val chat = chatWith(TokenDelegateListener { throw RuntimeException("token fetch failed") })

        val result = chat.requestImplicitToken()

        assertFalse(result.isSuccess)
        val exception = result.exceptionOrNull() as? TokenDelegationFailedException
        assertNotNull(exception)
        assertEquals("token fetch failed", exception.message)
    }
}
