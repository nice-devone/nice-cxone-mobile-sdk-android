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

import com.nice.cxonechat.ChatActionHandler
import com.nice.cxonechat.ChatActionHandler.OnPopupActionListener
import com.nice.cxonechat.analytics.ActionMetadata
import com.nice.cxonechat.enums.ActionType.CustomPopupBox
import com.nice.cxonechat.internal.model.network.EventProactiveAction
import com.nice.cxonechat.internal.socket.EventCallback.Companion.addCallback

internal class ChatActionHandlerImpl(
    chat: ChatWithParameters,
) : ChatActionHandler {

    private var latestParams: ParamsWithMetadata? = null
    private var popupListener: OnPopupActionListener? = null
    private val popupCancellable = chat.socketListener
        .addCallback(EventProactiveAction) { model ->
            val listener = popupListener
            val metadata = model.metadata
            if (model.type != CustomPopupBox) return@addCallback
            val variables = model.variables
            if (listener == null) {
                latestParams = ParamsWithMetadata(variables.orEmpty(), metadata)
                return@addCallback
            }
            if (variables != null) listener.onShowPopup(variables, metadata)
            latestParams = null
        }

    override fun onPopup(listener: OnPopupActionListener) {
        this.popupListener = listener
        val (params, metadata) = latestParams ?: return
        latestParams = null
        listener.onShowPopup(params, metadata)
    }

    override fun close() {
        popupListener = null
        popupCancellable.cancel()
    }

    private data class ParamsWithMetadata(
        val params: Map<String, Any?>,
        val metadata: ActionMetadata,
    )
}
