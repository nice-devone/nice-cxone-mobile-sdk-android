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

package com.nice.cxonechat.internal.model.network

import com.nice.cxonechat.enums.EventType.LivechatRecovered
import com.nice.cxonechat.internal.model.AgentModel
import com.nice.cxonechat.model.makeAgent
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Pins the priority order of the `agent` computed property on [EventLiveChatThreadRecovered]:
 *
 *   `inboxAssignee ?: previousInboxAssignee ?: ownerAssignee`
 *
 * The fallback chain was introduced to fix a bug (DE-171395) where a recovered thread whose
 * assigned agent was delivered only in `ownerAssignee` incorrectly left `threadAgent = null`,
 * causing the position-in-queue overlay to remain visible after an agent was associated.
 *
 * If the priority order changes or any path is dropped, these tests will fail immediately.
 */
internal class EventLiveChatThreadRecoveredAgentFallbackTest {

    // --- happy-path: each assignee field is the sole carrier of the agent ---

    @Test
    fun `agent returns inboxAssignee when it is the only assigned field`() {
        val inboxAgent = makeAgent(id = 1, firstName = "Inbox")
        val event = eventWith(inboxAssignee = inboxAgent)

        assertEquals(inboxAgent.toAgent(), event.agent)
    }

    @Test
    fun `agent returns previousInboxAssignee when inboxAssignee is absent`() {
        val previousAgent = makeAgent(id = 2, firstName = "Previous")
        val event = eventWith(previousInboxAssignee = previousAgent)

        assertEquals(previousAgent.toAgent(), event.agent)
    }

    @Test
    fun `agent returns ownerAssignee when inboxAssignee and previousInboxAssignee are both absent`() {
        val ownerAgent = makeAgent(id = 3, firstName = "Owner")
        val event = eventWith(ownerAssignee = ownerAgent)

        assertEquals(ownerAgent.toAgent(), event.agent)
    }

    @Test
    fun `agent returns null when all assignee fields are absent`() {
        val event = eventWith()

        assertNull(event.agent)
    }

    // --- priority: inboxAssignee wins over the other two fields ---

    @Test
    fun `inboxAssignee takes priority over previousInboxAssignee and ownerAssignee`() {
        val inbox = makeAgent(id = 10, firstName = "Inbox")
        val previous = makeAgent(id = 20, firstName = "Previous")
        val owner = makeAgent(id = 30, firstName = "Owner")
        val event = eventWith(
            inboxAssignee = inbox,
            previousInboxAssignee = previous,
            ownerAssignee = owner,
        )

        assertEquals(inbox.toAgent(), event.agent, "inboxAssignee must win when all three are set")
    }

    @Test
    fun `previousInboxAssignee takes priority over ownerAssignee`() {
        val previous = makeAgent(id = 20, firstName = "Previous")
        val owner = makeAgent(id = 30, firstName = "Owner")
        val event = eventWith(
            previousInboxAssignee = previous,
            ownerAssignee = owner,
        )

        assertEquals(
            previous.toAgent(),
            event.agent,
            "previousInboxAssignee must win over ownerAssignee when inboxAssignee is absent",
        )
    }

    // --- helpers ---

    private fun eventWith(
        inboxAssignee: AgentModel? = null,
        previousInboxAssignee: AgentModel? = null,
        ownerAssignee: AgentModel? = null,
    ) = EventLiveChatThreadRecovered(
        postback = Postback(
            eventType = LivechatRecovered,
            data = EventLiveChatThreadRecovered.Data(
                messagesScrollToken = "",
                inboxAssignee = inboxAssignee,
                previousInboxAssignee = previousInboxAssignee,
                ownerAssignee = ownerAssignee,
            ),
        ),
    )
}
