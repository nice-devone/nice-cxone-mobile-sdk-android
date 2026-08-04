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
import com.nice.cxonechat.event.ChatEvent
import com.nice.cxonechat.event.PageViewEvent
import com.nice.cxonechat.event.VisitEvent
import com.nice.cxonechat.log.warning
import com.nice.cxonechat.storage.ValueStorage.VisitDetails
import com.nice.cxonechat.util.UUIDProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.withLock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

internal class ChatEventHandlerVisitGuard(
    private val origin: ChatEventHandler,
    private val chat: ChatWithParameters,
) : ChatEventHandler {

    override suspend fun trigger(event: ChatEvent<*>) {
        if (event is PageViewEvent) {
            validateVisit(event.date)
        }
        origin.trigger(event)
    }

    private suspend fun validateVisit(instant: Instant) {
        // chat.guards.visitMutex is per-Chat, not per-guard. ChatEventHandlerProvider creates a
        // fresh guard on every chat.events() call, so a per-guard mutex would give each caller its
        // own lock — allowing concurrent visit creation when two callers use separate guard instances.
        chat.guards.visitMutex.withLock {
            // Re-read under the lock: a concurrent caller may have already refreshed the visit.
            val details = chat.storage.visitDetails
            val expires = instant + 30.minutes

            // if no visit exists or the visit expired before this page-view's timestamp, we need a new visit
            if (details?.validUntil?.let { it < instant } != false) {
                val newDetails = VisitDetails(UUIDProvider.next(), expires)
                // Storage is written first so VisitEvent.getModel() reads the correct new visitId.
                // The event reads storage.visitId in getModel() during serialization; setting visitDetails
                // before triggering ensures the new UUID is already in memory when the event is serialized.
                // If the trigger fails, roll back to the previous visit state so the next
                // PageViewEvent re-attempts the new visit rather than treating this one as valid.
                chat.storage.visitDetails = newDetails
                try {
                    origin.trigger(VisitEvent(instant))
                } catch (cancellation: CancellationException) {
                    // Do not roll back on cancellation — if the parent scope is being torn down,
                    // a rollback write on storageWriteScope may itself be cancelled before it can
                    // execute, leaving disk and memory in inconsistent states. Re-throw so the
                    // caller can handle scope shutdown cleanly.
                    throw cancellation
                } catch (expected: Exception) {
                    chat.entrails.logger.warning("VisitEvent trigger failed; rolling back visitDetails", expected)
                    chat.storage.visitDetails = details
                    throw expected
                }
            } else {
                // Extend the validity window of the current still-valid visit.
                chat.storage.visitDetails = details.copy(validUntil = expires)
            }
        }
    }
}
