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

import com.nice.cxonechat.ChatEventHandler
import com.nice.cxonechat.event.AnalyticsEvent
import com.nice.cxonechat.event.ChatEvent
import com.nice.cxonechat.event.LocalEvent
import com.nice.cxonechat.exceptions.AnalyticsEventDispatchException
import com.nice.cxonechat.exceptions.CXoneException
import com.nice.cxonechat.exceptions.InternalError
import com.nice.cxonechat.internal.socket.send
import com.nice.cxonechat.log.Level
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.SerializationException
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.ParseException
import kotlin.coroutines.resume

internal class ChatEventHandlerImpl(
    private val chat: ChatWithParameters,
) : ChatEventHandler {

    override suspend fun trigger(event: ChatEvent<*>) {
        // Is this an internal event that doesn't get broadcast any further?
        if (event is LocalEvent) return

        val model = try {
            event.getModel(chat.connection, chat.storage)
        } catch (throwable: CXoneException) {
            throw throwable
        } catch (throwable: ParseException) {
            throw InternalError("Serialization error", throwable)
        } catch (throwable: SerializationException) {
            throw InternalError("Serialization error", throwable)
        } ?: return

        when (model) {
            is LocalEvent -> Unit
            is AnalyticsEvent -> postAnalyticsEvent(model)
            else -> postWSSEvent(model)
        }
    }

    private suspend fun postWSSEvent(model: Any) {
        chat.awaitSocket().send(model)
    }

    private suspend fun postAnalyticsEvent(event: AnalyticsEvent) {
        suspendCancellableCoroutine { continuation ->
            val call = chat.service.postEvent(
                chat.connection.brandId.toString(),
                chat.storage.visitorId.toString(),
                event
            )
            continuation.invokeOnCancellation { call.cancel() }
            call.enqueue(object : Callback<Void> {
                override fun onResponse(call: Call<Void>, response: Response<Void>) {
                    if (continuation.isActive) continuation.resume(Unit)
                }

                override fun onFailure(call: Call<Void>, t: Throwable) {
                    if (continuation.isActive) {
                        // Analytics events are fire-and-forget telemetry — a dispatch failure (e.g. no
                        // connectivity) must never crash the caller. Log it and complete normally; the
                        // failure is not surfaced to the caller or the state listener.
                        val exception = AnalyticsEventDispatchException(t.message ?: "Failed to dispatch event.", t)
                        chat.entrails.logger.log(Level.Warning, "Failed to dispatch analytics event", exception)
                        continuation.resume(Unit)
                    }
                }
            })
        }
    }
}
