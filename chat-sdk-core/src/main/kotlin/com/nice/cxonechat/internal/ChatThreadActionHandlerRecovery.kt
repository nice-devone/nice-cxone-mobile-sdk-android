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
import com.nice.cxonechat.event.RecoverThreadEvent
import com.nice.cxonechat.internal.model.ChatThreadMutable
import com.nice.cxonechat.internal.model.network.EventThreadRecovered
import com.nice.cxonechat.internal.socket.EventCallback.Companion.eventFlow
import com.nice.cxonechat.thread.ChatThreadState.Pending
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.merge

/**
 * Adds a Satisfaction Survey missed while the app was closed, carried by [EventThreadRecovered],
 * on top of the live MessageCreated popups exposed by [handler]. Unlike
 * [ChatThreadActionHandlerLiveChat], this only ever recovers a [Popup.SatisfactionSurvey].
 */
internal class ChatThreadActionHandlerRecovery(
    private val handler: ChatThreadActionHandler,
    chat: ChatWithParameters,
    chatThread: ChatThreadMutable,
) : ChatThreadActionHandler by handler {

    /**
     * Cold merge of live MessageCreated popups and a survey embedded in a ThreadRecovered event.
     * Not gated on thread/closed state - a survey arrives when the conversation ends, so by
     * recovery time the thread is normally already archived/closed.
     */
    override val popupFlow: Flow<Popup> = merge(
        handler.popupFlow,
        chat.socketListener.eventFlow(EventThreadRecovered) {
            // A Pending thread does not exist on the server yet; recovering it fails and surfaces
            // as an error banner, matching the guard ChatThreadHandlerImpl.refresh() already uses.
            if (chatThread.threadState !== Pending) {
                chat.triggerRecoveryOrLog(RecoverThreadEvent(chatThread.id))
            }
        }
            .mapNotNull { (_, event) ->
                if (!event.inThread(chatThread)) {
                    null
                } else {
                    (chat.popupOrLogAndDrop { event.popup } as? Popup.SatisfactionSurvey)?.let { popup ->
                        chat.surveyIfUndelivered(chatThread.id, popup)
                    }
                }
            }
    )
}
