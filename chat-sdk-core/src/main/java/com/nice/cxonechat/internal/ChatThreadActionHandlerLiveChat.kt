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

import com.nice.cxonechat.Cancellable
import com.nice.cxonechat.ChatThreadActionHandler
import com.nice.cxonechat.Popup
import com.nice.cxonechat.enums.EventType.LivechatRecovered
import com.nice.cxonechat.internal.model.network.EventLiveChatThreadRecovered
import com.nice.cxonechat.internal.socket.EventCallback.Companion.addCallback
import com.nice.cxonechat.thread.ChatThreadState

/**
 * Handles actions related to the [EventLiveChatThreadRecovered].
 */
internal class ChatThreadActionHandlerLiveChat(
    private val handler: ChatThreadActionHandlerImpl,
    chat: ChatWithParameters,
    chatThread: com.nice.cxonechat.internal.model.ChatThreadMutable,
) : ChatThreadActionHandler by handler {
    private val inactivityCancellable = Cancellable(
        chat.socketListener.addCallback(LivechatRecovered) { event: EventLiveChatThreadRecovered ->
            if (event.inThread(chatThread) &&
                event.thread?.threadState != ChatThreadState.Closed &&
                event.thread?.canAddMoreMessages == true
            ) {
                val popup = event.popup
                if (popup is Popup.InactivityPopup) {
                    handler.handlePopup(popup)
                }
            }
        }
    )

    override fun close() {
        inactivityCancellable.cancel()
        handler.close()
    }
}
