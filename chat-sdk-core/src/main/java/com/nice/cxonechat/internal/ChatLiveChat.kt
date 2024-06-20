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

package com.nice.cxonechat.internal

import android.annotation.SuppressLint
import com.nice.cxonechat.Cancellable
import com.nice.cxonechat.enums.ErrorType.RecoveringLivechatFailed
import com.nice.cxonechat.enums.EventType.LivechatRecovered
import com.nice.cxonechat.exceptions.RuntimeChatException.ServerCommunicationError
import com.nice.cxonechat.internal.model.network.EventLiveChatThreadRecovered
import com.nice.cxonechat.internal.socket.ErrorCallback.Companion.addErrorCallback
import com.nice.cxonechat.internal.socket.EventCallback.Companion.addCallback
import com.nice.cxonechat.internal.socket.SocketConnectionListener
import com.nice.cxonechat.state.Configuration.Feature
import com.nice.cxonechat.util.DateProvider
import com.nice.cxonechat.util.onFailure
import com.nice.cxonechat.util.onSuccess
import com.nice.cxonechat.util.plus
import java.util.Date

internal class ChatLiveChat(
    private val origin: ChatWithParameters
) : ChatWithParameters by origin {

    private var availabilityExpiration = Date(0)

    init {
        isChatAvailable = false

        origin.socketListener.addListener(
            SocketConnectionListener(
                listener = origin.chatStateListener,
                onConnected = ::recoverThread
            )
        )
    }

    private fun getAvailability(): Boolean {
        if (availabilityExpiration.before(DateProvider.now())) {
            origin
                .service
                .getChannelAvailability(
                    origin.connection.brandId.toString(),
                    origin.connection.channelId
                )
                .execute()
                .onSuccess {
                    availabilityExpiration = DateProvider.now() + AVAILABILTY_TTL
                    isChatAvailable = body()?.isOnline != false
                }
                .onFailure {
                    isChatAvailable = false
                }
        }

        return isChatAvailable
    }

    override fun getChannelAvailability(callback: (Boolean) -> Unit): Cancellable {
        callback(getAvailability())

        return Cancellable.noop
    }

    override fun connect(): Cancellable {
        // Make a new channel availability request.
        return if (!getAvailability()) {
            // If chat isn't available immediately transition to ready with no websocket.
            chatStateListener?.onReady()
            Cancellable.noop
        } else {
            // If chat is available continue with the normal process.
            val originCancellable = origin.connect()
            val onSuccess = socketListener.addCallback<EventLiveChatThreadRecovered>(LivechatRecovered) { _ ->
                chatStateListener?.onReady()
            }
            val onFailure = socketListener.addErrorCallback(RecoveringLivechatFailed) {
                if (configuration.hasFeature(Feature.RecoverLiveChatDoesNotFail)) {
                    chatStateListener?.onChatRuntimeException(ServerCommunicationError(RecoveringLivechatFailed.value))
                } else {
                    chatStateListener?.onReady()
                }
            }

            Cancellable(
                originCancellable,
                onSuccess,
                onFailure,
            )
        }
    }

    @SuppressLint("CheckResult")
    private fun recoverThread() {
        chatStateListener?.onConnected()
        threads().threads {}
    }

    companion object {
        const val AVAILABILTY_TTL = 60L * 1_000L
    }
}
