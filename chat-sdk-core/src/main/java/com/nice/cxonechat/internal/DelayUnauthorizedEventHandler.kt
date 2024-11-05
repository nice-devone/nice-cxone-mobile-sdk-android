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
import com.nice.cxonechat.event.AuthorizeCustomerEvent
import com.nice.cxonechat.event.ChatEvent
import com.nice.cxonechat.event.LocalEvent
import com.nice.cxonechat.event.ReconnectCustomerEvent
import com.nice.cxonechat.event.RefreshToken
import com.nice.cxonechat.log.Logger
import com.nice.cxonechat.log.LoggerScope
import com.nice.cxonechat.log.scope
import com.nice.cxonechat.log.verbose
import com.nice.cxonechat.util.expiresWithin
import java.util.UUID
import kotlin.time.Duration.Companion.seconds

/**
 * Class which delays events which require authorization process to complete before they can be triggered.
 * It essentially serves as a buffer with a triggered flush mechanism [triggerDelayedEvents] which should be invoked once
 * the authorization process is complete.
 */
internal class DelayUnauthorizedEventHandler(
    private val events: ChatEventHandler,
    private val chat: ChatWithParameters,
    logger: Logger = chat.entrails.logger,
) : ChatEventHandler, LoggerScope by LoggerScope<DelayUnauthorizedEventHandler>(logger) {
    private val delayedEvents = LinkedHashMap<UUID, () -> Unit>()
    private var disableDelay = false

    override fun trigger(event: ChatEvent<*>, listener: OnEventSentListener?, errorListener: OnEventErrorListener?) = scope("trigger") {
        if (eventCanSkipAuthorization(event)) {
            events.trigger(event, listener, errorListener)
            return@scope
        }
        if (delayEvent()) {
            verbose(
                "Delaying trigger of an event $event, pending authorization," +
                        " ${chat.storage.authToken} ${chat.storage.authTokenExpDate}"
            )
            delayedEvents[UUID.randomUUID()] = { events.trigger(event, listener, errorListener) }
        } else {
            events.trigger(event, listener, errorListener)
        }
    }

    private fun delayEvent(): Boolean {
        val authTokenExpDate = chat.storage.authTokenExpDate
        return authTokenExpDate == null || chat.storage.authToken == null || authTokenExpDate.expiresWithin(1.seconds)
    }

    fun triggerDelayedEvents(disableFutureDelays: Boolean) = scope("triggerDelayedEvents") {
        disableDelay = disableFutureDelays
        if (delayedEvents.isEmpty()) return@scope
        val toTrigger = delayedEvents.toMap()
        delayedEvents.keys.removeAll(toTrigger.keys)
        verbose("Triggering all delayed events")
        toTrigger.entries.forEach {
            it.value()
        }
    }

    /**
     * Check if the event can be triggered without waiting for authorization by the backend.
     * Authorization events [AuthorizeCustomerEvent], [ReconnectCustomerEvent] and [RefreshToken] are always allowed by this filter.
     * [LocalEvent] are not sent to the backend and therefore are also allowed.
     * And events with model [AnalyticsEvent] are also allowed since they are sent via a different route.
     *
     * @return true iff the event can be triggered without waiting for authorization by the backend
     */
    private fun eventCanSkipAuthorization(event: ChatEvent<*>): Boolean = when (event) {
        is AuthorizeCustomerEvent -> true
        is ReconnectCustomerEvent -> true
        is RefreshToken -> true
        is LocalEvent -> true
        else -> when (event.getModel(chat.connection, chat.storage)) {
            is AnalyticsEvent -> true
            else -> disableDelay
        }
    }
}
