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

package com.nice.cxonechat.ui.viewmodel

import com.nice.cxonechat.ChatInstanceProvider
import com.nice.cxonechat.ChatState
import com.nice.cxonechat.exceptions.RuntimeChatException
import com.nice.cxonechat.exceptions.RuntimeChatException.AuthorizationError
import com.nice.cxonechat.exceptions.RuntimeChatException.ConnectionTokenFailed
import com.nice.cxonechat.exceptions.RuntimeChatException.FeatureUnavailableException
import com.nice.cxonechat.exceptions.RuntimeChatException.InvalidAccessTokenException
import com.nice.cxonechat.exceptions.RuntimeChatException.ServerCommunicationError
import com.nice.cxonechat.exceptions.RuntimeChatException.TokenDelegationFailedException
import com.nice.cxonechat.log.Logger
import com.nice.cxonechat.ui.data.ChatErrorState
import com.nice.cxonechat.ui.util.ErrorGroup
import com.nice.cxonechat.ui.util.ErrorGroup.DO_NOTHING
import com.nice.cxonechat.ui.util.ErrorGroup.HIGH
import com.nice.cxonechat.ui.util.ErrorGroup.LOW
import com.nice.cxonechat.ui.util.ErrorGroup.SILENT_EXIT
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

/**
 * Tests for [ChatStateViewModel.handleChatRuntimeException], which maps
 * [RuntimeChatException] subtypes to [ErrorGroup] values that drive UI error dialogs.
 *
 * Incorrect mappings either hide critical auth failures (under-alerting) or
 * flood users with blocking dialogs for recoverable errors (over-alerting), so
 * every branch of the when-expression must be pinned.
 *
 */
internal class ChatStateViewModelExceptionMappingTest {

    private lateinit var provider: ChatInstanceProvider
    private lateinit var capturedListener: ChatInstanceProvider.Listener
    private lateinit var viewModel: ChatStateViewModel

    @Before
    fun setUp() {
        val listenerSlot = slot<ChatInstanceProvider.Listener>()
        provider = mockk(relaxed = true) {
            every { chatState } returns ChatState.Initial
            every { addListener(capture(listenerSlot)) } just Runs
        }
        val logger: Logger = mockk(relaxed = true)

        viewModel = ChatStateViewModel(provider, logger)
        capturedListener = listenerSlot.captured
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private fun fireException(exception: RuntimeChatException): ChatErrorState {
        capturedListener.onChatRuntimeException(exception)
        return viewModel.chatErrorState.value
    }

    // AuthorizationError, ServerCommunicationError, ConnectionTokenFailed, FeatureUnavailableException,
    // and InvalidAccessTokenException have `internal constructor` in chat-sdk-core; use mockk so they
    // can be instantiated from this module while still satisfying the `is` type checks.
    private inline fun <reified T : RuntimeChatException> mockException(message: String? = null): T =
        mockk<T>(relaxed = true).also { if (message != null) every { it.message } returns message }

    private inline fun <reified T : RuntimeChatException> assertErrorGroup(expected: ErrorGroup) {
        val state = fireException(mockException<T>())
        assertEquals(
            expected,
            state.errorGroup,
            "${T::class.simpleName} should map to $expected but got ${state.errorGroup}"
        )
    }

    // ── AuthorizationError → HIGH (blocking dialog) ───────────────────────────

    @Test
    fun `AuthorizationError maps to HIGH`() {
        assertErrorGroup<AuthorizationError>(HIGH)
    }

    // ── ServerCommunicationError → LOW (dismissible banner) ──────────────────

    @Test
    fun `ServerCommunicationError maps to LOW`() {
        assertErrorGroup<ServerCommunicationError>(LOW)
    }

    // ── ConnectionTokenFailed → SILENT_EXIT ───────────────────────────────────

    @Test
    fun `ConnectionTokenFailed maps to SILENT_EXIT`() {
        assertErrorGroup<ConnectionTokenFailed>(SILENT_EXIT)
    }

    // ── FeatureUnavailableException → SILENT_EXIT ─────────────────────────────

    @Test
    fun `FeatureUnavailableException maps to SILENT_EXIT`() {
        assertErrorGroup<FeatureUnavailableException>(SILENT_EXIT)
    }

    // ── Other new exceptions → DO_NOTHING (log only, no UI) ──────────────────

    @Test
    fun `InvalidAccessTokenException maps to DO_NOTHING`() {
        assertErrorGroup<InvalidAccessTokenException>(DO_NOTHING)
    }

    @Test
    fun `TokenDelegationFailedException maps to DO_NOTHING`() {
        assertErrorGroup<TokenDelegationFailedException>(DO_NOTHING)
    }

    // ── error message is propagated ───────────────────────────────────────────

    @Test
    fun `exception message is forwarded into ChatErrorState`() {
        // ServerCommunicationError maps to LOW, which preserves the message (HIGH suppresses it)
        val exception = mockException<ServerCommunicationError>("specific server failure message")
        val state = fireException(exception)
        assertEquals("specific server failure message", state.message)
    }

    // ── resetError clears state ───────────────────────────────────────────────

    @Test
    fun `resetError restores DO_NOTHING error group`() {
        fireException(mockException<AuthorizationError>())

        viewModel.resetError()

        assertEquals(DO_NOTHING, viewModel.chatErrorState.value.errorGroup)
    }
}
