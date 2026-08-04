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

package com.nice.cxonechat

import com.nice.cxonechat.internal.ChatThreadHandlerMessages
import com.nice.cxonechat.message.Message
import com.nice.cxonechat.message.MessageMetadata
import com.nice.cxonechat.message.MessageStatus
import com.nice.cxonechat.model.makeMessage
import com.nice.cxonechat.model.makeMessageModel
import com.nice.cxonechat.model.makeUserStatistics
import io.mockk.every
import io.mockk.mockk
import org.junit.Test
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Instant

internal class ChatThreadHandlerMessagesResolveTest {

    @Test
    fun resolveMessageToAdd_returns_null_for_null_input() {
        val result = ChatThreadHandlerMessages.resolveMessageToAdd(null, emptyList())
        assertNull(result)
    }

    @Test
    fun resolveMessageToAdd_returns_message_when_not_in_existing_list() {
        val msg = makeMessage()
        val result = ChatThreadHandlerMessages.resolveMessageToAdd(msg, emptyList())
        assertEquals(msg, result)
    }

    @Test
    fun resolveMessageToAdd_keeps_read_status_over_incoming_delivered() {
        val id = UUID.randomUUID()
        val readMessage = makeMessage(makeMessageModel(idOnExternalPlatform = id, userStatistics = makeUserStatistics(readAt = Instant.fromEpochMilliseconds(0))))
        val deliveredMessage = makeMessage(makeMessageModel(idOnExternalPlatform = id))
        val result = ChatThreadHandlerMessages.resolveMessageToAdd(deliveredMessage, listOf(readMessage))
        assertEquals(MessageStatus.Read, result?.metadata?.status)
    }

    @Test
    fun resolveMessageToAdd_allows_advance_from_failed_to_deliver() {
        val id = UUID.randomUUID()
        // FailedToDeliver is a client-side status; create it via mock since it cannot be
        // produced from server MessageModel payloads (MessageMetadataInternal only yields
        // Delivered/Seen/Read).
        val failedMessage = mockk<Message> {
            every { this@mockk.id } returns id
            every { metadata } returns mockk<MessageMetadata> {
                every { status } returns MessageStatus.FailedToDeliver
            }
        }
        val deliveredMessage = makeMessage(makeMessageModel(idOnExternalPlatform = id))
        val result = ChatThreadHandlerMessages.resolveMessageToAdd(deliveredMessage, listOf(failedMessage))
        assertEquals(MessageStatus.Delivered, result?.metadata?.status)
    }

    @Test
    fun resolveMessageToAdd_returns_incoming_when_incoming_has_higher_status() {
        val id = UUID.randomUUID()
        // Existing is Delivered; incoming is Read (higher rank) — incoming should win.
        val deliveredMessage = makeMessage(makeMessageModel(idOnExternalPlatform = id))
        val readMessage = makeMessage(
            makeMessageModel(
                idOnExternalPlatform = id,
                userStatistics = makeUserStatistics(readAt = Instant.fromEpochMilliseconds(0))
            )
        )
        val result = ChatThreadHandlerMessages.resolveMessageToAdd(readMessage, listOf(deliveredMessage))
        assertEquals(MessageStatus.Read, result?.metadata?.status)
    }

    @Test
    fun resolveMessageToAdd_returns_incoming_when_statuses_are_equal() {
        val id = UUID.randomUUID()
        val existing = makeMessage(makeMessageModel(idOnExternalPlatform = id))
        val incoming = makeMessage(makeMessageModel(idOnExternalPlatform = id))
        // Both are Delivered; incoming should be returned (no preference for existing when equal).
        assertEquals(MessageStatus.Delivered, existing.metadata.status)
        assertEquals(MessageStatus.Delivered, incoming.metadata.status)
        val result = ChatThreadHandlerMessages.resolveMessageToAdd(incoming, listOf(existing))
        assertEquals(incoming, result)
    }

    @Test
    fun resolveMessageToAdd_keeps_read_over_incoming_failed_to_deliver() {
        val id = UUID.randomUUID()
        val readMessage = makeMessage(
            makeMessageModel(
                idOnExternalPlatform = id,
                userStatistics = makeUserStatistics(readAt = Instant.fromEpochMilliseconds(0))
            )
        )
        // FailedToDeliver is client-side only; must be created via mock.
        val failedMessage = mockk<Message> {
            every { this@mockk.id } returns id
            every { metadata } returns mockk<MessageMetadata> {
                every { status } returns MessageStatus.FailedToDeliver
            }
        }
        val result = ChatThreadHandlerMessages.resolveMessageToAdd(failedMessage, listOf(readMessage))
        assertEquals(MessageStatus.Read, result?.metadata?.status)
    }

    @Test
    fun resolveMessageToAdd_advances_from_delivered_to_seen() {
        val id = UUID.randomUUID()
        val deliveredMessage = makeMessage(makeMessageModel(idOnExternalPlatform = id))
        val seenMessage = makeMessage(
            makeMessageModel(
                idOnExternalPlatform = id,
                userStatistics = makeUserStatistics(seenAt = Instant.fromEpochMilliseconds(0))
            )
        )
        assertEquals(MessageStatus.Delivered, deliveredMessage.metadata.status)
        assertEquals(MessageStatus.Seen, seenMessage.metadata.status)
        val result = ChatThreadHandlerMessages.resolveMessageToAdd(seenMessage, listOf(deliveredMessage))
        assertEquals(MessageStatus.Seen, result?.metadata?.status)
    }
}
