package com.nice.cxonechat.internal

import com.nice.cxonechat.Authorization
import com.nice.cxonechat.enums.EventType.CustomerAuthorized
import com.nice.cxonechat.enums.EventType.TokenRefreshed
import com.nice.cxonechat.event.AuthorizeCustomerEvent
import com.nice.cxonechat.event.ReconnectCustomerEvent
import com.nice.cxonechat.internal.copy.ConnectionCopyable.Companion.asCopyable
import com.nice.cxonechat.internal.model.network.EventCustomerAuthorized
import com.nice.cxonechat.internal.model.network.EventTokenRefreshed
import com.nice.cxonechat.socket.EventCallback.Companion.addCallback
import java.util.UUID

internal class ChatAuthorization(
    private val origin: ChatWithParameters,
    authorization: Authorization,
) : ChatWithParameters by origin {

    private val customerAuthorized = socket.addCallback<EventCustomerAuthorized>(CustomerAuthorized) { model ->
        val authorizationEnabled = origin.configuration.isAuthorizationEnabled
        connection = connection.asCopyable().copy(
            firstName = if (authorizationEnabled) {
                model.firstName ?: connection.firstName
            } else {
                connection.firstName
            },
            lastName = if (authorizationEnabled) {
                model.lastName ?: connection.lastName
            } else {
                connection.lastName
            },
            consumerId = model.id
        )
        storage.authToken = model.token
        storage.authTokenExpDate = model.tokenExpiresAt
        storage.consumerId = connection.consumerId
    }

    private val tokenRefresh = socket.addCallback<EventTokenRefreshed>(TokenRefreshed) { model ->
        storage.authToken = model.token
        storage.authTokenExpDate = model.expiresAt
    }

    init {
        if (storage.consumerId == null)
            connection = connection.asCopyable().copy(consumerId = UUID.randomUUID())
        val event = when (storage.authToken == null) {
            true -> AuthorizeCustomerEvent(authorization.code, authorization.verifier)
            else -> ReconnectCustomerEvent
        }
        events().trigger(event)
    }

    override fun close() {
        customerAuthorized.cancel()
        tokenRefresh.cancel()
        origin.close()
    }
}
