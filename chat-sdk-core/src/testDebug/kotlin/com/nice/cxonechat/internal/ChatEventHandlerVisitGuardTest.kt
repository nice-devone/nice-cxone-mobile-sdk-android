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
import com.nice.cxonechat.event.PageViewEvent
import com.nice.cxonechat.event.VisitEvent
import com.nice.cxonechat.storage.ValueStorage
import com.nice.cxonechat.storage.ValueStorage.VisitDetails
import io.mockk.MockKAnnotations
import io.mockk.Ordering.ORDERED
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.util.UUID
import java.util.concurrent.atomic.AtomicReference
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

class ChatEventHandlerVisitGuardTest {
    @MockK
    private lateinit var origin: ChatEventHandler

    @MockK
    private lateinit var storage: ValueStorage

    @MockK
    private lateinit var chat: ChatWithParameters

    @InjectMockKs
    private lateinit var guard: ChatEventHandlerVisitGuard

    private val now = Clock.System.now()

    private val Instant.expires: Instant
        get() = this + 30.minutes

    private val title = "title"
    private val url = "/some/page"

    // Shared real Mutex — injected into Guards so all guards created from the same chat share it.
    private val realVisitMutex = Mutex()

    @Before
    fun prepare() {
        MockKAnnotations.init(this)

        every { chat.events() } returns guard
        every { chat.guards } returns Threading.Guards(visitMutex = realVisitMutex)
        every { chat.entrails } returns mockk { every { logger } returns mockk(relaxed = true) }
        coEvery { origin.trigger(any()) } returns Unit
        every { chat.storage } returns storage
    }

    @Test
    fun `non-PageViewEvent bypasses visit validation and passes through to origin`() = runTest {
        val event = VisitEvent(now)

        guard.trigger(event)

        coVerify(exactly = 1) { origin.trigger(event) }
        coVerify(exactly = 0) { storage.visitDetails = any() }
    }

    @Test
    fun `no visit generates new visit`() = runTest {
        val event = PageViewEvent(title, url, now)

        // return no visit in place yet
        every { storage.visitDetails } returns null

        every { storage.visitDetails = any() } returns Unit

        guard.trigger(event)

        coVerify(ordering = ORDERED) {
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
            origin.trigger(event)
        }
    }

    @Test
    fun `stale visit id generates new visit`() = runTest {
        val event = PageViewEvent(title, url, now)
        val visitID = UUID.randomUUID()

        // return a stale visit details, it expired 1 millisecond ago
        every { storage.visitDetails } returns VisitDetails(visitID, now - 1.milliseconds)

        every { storage.visitDetails = any() } returns Unit

        guard.trigger(event)

        coVerify(ordering = ORDERED) {
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
            origin.trigger(event)
        }
    }

    @Test
    fun `concurrent page view events with stale visit create only one new visit`() = runTest {
        val event = PageViewEvent(title, url, now)
        // Track stored visit so the mock getter reflects writes — without this all 5 callers
        // would always see null and each create a separate new visit.
        val currentDetails = AtomicReference<VisitDetails?>(null)
        every { storage.visitDetails } answers { currentDetails.get() }
        every { storage.visitDetails = any() } answers { currentDetails.set(firstArg()) }
        // Sleep inside the VisitEvent trigger (while the mutex is held) so all 5 coroutines
        // pile up at the mutex before the first one releases it.
        coEvery { origin.trigger(match { it is VisitEvent }) } answers { Thread.sleep(50) }

        val jobs = (1..5).map {
            launch(Dispatchers.IO) { guard.trigger(event) }
        }
        jobs.joinAll()

        // With mutex serialization, only one VisitEvent (new visit creation) fires;
        // subsequent callers read the already-valid visit and only extend it.
        coVerify(exactly = 1) {
            origin.trigger(match { it is VisitEvent })
        }
    }

    @Test
    fun `fresh visit updates visit id`() = runTest {
        val event = PageViewEvent(title, url, now)
        val visitID = UUID.randomUUID()

        every { storage.visitDetails } returns VisitDetails(visitID, now + 1.milliseconds)

        every { storage.visitDetails = any() } returns Unit

        guard.trigger(event)

        coVerify(ordering = ORDERED) {
            // visit details should be updated with original visit and updated time
            storage.visitDetails = VisitDetails(visitID, now.expires)
            // and the original event should be passed on to the origin
            origin.trigger(event)
        }
    }

    @Test
    fun `concurrent page view events via separate guard instances create only one new visit`() = runTest {
        // Simulate two separate chat.events() callers — each gets its own guard but SAME chat.
        // Guards share chat.guards.visitMutex (per-Chat, not per-guard), so only one VisitEvent
        // fires regardless of how many separate guard instances are created.
        val event = PageViewEvent(title, url, now)
        val currentDetails = AtomicReference<VisitDetails?>(null)
        every { storage.visitDetails } answers { currentDetails.get() }
        every { storage.visitDetails = any() } answers { currentDetails.set(firstArg()) }
        coEvery { origin.trigger(match { it is VisitEvent }) } answers { Thread.sleep(50) }

        val jobs = (1..5).map {
            val separateGuard = ChatEventHandlerVisitGuard(origin, chat)
            launch(Dispatchers.IO) { separateGuard.trigger(event) }
        }
        jobs.joinAll()

        coVerify(exactly = 1) {
            origin.trigger(match { it is VisitEvent })
        }
    }

    @Test
    fun `CancellationException from VisitEvent trigger is re-thrown without rolling back visitDetails`() = runTest {
        val event = PageViewEvent(title, url, now)
        // Stale visit so the guard creates a new visit and triggers VisitEvent.
        val priorDetails = VisitDetails(UUID.randomUUID(), now - 1.milliseconds)
        val currentDetails = AtomicReference<VisitDetails?>(priorDetails)
        every { storage.visitDetails } answers { currentDetails.get() }
        every { storage.visitDetails = any() } answers { currentDetails.set(firstArg()) }
        coEvery { origin.trigger(match { it is VisitEvent }) } throws CancellationException("scope cancelled")

        assertFailsWith<CancellationException> {
            guard.trigger(event)
        }

        // visitDetails must NOT be rolled back on cancellation — rolling it back would cause the
        // next page-view to re-create a duplicate visit rather than treating this one as valid.
        val storedDetails = assertNotNull(currentDetails.get())
        assertEquals(now.expires, storedDetails.validUntil)
    }

    @Test
    fun `VisitEvent trigger failure rolls back visitDetails to previous state and rethrows`() = runTest {
        val event = PageViewEvent(title, url, now)
        val priorDetails = VisitDetails(UUID.randomUUID(), now - 1.milliseconds)
        val currentDetails = AtomicReference<VisitDetails?>(priorDetails)
        every { storage.visitDetails } answers { currentDetails.get() }
        every { storage.visitDetails = any() } answers { currentDetails.set(firstArg()) }
        coEvery { origin.trigger(match { it is VisitEvent }) } throws RuntimeException("trigger failed")

        var threw = false
        try {
            guard.trigger(event)
        } catch (_: RuntimeException) {
            threw = true
        }

        assertTrue(threw)
        assertEquals(priorDetails, currentDetails.get())
    }
}
