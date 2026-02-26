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

import com.nice.cxonechat.ChatThreadEventHandler.OnEventErrorListener
import com.nice.cxonechat.ChatThreadEventHandler.OnEventResponseListener
import com.nice.cxonechat.ChatThreadEventHandler.OnEventSentListener
import com.nice.cxonechat.ChatThreadEventHandlerActions.sendTranscript
import com.nice.cxonechat.exceptions.RuntimeChatException
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

class ChatThreadEventHandlerActionsTest {

    @Test
    fun `sendTranscript triggers success response`() {
        val handler = mockk<ChatThreadEventHandler>(relaxed = true)
        val sentListener = mockk<OnEventSentListener>(relaxed = true)
        val errorListener = mockk<OnEventErrorListener>(relaxed = true)
        val responseListener = mockk<OnEventResponseListener>(relaxed = true)

        every {
            handler.trigger(any(), sentListener, errorListener, responseListener)
        } answers {
            sentListener.onSent()
            responseListener.onResponse(EventResponse.Success)
        }

        handler.sendTranscript(
            email = "test@example.com",
            listener = sentListener,
            errorListener = errorListener,
            onEventResponseListener = responseListener
        )

        verify { sentListener.onSent() }
        verify { responseListener.onResponse(EventResponse.Success) }
        verify(exactly = 0) { errorListener.onError(any()) }
    }

    @Test
    fun `sendTranscript triggers error response`() {
        val handler = mockk<ChatThreadEventHandler>(relaxed = true)
        val sentListener = mockk<OnEventSentListener>(relaxed = true)
        val errorListener = mockk<OnEventErrorListener>(relaxed = true)
        val responseListener = mockk<OnEventResponseListener>(relaxed = true)
        val error = RuntimeChatException.ServerCommunicationError("Failed")

        every {
            handler.trigger(any(), sentListener, errorListener, responseListener)
        } answers {
            sentListener.onSent()
            errorListener.onError(error)
        }

        handler.sendTranscript(
            email = "test@example.com",
            listener = sentListener,
            errorListener = errorListener,
            onEventResponseListener = responseListener
        )

        verify { sentListener.onSent() }
        verify { errorListener.onError(error) }
        verify(exactly = 0) { responseListener.onResponse(any()) }
    }
}
