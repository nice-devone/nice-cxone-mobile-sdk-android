/*
 * Copyright (c) 2021-2024. NICE Ltd. All rights reserved.
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

import com.nice.cxonechat.enums.ErrorType.ArchivingThreadFailed
import com.nice.cxonechat.enums.ErrorType.RecoveringLivechatFailed
import com.nice.cxonechat.enums.ErrorType.RecoveringThreadFailed
import com.nice.cxonechat.enums.ErrorType.SendingMessageFailed
import com.nice.cxonechat.enums.ErrorType.SendingOfflineMessageFailed
import com.nice.cxonechat.enums.ErrorType.SendingOutboundFailed
import com.nice.cxonechat.enums.ErrorType.SendingTranscriptFailed
import com.nice.cxonechat.enums.ErrorType.UpdatingThreadFailed
import com.nice.cxonechat.exceptions.RuntimeChatException
import com.nice.cxonechat.server.ServerResponse
import org.junit.Test
import kotlin.test.assertEquals

internal class ChatBackendErrorReportingTest : AbstractChatTest() {

    @Test
    fun test_SendingMessageFailed_is_reported() {
        this serverResponds ServerResponse.ErrorResponse(SendingMessageFailed.value)
        val last = this.chatStateListener.onChatRuntimeExceptions.last()
        assertServerCommunicationError(SendingMessageFailed.value, last)
    }

    @Test
    fun test_RecoveringLivechatFailed_is_not_reported() {
        this serverResponds ServerResponse.ErrorResponse(RecoveringLivechatFailed.value)
        val chatStateListener = this.chatStateListener
        assert(chatStateListener.onChatRuntimeExceptions.isEmpty())
    }

    @Test
    fun test_RecoveringThreadFailed_is_reported() {
        this serverResponds ServerResponse.ErrorResponse(RecoveringThreadFailed.value)
        val last = this.chatStateListener.onChatRuntimeExceptions.last()
        assertServerCommunicationError(RecoveringThreadFailed.value, last)
    }

    @Test
    fun test_SendingOutboundFailed_is_reported() {
        this serverResponds ServerResponse.ErrorResponse(SendingOutboundFailed.value)
        val last = this.chatStateListener.onChatRuntimeExceptions.last()
        assertServerCommunicationError(SendingOutboundFailed.value, last)
    }

    @Test
    fun test_UpdatingThreadFailed_is_reported() {
        this serverResponds ServerResponse.ErrorResponse(UpdatingThreadFailed.value)
        val last = this.chatStateListener.onChatRuntimeExceptions.last()
        assertServerCommunicationError(UpdatingThreadFailed.value, last)
    }

    @Test
    fun test_ArchivingThreadFailed_is_reported() {
        this serverResponds ServerResponse.ErrorResponse(ArchivingThreadFailed.value)
        val last = this.chatStateListener.onChatRuntimeExceptions.last()
        assertServerCommunicationError(ArchivingThreadFailed.value, last)
    }

    @Test
    fun test_SendingTranscriptFailed_is_reported() {
        this serverResponds ServerResponse.ErrorResponse(SendingTranscriptFailed.value)
        val last = this.chatStateListener.onChatRuntimeExceptions.last()
        assertServerCommunicationError(SendingTranscriptFailed.value, last)
    }

    @Test
    fun test_SendingOfflineMessageFailed_is_reported() {
        this serverResponds ServerResponse.ErrorResponse(SendingOfflineMessageFailed.value)
        val last = this.chatStateListener.onChatRuntimeExceptions.last()
        assertServerCommunicationError(SendingOfflineMessageFailed.value, last)
    }

    private fun assertServerCommunicationError(expectedMessage: String, actualException: RuntimeChatException) {
        assertEquals(RuntimeChatException.ServerCommunicationError::class, actualException::class)
        assertEquals(expectedMessage, actualException.message)
    }
}
