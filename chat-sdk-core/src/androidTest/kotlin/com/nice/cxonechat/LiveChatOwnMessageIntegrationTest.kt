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

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import com.nice.cxonechat.internal.model.EnvironmentInternal
import com.nice.cxonechat.log.Level
import com.nice.cxonechat.log.LoggerAndroid
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.util.Collections
import kotlin.time.Duration.Companion.seconds

/**
 * Reproduction of DE-182612 against the real EU1 QA backend: in LiveChat mode, a custom UI that renders
 * `threadFlow` of the handler returned by `create()` must see the echo of every message the customer sends.
 *
 * Talks to a real server, so it runs only when asked to:
 * `-Pandroid.testInstrumentationRunnerArguments.liveBackend=true`.
 */
@LargeTest
internal class LiveChatOwnMessageIntegrationTest {

    @Test
    fun threadFlowDeliversEveryOwnMessageOfNewLiveChatThread() {
        val arguments = InstrumentationRegistry.getArguments()
        assumeTrue("Runs against the real backend only on request", arguments.getString("liveBackend") == "true")
        val messageCount = arguments.getString("messageCount")?.toIntOrNull() ?: DEFAULT_MESSAGE_COUNT
        val context = ApplicationProvider.getApplicationContext<Application>()
        val config = SocketFactoryConfiguration(QA_EU1, BRAND_ID, LIVE_CHAT_CHANNEL_ID)
        runBlocking {
            val builder = ChatBuilder(context, config, logger)
                .setDevelopmentMode(true)
                .setUserName("de182612", "repro")
            // A customer stored by an earlier run would recover its thread, whose 2+ messages keep the welcome
            // monitor from starting, and the test would pass without the fix. Sign out to start as a new customer.
            builder.build().signOut()
            val chat = builder.build()
            val collectorScope = CoroutineScope(Dispatchers.Default)
            try {
                verifyOwnMessagesReachThreadFlow(chat, messageCount, collectorScope)
            } finally {
                // A failed assert must not leave the collector running or the socket open.
                collectorScope.cancel()
                withContext(NonCancellable) { chat.closeSuspending() }
            }
        }
    }

    private suspend fun verifyOwnMessagesReachThreadFlow(chat: Chat, messageCount: Int, collectorScope: CoroutineScope) {
        chat.connect()
        report("chatMode=${chat.chatMode} isChatAvailable=${chat.isChatAvailable}")
        assertTrue("LiveChat channel is offline: log an agent in as available first", chat.isChatAvailable)
        val threads = chat.threads()
        val existing = withTimeout(THREADS_TIMEOUT) { threads.threadsFlow.first() }
        report("existing threads=${existing.size}")
        assertTrue("A new customer must have no threads, found ${existing.size}", existing.isEmpty())
        val handler = threads.create()
        report("created thread ${handler.get().id} state=${handler.get().threadState} messages=${handler.get().messages.size}")

        val seenInFlow = Collections.synchronizedSet(mutableSetOf<String>())
        collectorScope.launch {
            handler.threadFlow.collect { thread ->
                report("threadFlow emission: state=${thread.threadState} messages=${thread.messages.size}")
                thread.messages.forEach { seenInFlow += it.id.toString() }
            }
        }
        delay(1.seconds)

        val notEchoed = mutableListOf<Int>()
        val missing = mutableListOf<Int>()
        repeat(messageCount) { index ->
            val number = index + 1
            val (inGet, inFlow) = sendAndTrack(handler, number, seenInFlow)
            if (!inGet) notEchoed += number
            if (inGet && !inFlow) missing += number
            delay(GAP_BETWEEN_MESSAGES)
        }
        report("SUMMARY sent=$messageCount notEchoed=$notEchoed missingFromThreadFlow=$missing thread=${handler.get().id}")
        // Without echoes the test proves nothing about threadFlow, so a backend that delivers nothing fails it too.
        assertEquals("Messages the server never echoed into handler.get()", emptyList<Int>(), notEchoed)
        assertEquals("Messages echoed into handler.get() but never emitted by threadFlow", emptyList<Int>(), missing)
    }

    /**
     * Sends message [number] and waits for its echo.
     *
     * @return whether the echo reached `handler.get()` and whether `threadFlow` emitted it.
     */
    private suspend fun sendAndTrack(handler: ChatThreadHandler, number: Int, seenInFlow: Set<String>): Pair<Boolean, Boolean> {
        val id = handler.messages().send("DE-182612 repro message $number")
        val inGet = withTimeoutOrNull(ECHO_TIMEOUT) {
            while (handler.get().messages.none { it.id.toString() == id }) delay(POLL_INTERVAL)
            true
        } == true
        // Give the collector time to receive an emission for the same echo.
        withTimeoutOrNull(EMISSION_GRACE) {
            while (id !in seenInFlow) delay(POLL_INTERVAL)
        }
        val inFlow = id in seenInFlow
        report("message $number id=$id inGet=$inGet inThreadFlow=$inFlow")
        return inGet to inFlow
    }

    private fun report(text: String) = logger.log(Level.Info, "DE182612: $text")

    private companion object {
        // The SDK logs through the same tag, so one `adb logcat -s DE182612` shows the test and the SDK side by side.
        val logger = LoggerAndroid("DE182612")
        const val BRAND_ID = 6455L
        const val LIVE_CHAT_CHANNEL_ID = "chat_c8b06b11-1f1e-437f-ae57-ac1bdf58c55d"
        const val DEFAULT_MESSAGE_COUNT = 5
        val THREADS_TIMEOUT = 20.seconds
        val ECHO_TIMEOUT = 15.seconds
        val EMISSION_GRACE = 3.seconds
        val POLL_INTERVAL = 0.1.seconds
        val GAP_BETWEEN_MESSAGES = 1.seconds
        val QA_EU1 = EnvironmentInternal(
            name = "QA_EU1",
            location = "Europe",
            baseUrl = "https://channels-eu1-qa.brandembassy.com/",
            socketUrl = "wss://chat-gateway-eu1-qa.brandembassy.com",
            originHeader = "https://livechat-eu1-qa.brandembassy.com/",
            chatUrl = "https://channels-eu1-qa.brandembassy.com/chat/",
            loggerUrl = "https://app-eu1-qa.brandembassy.com/logger-public",
            tokenUrl = "https://digital-oauth-eu1-qa.brandembassy.com/",
        )
    }
}
