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

import com.nice.cxonechat.ChatEventHandler
import com.nice.cxonechat.ChatEventHandler.OnEventErrorListener
import com.nice.cxonechat.ChatEventHandler.OnEventSentListener
import com.nice.cxonechat.event.AnalyticsEvent
import com.nice.cxonechat.event.ChatEvent
import com.nice.cxonechat.event.LocalEvent
import com.nice.cxonechat.exceptions.AnalyticsEventDispatchException
import com.nice.cxonechat.exceptions.CXOneException
import com.nice.cxonechat.exceptions.InternalError
import com.nice.cxonechat.internal.socket.send
import kotlinx.serialization.SerializationException
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.ParseException

internal class ChatEventHandlerImpl(
    private val chat: ChatWithParameters,
) : ChatEventHandler {

    override fun trigger(event: ChatEvent<*>, listener: OnEventSentListener?, errorListener: OnEventErrorListener?) {
        // Is this an internal event that doesn't get broadcast any further?
        if (event is LocalEvent) return

        val model = runCatching {
            event.getModel(chat.connection, chat.storage)
        }.onFailure { throwable ->
            when (throwable) {
                is CXOneException -> errorListener?.onError(throwable)
                is ParseException, is SerializationException -> errorListener?.onError(InternalError("Serialization error", throwable))
            }
        }.getOrNull() ?: return
        when (model) {
            is LocalEvent -> Unit
            is AnalyticsEvent -> postAnalyticsEvent(model, listener, errorListener)
            else -> postWSSEvent(model, listener)
        }
    }

    private fun postWSSEvent(model: Any, listener: OnEventSentListener?) {
        chat.socket?.send(model, listener?.run { ::onSent })
    }

    private fun postAnalyticsEvent(
        event: AnalyticsEvent,
        listener: OnEventSentListener?,
        errorListener: OnEventErrorListener?,
    ) {
        chat.service.postEvent(
            chat.connection.brandId.toString(),
            chat.storage.visitorId.toString(),
            event
        ).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                listener?.onSent()
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                listener?.onSent()
                errorListener?.onError(AnalyticsEventDispatchException(t.message ?: "Failed to dispatch event.", t))
            }
        })
    }
}
