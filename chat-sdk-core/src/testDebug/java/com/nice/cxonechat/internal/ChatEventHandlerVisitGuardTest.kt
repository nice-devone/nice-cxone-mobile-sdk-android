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
import com.nice.cxonechat.ChatEventHandler.OnEventSentListener
import com.nice.cxonechat.event.PageViewEvent
import com.nice.cxonechat.event.VisitEvent
import com.nice.cxonechat.storage.ValueStorage
import com.nice.cxonechat.storage.ValueStorage.VisitDetails
import com.nice.cxonechat.tool.awaitResult
import io.mockk.MockKAnnotations
import io.mockk.Ordering.ORDERED
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import java.util.Date
import java.util.UUID
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

class ChatEventHandlerVisitGuardTest {
    @MockK
    private lateinit var origin: ChatEventHandler

    @MockK
    private lateinit var storage: ValueStorage

    @MockK
    private lateinit var chat: ChatWithParameters

    @InjectMockKs
    private lateinit var guard: ChatEventHandlerVisitGuard

    private val now = Date()

    private val Date.expires: Date
        get() = this + 30.minutes

    private val title = "title"
    private val url = "/some/page"

    private operator fun Date.plus(duration: Duration) = Date(time + duration.inWholeMilliseconds)

    private operator fun Date.minus(duration: Duration) = Date(time - duration.inWholeMilliseconds)

    @Before
    fun prepare() {
        MockKAnnotations.init(this)

        every { chat.events() } returns guard
        every { origin.trigger(any(), any(), any()) } answers {
            arg<OnEventSentListener?>(1)?.onSent()
            Unit
        }
        every { chat.storage } returns storage
    }

    @Test
    fun `no visit generates new visit`() {
        val event = PageViewEvent(title, url, now)

        // return no visit in place yet
        every { storage.visitDetails } returns null

        every { storage.visitDetails = any() } returns Unit

        awaitResult(100.milliseconds) { done ->
            guard.trigger(event) { done(Unit) }
        }

        verify(ordering = ORDERED) {
            // visit details should be updated with matching visit and updated time
            storage.visitDetails = match {
                it.validUntil == now.expires
            }
            // visit event should be generated with a current time
            origin.trigger(
                match {
                    (it as? VisitEvent)?.date == now
                }
            )
            // and the page view event should be passed on to the origin
            origin.trigger(event, any(), any())
        }
    }

    @Test
    fun `stale visit id generates new visit`() {
        val event = PageViewEvent(title, url, now)
        val visitID = UUID.randomUUID()

        // return a stale visit details, it expired 1 millisecond ago
        every { storage.visitDetails } returns VisitDetails(visitID, now - 1.milliseconds)

        every { storage.visitDetails = any() } returns Unit

        awaitResult(100.milliseconds) { done ->
            guard.trigger(event) { done(Unit) }
        }

        verify(ordering = ORDERED) {
            // visit details should be updated with matching visit and updated time
            storage.visitDetails = match {
                it.validUntil == now.expires
            }
            // visit event should be generated with a current time
            origin.trigger(
                match {
                    (it as? VisitEvent)?.date == now
                }
            )
            // and the original event should be passed on to the origin
            origin.trigger(event, any(), any())
        }
    }

    @Test
    fun `fresh visit updates visit id`() {
        val event = PageViewEvent(title, url, now)
        val visitID = UUID.randomUUID()

        every { storage.visitDetails } returns VisitDetails(visitID, now + 1.milliseconds)

        every { storage.visitDetails = any() } returns Unit

        awaitResult(100.milliseconds) { done ->
            guard.trigger(event) { done(Unit) }
        }

        verify(ordering = ORDERED) {
            // visit details should be updated with original visit and updated time
            storage.visitDetails = VisitDetails(visitID, now.expires)
            // and the original event should be passed on to the origin
            origin.trigger(event, any(), any())
        }
    }
}
