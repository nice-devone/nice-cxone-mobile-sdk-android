/*
 * Copyright (c) 2026. NICE Ltd. All rights reserved.
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

import com.nice.cxonechat.internal.CaseStatusChangedHandlerActions.staysClosedFor
import com.nice.cxonechat.internal.copy.ChatThreadCopyable.Companion.asCopyable
import com.nice.cxonechat.internal.model.ChatThreadMutable
import com.nice.cxonechat.internal.model.ChatThreadMutable.Companion.asMutable
import com.nice.cxonechat.internal.model.network.EventCaseStatusChanged
import com.nice.cxonechat.internal.model.network.EventCaseStatusChanged.CaseStatus
import com.nice.cxonechat.internal.model.network.EventCaseStatusChanged.CaseStatus.Closed
import com.nice.cxonechat.internal.model.network.EventCaseStatusChanged.CaseStatus.Open
import com.nice.cxonechat.model.makeChatThread
import com.nice.cxonechat.thread.ChatThreadState
import com.nice.cxonechat.thread.ContactHandler.CXone
import com.nice.cxonechat.thread.ContactHandler.Cognigy
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Instant

internal class CaseStatusChangedHandlerActionsTest {

    private val thread = makeChatThread(contactHandler = CXone).asMutable()

    @Test
    fun `closing the case also takes the new handler`() {
        var archived = false

        CaseStatusChangedHandlerActions.handleCaseClosed(thread, event(thread, Closed, "cognigy")) { archived = true }

        assertEquals(true, archived)
        assertEquals(Cognigy, thread.contactHandler)
    }

    @Test
    fun `a handler-only change is not reported as an archive`() {
        var archived = false

        CaseStatusChangedHandlerActions.handleCaseClosed(thread, event(thread, Open, "cognigy")) { archived = true }

        assertFalse(archived)
        assertEquals(CXone, thread.contactHandler)
    }

    @Test
    fun `closing the case without a handler keeps the previous one`() {
        thread.update(thread.asCopyable().copy(contactHandler = Cognigy))

        CaseStatusChangedHandlerActions.handleCaseClosed(thread, event(thread, Closed, null)) { }

        assertEquals(Cognigy, thread.contactHandler)
    }

    @Test
    fun `closing the case of an archived thread still closes it`() {
        // archive() disables sending but leaves the thread state unchanged.
        thread.update(thread.asCopyable().copy(canAddMoreMessages = false, threadState = ChatThreadState.Ready))
        var updated = false

        CaseStatusChangedHandlerActions.handleCaseClosed(thread, event(thread, Closed, null)) { updated = true }

        assertTrue(updated)
        assertEquals(ChatThreadState.Closed, thread.threadState)
    }

    @Test
    fun `closing an already closed case is not reported again`() {
        thread.update(thread.asCopyable().copy(canAddMoreMessages = false, threadState = ChatThreadState.Closed))
        var updated = false

        CaseStatusChangedHandlerActions.handleCaseClosed(thread, event(thread, Closed, null)) { updated = true }

        assertFalse(updated)
    }

    @Test
    fun `a closed thread stays closed for its own or an unknown case, not for a new one`() {
        thread.update(thread.asCopyable().copy(threadState = ChatThreadState.Closed, contactId = "case1"))

        assertTrue(thread.staysClosedFor("case1"))
        assertTrue(thread.staysClosedFor(null))
        assertFalse(thread.staysClosedFor("case2"))
        thread.update(thread.asCopyable().copy(contactId = null))
        assertTrue(thread.staysClosedFor("case2"))
        thread.update(thread.asCopyable().copy(threadState = ChatThreadState.Ready, contactId = "case1"))
        assertFalse(thread.staysClosedFor("case1"))
    }

    private fun event(thread: ChatThreadMutable, status: CaseStatus, handler: String?) = EventCaseStatusChanged(
        createdAt = Instant.fromEpochMilliseconds(0),
        data = EventCaseStatusChanged.Data(
            EventCaseStatusChanged.Case(
                threadIdOnExternalPlatform = thread.id.toString(),
                status = status,
                statusUpdatedAt = Instant.fromEpochMilliseconds(0),
                handler = handler,
            )
        ),
    )
}
