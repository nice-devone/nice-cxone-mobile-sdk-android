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
import com.nice.cxonechat.ChatInstanceProvider.Listener
import com.nice.cxonechat.ChatState
import com.nice.cxonechat.exceptions.RuntimeChatException.AuthorizationError
import com.nice.cxonechat.exceptions.RuntimeChatException.ConnectionTokenFailed
import com.nice.cxonechat.exceptions.RuntimeChatException.ServerCommunicationError
import com.nice.cxonechat.log.LoggerNoop
import com.nice.cxonechat.ui.util.ErrorGroup
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test

internal class ChatStateViewModelTest {

    private val chatInstanceProvider = mockk<ChatInstanceProvider>(relaxed = true)
    private val listenerSlot = slot<Listener>()
    private lateinit var viewModel: ChatStateViewModel
    private lateinit var capturedListener: Listener

    @Before
    fun setUp() {
        every { chatInstanceProvider.chatState } returns ChatState.Initial
        every { chatInstanceProvider.addListener(capture(listenerSlot)) } just runs
        viewModel = ChatStateViewModel(chatInstanceProvider, LoggerNoop)
        capturedListener = listenerSlot.captured
    }

    @Test
    fun `SdkNotSupported state triggers HIGH_SPECIFIC error`() {
        capturedListener.onChatStateChanged(ChatState.SdkNotSupported)

        assertSame(ErrorGroup.HIGH_SPECIFIC, viewModel.chatErrorState.value.errorGroup)
    }

    @Test
    fun `SdkNotSupported state still updates internal state`() {
        capturedListener.onChatStateChanged(ChatState.SdkNotSupported)

        assertSame(ChatState.SdkNotSupported, viewModel.state.value)
    }

    @Test
    fun `SdkNotSupported error passes null message so screen resolves localized string`() {
        capturedListener.onChatStateChanged(ChatState.SdkNotSupported)

        assertNull(viewModel.chatErrorState.value.message)
    }

    @Test
    fun `ConnectionTokenFailed exception triggers SILENT_EXIT`() {
        val exception = mockk<ConnectionTokenFailed>(relaxed = true)
        capturedListener.onChatRuntimeException(exception)

        assertSame(ErrorGroup.SILENT_EXIT, viewModel.chatErrorState.value.errorGroup)
    }

    @Test
    fun `normal state change does not set error state`() {
        capturedListener.onChatStateChanged(ChatState.Ready)

        assertSame(ErrorGroup.DO_NOTHING, viewModel.chatErrorState.value.errorGroup)
    }

    @Test
    fun `SdkNotSupported initial state triggers HIGH_SPECIFIC error without listener callback`() {
        every { chatInstanceProvider.chatState } returns ChatState.SdkNotSupported
        val viewModelWithInitialSdkError = ChatStateViewModel(chatInstanceProvider, LoggerNoop)

        assertSame(ErrorGroup.HIGH_SPECIFIC, viewModelWithInitialSdkError.chatErrorState.value.errorGroup)
    }

    @Test
    fun `AuthorizationError suppresses exception message to prevent server text leaking to users`() {
        val exception = mockk<AuthorizationError>(relaxed = true)
        every { exception.message } returns "server-internal detail"
        capturedListener.onChatRuntimeException(exception)

        assertSame(ErrorGroup.HIGH, viewModel.chatErrorState.value.errorGroup)
        assertNull(viewModel.chatErrorState.value.message)
    }

    @Test
    fun `ServerCommunicationError preserves exception message for LOW error display`() {
        val exception = mockk<ServerCommunicationError>(relaxed = true)
        every { exception.message } returns "transient network error"
        capturedListener.onChatRuntimeException(exception)

        assertSame(ErrorGroup.LOW, viewModel.chatErrorState.value.errorGroup)
        assertEquals("transient network error", viewModel.chatErrorState.value.message)
    }

    @Test
    fun `chatState change between initialState capture and addListener is caught by post-registration resync`() {
        every { chatInstanceProvider.chatState } returnsMany listOf(ChatState.Initial, ChatState.SdkNotSupported)
        val viewModelWithRace = ChatStateViewModel(chatInstanceProvider, LoggerNoop)

        assertSame(ErrorGroup.HIGH_SPECIFIC, viewModelWithRace.chatErrorState.value.errorGroup)
    }
}
