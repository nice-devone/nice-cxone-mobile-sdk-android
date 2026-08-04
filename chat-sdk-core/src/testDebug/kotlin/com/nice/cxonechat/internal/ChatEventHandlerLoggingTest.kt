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
import com.nice.cxonechat.log.Level
import com.nice.cxonechat.log.Logger
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

internal class ChatEventHandlerLoggingTest {

    private class RecordingLogger : Logger {
        val records = mutableListOf<Triple<Level, String, Throwable?>>()
        override fun log(level: Level, message: String, throwable: Throwable?) {
            records += Triple(level, message, throwable)
        }
    }

    private val logger = RecordingLogger()
    private val origin = mockk<ChatEventHandler>()
    private val handler = ChatEventHandlerLogging(origin, logger)
    private val event = mockk<ChatEvent<Any>>(relaxed = true)

    @Test
    fun `trigger delegates to origin`() = runTest {
        coEvery { origin.trigger(event) } returns Unit

        handler.trigger(event)

        coVerify(exactly = 1) { origin.trigger(event) }
    }

    @Test
    fun `trigger does not log on success`() = runTest {
        coEvery { origin.trigger(event) } returns Unit

        handler.trigger(event)

        assertTrue(logger.records.none { it.first == Level.Warning })
    }

    @Test
    fun `trigger rethrows CancellationException`() = runTest {
        coEvery { origin.trigger(event) } throws CancellationException("cancelled")

        assertFailsWith<CancellationException> { handler.trigger(event) }
    }

    @Test
    fun `trigger does not log warning on cancellation`() = runTest {
        coEvery { origin.trigger(event) } throws CancellationException("cancelled")

        runCatching { handler.trigger(event) }

        assertTrue(logger.records.none { it.first == Level.Warning })
    }

    @Test
    fun `trigger logs warning and rethrows on exception`() = runTest {
        val error = RuntimeException("dispatch failed")
        coEvery { origin.trigger(event) } throws error

        val thrown = assertFailsWith<RuntimeException> { handler.trigger(event) }

        assertEquals(error, thrown)
        assertEquals(1, logger.records.count { it.first == Level.Warning })
        assertEquals(error, logger.records.first { it.first == Level.Warning }.third)
    }
}
