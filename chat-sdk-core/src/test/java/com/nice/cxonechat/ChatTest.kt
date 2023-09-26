/*
 * Copyright (c) 2021-2023. NICE Ltd. All rights reserved.
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

@file:Suppress("FunctionMaxLength")

package com.nice.cxonechat

import com.nice.cxonechat.internal.model.Visitor
import com.nice.cxonechat.internal.socket.WebSocketSpec
import com.nice.cxonechat.server.ServerResponse
import com.nice.cxonechat.tool.nextString
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.kotlin.any
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.Date

internal class ChatTest : AbstractChatTest() {

    @Test
    fun setDeviceToken_sendsExpectedMessage() {
        val token = nextString()
        val inOrder = inOrder(service)
        // The first empty token is set during instance creation
        inOrder.verify(service, times(1)).createOrUpdateVisitor(
            brandId = connection.brandId,
            visitorId = connection.visitorId.toString(),
            visitor = Visitor(connection)
        )
        chat.setDeviceToken(token)
        verify(storage, times(1)).deviceToken = token
        inOrder.verify(service, times(1)).createOrUpdateVisitor(
            brandId = connection.brandId,
            visitorId = connection.visitorId.toString(),
            visitor = Visitor(connection, token)
        )
        inOrder.verifyNoMoreInteractions()
    }

    @Test
    fun setDeviceToken_ignoresKnownToken() {
        val inOrder = inOrder(service)
        // The first empty token is set during instance creation
        inOrder.verify(service, times(1)).createOrUpdateVisitor(
            brandId = connection.brandId,
            visitorId = connection.visitorId.toString(),
            visitor = Visitor(connection)
        )
        val token = nextString()
        // Let's pretend that stored value equals to value which will be set
        whenever(storage.deviceToken).thenReturn(token)
        chat.setDeviceToken(token)
        verify(storage, times(0)).deviceToken = any()
        inOrder.verifyNoMoreInteractions()
    }

    @Test
    fun signOut_clearsStorage() {
        chat.signOut()
        verify(storage).clearStorage()
    }

    @Test
    fun signOut_closesConnection() {
        chat.signOut()
        verify(socket).close(WebSocketSpec.CLOSE_NORMAL_CODE, null)
    }

    @Test
    fun close_performsActions() {
        chat.close()
        socket.inOrder {
            verify().close(WebSocketSpec.CLOSE_NORMAL_CODE, null)
            Unit
        }
    }

    @Test
    fun build_authorization_updatesStorage_token() {
        val token = nextString()
        this serverResponds ServerResponse.TokenRefreshed(accessToken = token)
        verify(storage).authToken = token
    }

    @Test
    fun build_authorization_updatesStorage_tokenExpDate() {
        val captor = ArgumentCaptor.forClass(Date::class.java)
        this serverResponds ServerResponse.TokenRefreshed()
        verify(storage).authTokenExpDate = captor.capture()
    }
}
