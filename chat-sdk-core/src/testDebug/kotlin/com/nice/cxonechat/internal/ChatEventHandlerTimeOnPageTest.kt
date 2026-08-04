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
import com.nice.cxonechat.event.CustomVisitorEvent
import com.nice.cxonechat.event.PageViewEndedEvent
import com.nice.cxonechat.event.PageViewEvent
import com.nice.cxonechat.event.TimeSpentOnPageEvent
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

internal class ChatEventHandlerTimeOnPageTest {

    @MockK
    private lateinit var origin: ChatEventHandler

    @MockK
    private lateinit var chat: ChatWithParameters

    private lateinit var handler: ChatEventHandlerTimeOnPage

    private val now = Instant.fromEpochMilliseconds(1_000_000L)

    private val capturedEvents = mutableListOf<ChatEvent<*>>()

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        capturedEvents.clear()
        var storedPageView: PageViewEvent? = null
        every { chat.lastPageViewed } answers { storedPageView }
        every { chat.lastPageViewed = any() } answers { storedPageView = arg(0) }
        coEvery { origin.trigger(capture(capturedEvents)) } returns Unit
        handler = ChatEventHandlerTimeOnPage(origin, chat)
    }

    private fun assertTimeSpentEvent(title: String, uri: String, timeSpentOnPage: Long) {
        val timeSpentEvents = capturedEvents.filterIsInstance<TimeSpentOnPageEvent>()
        assertEquals(1, timeSpentEvents.size, "Expected exactly one TimeSpentOnPageEvent")
        val event = timeSpentEvents.first()
        assertContains(
            event.toString(),
            "TimeSpentOnPage(title='$title', uri='$uri', timeOnPage=$timeSpentOnPage)"
        )
    }

    @Test
    fun trigger_nonPageEvent_delegatesToOrigin() = runTest {
        val event = CustomVisitorEvent("data")
        handler.trigger(event)
        coVerify(exactly = 1) { origin.trigger(event) }
    }

    @Test
    fun trigger_pageViewEvent_delegatesToOrigin() = runTest {
        val event = PageViewEvent("title", "/page", now)
        handler.trigger(event)
        coVerify(exactly = 1) { origin.trigger(event) }
    }

    @Test
    fun trigger_pageViewEndedEvent_sendsTimeSpent_whenPageWasViewed() = runTest {
        val viewEvent = PageViewEvent("title", "/page", now)
        val endDate = now + 5.seconds
        val endEvent = PageViewEndedEvent("title", "/page", endDate)

        handler.trigger(viewEvent)
        handler.trigger(endEvent)

        coVerify(exactly = 1) { origin.trigger(viewEvent) }
        assertTimeSpentEvent("title", "/page", 5L)
    }

    @Test
    fun trigger_pageViewEndedEvent_clearsLastPage() = runTest {
        val viewEvent = PageViewEvent("title", "/page", now)
        val endDate = now + 3.seconds
        val endEvent = PageViewEndedEvent("title", "/page", endDate)

        handler.trigger(viewEvent)
        handler.trigger(endEvent)

        // Second end event should not trigger another TimeSpent since lastPageViewed is null
        handler.trigger(endEvent)

        assertTimeSpentEvent("title", "/page", 3L)
    }

    @Test
    fun trigger_duplicatePageView_isIgnored() = runTest {
        val event = PageViewEvent("title", "/page", now)

        handler.trigger(event)
        handler.trigger(event) // duplicate

        coVerify(exactly = 1) { origin.trigger(event) }
    }

    @Test
    fun trigger_newPageView_autoEndsOldPage() = runTest {
        val firstPage = PageViewEvent("first", "/first", now)
        val secondPageDate = now + 10.seconds
        val secondPage = PageViewEvent("second", "/second", secondPageDate)

        handler.trigger(firstPage)
        handler.trigger(secondPage)

        coVerify(exactly = 1) { origin.trigger(firstPage) }
        assertTimeSpentEvent("first", "/first", 10L)
        coVerify(exactly = 1) { origin.trigger(secondPage) }
    }

    @Test
    fun trigger_pageViewEnded_doesNothing_whenNoPageViewed() = runTest {
        val endEvent = PageViewEndedEvent("title", "/page", now)
        handler.trigger(endEvent)

        assertTrue(capturedEvents.filterIsInstance<TimeSpentOnPageEvent>().isEmpty())
    }

    @Test
    fun trigger_pageViewEnded_doesNothing_whenDifferentPage() = runTest {
        val viewEvent = PageViewEvent("title", "/page", now)
        val endEvent = PageViewEndedEvent("other", "/other", now + 5.seconds)

        handler.trigger(viewEvent)
        handler.trigger(endEvent)

        assertTrue(capturedEvents.filterIsInstance<TimeSpentOnPageEvent>().isEmpty())
    }

    @Test
    fun trigger_pageViewEnded_minimumTimeIsOneSecond() = runTest {
        val viewEvent = PageViewEvent("title", "/page", now)
        // End event at same time as view — 0ms elapsed, should report 1s minimum
        val endEvent = PageViewEndedEvent("title", "/page", now)

        handler.trigger(viewEvent)
        handler.trigger(endEvent)

        assertTimeSpentEvent("title", "/page", 1L)
    }
}
