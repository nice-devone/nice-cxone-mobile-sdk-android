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

import com.nice.cxonechat.Chat
import com.nice.cxonechat.ChatStateEvent
import com.nice.cxonechat.ChatStateListener
import com.nice.cxonechat.TokenDelegateListener
import com.nice.cxonechat.event.PageViewEvent
import com.nice.cxonechat.internal.model.ConfigurationInternal
import com.nice.cxonechat.internal.socket.ProxyWebSocketListener
import com.nice.cxonechat.state.Connection
import com.nice.cxonechat.thread.CustomField
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import okhttp3.WebSocket

@Suppress(
    "ComplexInterface"
)
internal interface ChatWithParameters : Chat {

    val entrails: ChatEntrails
    override val configuration: ConfigurationInternal
    val socket: WebSocket?
    val socketFlow: StateFlow<WebSocket?>
    val socketListener: ProxyWebSocketListener
    var connection: Connection
    override var fields: List<CustomField>

    /** Last page view event received, if any. */
    var lastPageViewed: PageViewEvent?

    val chatStateListener: ChatStateListener?

    val tokenDelegateListener: TokenDelegateListener?

    /** True when this instance was built for the implicit OAuth flow (tokenDelegateListener is set and authorization is None). */
    val isImplicitOAuthFlow: Boolean

    /** Shared coordinator for token refresh — one instance per chat session. */
    val tokenRefreshCoordinator: TokenRefreshCoordinator

    override var isChatAvailable: Boolean

    val storage get() = entrails.storage
    val service get() = entrails.service
    val cookieJar get() = entrails.cookieJar
    val guards get() = entrails.threading.guards

    var eventHandlerProvider: ChatEventHandlerProvider
}

/**
 * Returns the current [WebSocket] immediately if available, or suspends until one becomes
 * available. Performs a best-effort fail-fast check: if the last known state is not
 * [ChatStateEvent.Connecting], [ChatStateEvent.Connected], or [ChatStateEvent.Ready],
 * an [IllegalStateException] is thrown instead of suspending.
 *
 * While suspended it also observes [stateFlow]: if reconnection exhausts and the chat emits
 * [ChatStateEvent.UnexpectedDisconnect], no further socket will arrive, so this throws an
 * [IllegalStateException] instead of hanging forever. Reconnect attempts keep the state at
 * [ChatStateEvent.Connecting], so a send issued during a transient drop still waits for the
 * reconnected socket.
 */
internal suspend fun ChatWithParameters.awaitSocket(): WebSocket {
    socket?.let { return it }
    val lastState = stateFlow.replayCache.firstOrNull()
    check(
        lastState is ChatStateEvent.Connecting ||
                lastState is ChatStateEvent.Connected ||
                lastState is ChatStateEvent.Ready
    ) { "Cannot dispatch event: chat is not in Connecting, Connected, or Ready state (state: $lastState)" }
    val (awaited, state) = combine(socketFlow, stateFlow) { awaited, state -> awaited to state }
        .first { (awaited, state) -> awaited != null || state is ChatStateEvent.UnexpectedDisconnect }
    return awaited ?: error(
        "Cannot dispatch event: reconnection exhausted while awaiting a socket (state: $state)"
    )
}
