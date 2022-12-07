@file:Suppress("FunctionMaxLength")

package com.nice.cxonechat

import com.nice.cxonechat.server.ServerRequest
import com.nice.cxonechat.server.ServerResponse
import com.nice.cxonechat.tool.nextString
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.verify
import java.util.Date

internal class ChatTest : AbstractChatTest() {

    @Test
    fun setDeviceToken_sendsExpectedMessage() {
        val token = nextString()
        assertSendText(ServerRequest.StoreVisitor(connection, token)) {
            chat.setDeviceToken(token)
        }
    }

    @Test
    fun signOut_clearsStorage() {
        chat.signOut()
        verify(storage).clearStorage()
    }

    @Test
    fun signOut_closesConnection() {
        chat.signOut()
        verify(socket).sendClose()
    }

    @Test
    fun close_performsActions() {
        chat.close()
        socket.inOrder {
            verify().sendClose()
            verify().clearListeners()
            verify().disconnect()
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
