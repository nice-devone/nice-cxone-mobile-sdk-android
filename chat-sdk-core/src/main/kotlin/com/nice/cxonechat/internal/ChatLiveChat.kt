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

import com.nice.cxonechat.enums.ErrorType.RecoveringLivechatFailed
import com.nice.cxonechat.enums.EventType.LivechatRecovered
import com.nice.cxonechat.exceptions.RuntimeChatException.ServerCommunicationError
import com.nice.cxonechat.internal.model.network.EventLiveChatThreadRecovered
import com.nice.cxonechat.internal.socket.ErrorCallback.Companion.addErrorCallback
import com.nice.cxonechat.internal.socket.EventCallback.Companion.addCallback
import com.nice.cxonechat.internal.socket.SocketConnectionListener
import com.nice.cxonechat.state.Configuration.Feature
import com.nice.cxonechat.util.onFailure
import com.nice.cxonechat.util.onSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withContext
import java.io.IOException
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

internal class ChatLiveChat(
    private val origin: ChatWithParameters,
) : ChatWithParameters by origin {

    private var availabilityExpiration: Instant = Instant.fromEpochMilliseconds(0)

    init {
        isChatAvailable = false
    }

    private fun getAvailability(): Boolean {
        if (availabilityExpiration < Clock.System.now()) {
            try {
                origin
                    .service
                    .getChannelAvailability(
                        origin.connection.brandId.toString(),
                        origin.connection.channelId
                    )
                    .execute()
                    .onSuccess {
                        availabilityExpiration = Clock.System.now() + AVAILABILTY_TTL
                        isChatAvailable = body()?.isOnline != false
                    }
                    .onFailure {
                        isChatAvailable = false
                    }
            } catch (e: IOException) {
                // isChatAvailable is "last known" — must reflect this like onFailure {} does, else
                // getChannelAvailability() returns false while the property stays stale true.
                isChatAvailable = false
                // Pre-socket failure (e.g. offline) before any socket opens — ReconnectingListener
                // never sees it. Wrapped for ChatInstanceProvider's auto-retry.
                throw ChannelAvailabilityFailedException("Failed to fetch channel availability", e)
            }
        }

        return isChatAvailable
    }

    override suspend fun getChannelAvailability(): Boolean = withContext(entrails.threading.ioDispatcher) {
        try {
            getAvailability()
        } catch (expected: ChannelAvailabilityFailedException) {
            // Preserves the no-throw Boolean contract; connect() calls getAvailability() directly
            // (not through this wrapper) to get the exception for auto-retry.
            false
        }
    }

    override suspend fun connect() {
        if (!getAvailability()) {
            // If chat isn't available immediately transition to ready with no websocket.
            chatStateListener?.onReady()
            return
        }
        origin.connect()
        chatStateListener?.onConnected()
        var recoveryException: ServerCommunicationError? = null
        val recoveredDeferred = CompletableDeferred<Unit>()
        // Guard: complete the deferred if the socket drops so connect() never suspends indefinitely.
        val socketDropListener = SocketConnectionListener(
            onConnected = {},
            onFailed = { t ->
                recoveredDeferred.completeExceptionally(CancellationException("Socket dropped during live chat recovery", t))
            },
            onDisconnected = { _, _ ->
                recoveredDeferred.completeExceptionally(CancellationException("Socket closed during live chat recovery"))
            },
        )
        socketListener.addListener(socketDropListener)
        val onSuccess = socketListener.addCallback<EventLiveChatThreadRecovered>(LivechatRecovered) { _ ->
            recoveredDeferred.complete(Unit)
        }
        val onFailure = socketListener.addErrorCallback(RecoveringLivechatFailed) {
            if (configuration.hasFeature(Feature.RecoverLiveChatDoesNotFail)) {
                recoveryException = ServerCommunicationError(RecoveringLivechatFailed.value)
            }
            recoveredDeferred.complete(Unit)
        }
        try {
            threads().refresh()
            recoveredDeferred.await()
        } finally {
            socketListener.removeListener(socketDropListener)
            onSuccess.cancel()
            onFailure.cancel()
        }
        // Error reporting when recovery fails (RecoverLiveChatDoesNotFail=true) is handled
        // exclusively by ChatThreadsHandlerLive.onFailure to avoid double-reporting the same error.
        if (recoveryException == null) chatStateListener?.onReady()
    }

    companion object {
        val AVAILABILTY_TTL: Duration = 60.seconds
    }
}
