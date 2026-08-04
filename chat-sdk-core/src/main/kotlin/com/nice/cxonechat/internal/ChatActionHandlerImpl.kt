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

import com.nice.cxonechat.ChatActionHandler
import com.nice.cxonechat.PopupEvent
import com.nice.cxonechat.analytics.ActionMetadata
import com.nice.cxonechat.enums.ActionType.CustomPopupBox
import com.nice.cxonechat.internal.model.network.EventProactiveAction
import com.nice.cxonechat.internal.socket.EventCallback.Companion.addCallback
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

internal class ChatActionHandlerImpl(
    chat: ChatWithParameters,
) : ChatActionHandler {

    // A proactive popup is a one-shot event: deliver it exactly once, buffering the latest one until
    // a collector is present. A CONFLATED Channel gives that consume-once semantic (matching the
    // pre-coroutine listener model) — unlike a replay=1 SharedFlow, an already-delivered popup is not
    // re-emitted to a re-subscribing collector, so the popup is never shown twice on screen re-entry.
    private val popupChannel = Channel<PopupEvent>(capacity = Channel.CONFLATED)

    private val popupCancellable = chat.socketListener
        .addCallback(EventProactiveAction) { model ->
            if (model.type != CustomPopupBox) return@addCallback
            popupChannel.trySend(PopupEventImpl(model.variables, model.metadata))
        }

    override val popupFlow: Flow<PopupEvent> = popupChannel.receiveAsFlow()

    override fun close() {
        popupCancellable.cancel()
        // Drop any popup buffered but not yet consumed so it cannot surface after close().
        do {
            val received = popupChannel.tryReceive()
        } while (received.isSuccess)
    }

    private data class PopupEventImpl(
        override val variables: Map<String, Any?>,
        override val metadata: ActionMetadata,
    ) : PopupEvent
}
