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

import com.nice.cxonechat.ChatMode
import com.nice.cxonechat.ChatThreadActionHandler
import com.nice.cxonechat.Popup
import com.nice.cxonechat.internal.model.network.EventMessageCreated
import com.nice.cxonechat.internal.socket.EventCallback.Companion.eventFlow
import com.nice.cxonechat.thread.ChatThread
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.mapNotNull

internal class ChatThreadActionHandlerImpl(
    private val chat: ChatWithParameters,
    private val chatThread: ChatThread,
) : ChatThreadActionHandler {

    /**
     * Cold flow of inactivity popups carried by MessageCreated events for this thread.
     *
     * Deliberately cold — not a hot [kotlinx.coroutines.flow.MutableSharedFlow] fed by a
     * constructor-launched job — so that every collection re-establishes the underlying socket
     * subscription. This mirrors [ChatThreadHandlerImpl.threadFlow] and is what makes popup
     * delivery survive a [com.nice.cxonechat.Chat.close]/reconnect cycle: the previous design
     * launched the listener once in the constructor on the chat-session coroutine scope, which
     * [com.nice.cxonechat.internal.ChatImpl.close] cancels — leaving the cached/memoized handler
     * with a dead producer after reconnect (DE-172157).
     *
     * Only LiveChat threads receive inactivity popups; other modes expose an empty flow.
     */
    override val popupFlow: Flow<Popup> = if (chat.chatMode === ChatMode.LiveChat) {
        chat.socketListener.eventFlow(EventMessageCreated)
            .mapNotNull { (_, eventMessageCreated) ->
                if (eventMessageCreated.inThread(chatThread)) {
                    eventMessageCreated.popup as? Popup.InactivityPopup
                } else {
                    null
                }
            }
    } else {
        emptyFlow()
    }
}
