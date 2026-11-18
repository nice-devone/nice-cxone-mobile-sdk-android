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
import com.nice.cxonechat.event.RecoverLiveChatThreadEvent
import com.nice.cxonechat.internal.model.network.EventLiveChatThreadRecovered
import com.nice.cxonechat.internal.socket.EventCallback.Companion.eventFlow
import com.nice.cxonechat.thread.ChatThreadState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.merge

/**
 * Adds popups (inactivity popups, Satisfaction Survey) carried by [EventLiveChatThreadRecovered]
 * (e.g. after reconnect, or returning after the app was closed) on top of the live MessageCreated
 * popups exposed by [handler].
 *
 * This is also how a missed Satisfaction Survey (sent after the customer closed the app) reaches
 * the app: it was never delivered live, so it surfaces here on the next recovery instead.
 */
internal class ChatThreadActionHandlerLiveChat(
    private val handler: ChatThreadActionHandler,
    chat: ChatWithParameters,
    chatThread: com.nice.cxonechat.internal.model.ChatThreadMutable,
) : ChatThreadActionHandler by handler {

    /**
     * Cold merge of live MessageCreated popups and LiveChatThreadRecovered popups. The
     * closed/can't-add-more-messages guard only applies to [Popup.InactivityPopup] - a Satisfaction
     * Survey is sent precisely when the conversation ends, so gating it the same way would drop it.
     */
    override val popupFlow: Flow<Popup> = merge(
        handler.popupFlow,
        chat.socketListener.eventFlow(EventLiveChatThreadRecovered) {
            // A Pending thread does not exist on the server yet; recovering it fails and surfaces
            // as an error banner, matching the guard ChatThreadHandlerLiveChat.refresh() already uses.
            if (chatThread.threadState !== ChatThreadState.Pending) {
                chat.triggerRecoveryOrLog(RecoverLiveChatThreadEvent(chatThread.id))
            }
        }
            .mapNotNull { (_, event) ->
                if (!event.inThread(chatThread)) {
                    null
                } else {
                    when (val popup = chat.popupOrLogAndDrop { event.popup }) {
                        is Popup.InactivityPopup ->
                            popup.takeIf {
                                event.thread?.threadState != ChatThreadState.Closed &&
                                        event.thread?.canAddMoreMessages == true
                            }

                        is Popup.SatisfactionSurvey -> chat.surveyIfUndelivered(chatThread.id, popup)
                        else -> null
                    }
                }
            }
    )
}
