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
import com.nice.cxonechat.event.ChatEvent
import com.nice.cxonechat.internal.model.network.EventMessageCreated
import com.nice.cxonechat.internal.socket.EventCallback.Companion.eventFlow
import com.nice.cxonechat.log.warning
import com.nice.cxonechat.thread.ChatThread
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.serialization.SerializationException
import java.util.UUID

internal class ChatThreadActionHandlerImpl(
    private val chat: ChatWithParameters,
    private val chatThread: ChatThread,
) : ChatThreadActionHandler {

    /**
     * Cold flow of popups (inactivity popups, Satisfaction Survey) carried by MessageCreated events
     * for this thread. Deliberately cold so every collection re-subscribes to the socket, surviving
     * a close()/reconnect cycle. [Popup.InactivityPopup] is restricted to [ChatMode.LiveChat];
     * [Popup.SatisfactionSurvey] is delivered for every mode.
     */
    override val popupFlow: Flow<Popup> = chat.socketListener.eventFlow(EventMessageCreated)
        .mapNotNull { (_, eventMessageCreated) ->
            if (!eventMessageCreated.inThread(chatThread)) {
                null
            } else {
                when (val popup = chat.popupOrLogAndDrop { eventMessageCreated.popup }) {
                    is Popup.InactivityPopup -> popup.takeIf { chat.chatMode === ChatMode.LiveChat }
                    is Popup.SatisfactionSurvey -> chat.surveyIfUndelivered(chatThread.id, popup)
                    else -> null
                }
            }
        }
}

/**
 * A Satisfaction Survey is deduplicated per thread, recorded here the moment it's emitted, for the
 * last [storage]-bounded number of threads. Returns `null` for a thread already delivered for.
 */
internal fun ChatWithParameters.surveyIfUndelivered(
    threadId: UUID,
    popup: Popup.SatisfactionSurvey,
): Popup.SatisfactionSurvey? =
    popup.takeIf { storage.markSurveyDeliveredIfAbsent(threadId) }

/**
 * Evaluates [block], dropping and logging a malformed popup instead of letting the
 * [SerializationException] escape and cancel the whole popupFlow collector.
 */
internal inline fun ChatWithParameters.popupOrLogAndDrop(block: () -> Popup?): Popup? =
    try {
        block()
    } catch (loggedException: SerializationException) {
        entrails.logger.warning("Dropping malformed popup message", loggedException)
        null
    }

/**
 * Sends [event], logging and swallowing a send failure instead of letting it escape and cancel
 * the merged popupFlow (which would also take down the sibling live popup source).
 */
internal suspend fun ChatWithParameters.triggerRecoveryOrLog(event: ChatEvent<*>) {
    try {
        events().trigger(event)
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (loggedException: Exception) {
        entrails.logger.warning("Failed to trigger recovery event", loggedException)
    }
}
