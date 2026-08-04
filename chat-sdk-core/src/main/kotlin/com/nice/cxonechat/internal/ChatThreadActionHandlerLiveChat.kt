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

import com.nice.cxonechat.ChatThreadActionHandler
import com.nice.cxonechat.Popup
import com.nice.cxonechat.internal.model.network.EventLiveChatThreadRecovered
import com.nice.cxonechat.internal.socket.EventCallback.Companion.eventFlow
import com.nice.cxonechat.thread.ChatThreadState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.merge

/**
 * Adds inactivity popups carried by [EventLiveChatThreadRecovered] (e.g. after reconnect) on top of
 * the live MessageCreated popups exposed by [handler].
 */
internal class ChatThreadActionHandlerLiveChat(
    private val handler: ChatThreadActionHandlerImpl,
    chat: ChatWithParameters,
    chatThread: com.nice.cxonechat.internal.model.ChatThreadMutable,
) : ChatThreadActionHandler by handler {

    /**
     * Cold merge of the two popup sources — live MessageCreated popups ([handler]'s popupFlow) and
     * popups embedded in LiveChatThreadRecovered events. Both are cold, so the merged flow
     * re-subscribes on every collection and survives a close()/reconnect cycle (DE-172157).
     * The recovery source is suppressed once the thread is closed / can no longer accept messages.
     */
    override val popupFlow: Flow<Popup> = merge(
        handler.popupFlow,
        chat.socketListener.eventFlow(EventLiveChatThreadRecovered)
            .mapNotNull { (_, event) ->
                if (event.inThread(chatThread) &&
                    event.thread?.threadState != ChatThreadState.Closed &&
                    event.thread?.canAddMoreMessages == true
                ) {
                    event.popup as? Popup.InactivityPopup
                } else {
                    null
                }
            }
    )
}
