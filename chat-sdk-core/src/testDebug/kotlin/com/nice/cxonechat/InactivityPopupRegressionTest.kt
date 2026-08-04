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

import com.nice.cxonechat.internal.model.ChatThreadMutable
import com.nice.cxonechat.internal.model.ChatThreadMutable.Companion.asMutable
import com.nice.cxonechat.model.makeChatThread
import com.nice.cxonechat.server.ServerResponse
import com.nice.cxonechat.server.ServerResponse.Message.InactivityPopup
import com.nice.cxonechat.server.ServerResponse.MessageCreated
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Reproductions of DE-172157 — InactivityPopup not appearing in UI.
 *
 * These mirror the *production* consumption pattern in
 * ChatThreadViewModel.init (chat-sdk-ui), which differs from the existing happy-path
 * tests in [ChatThreadActionHandlerLiveChatTest]:
 *  - the UI obtains the handler from a StateFlow and re-runs a collectLatest block,
 *  - it calls handler.actions() (a *cached* instance via ChatThreadHandlerLogging),
 *  - it calls close() on the previous handler before re-collecting,
 *  - it collects popupFlow (suspending) instead of registering an onPopup callback.
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal class InactivityPopupRegressionTest : AbstractLiveChatTest() {

    private lateinit var thread: ChatThreadMutable

    override fun prepare() {
        super.prepare()
        thread = makeChatThread().asMutable()
    }

    // ---

    /** Exact production loop, single handler emission. */
    @Test
    fun popup_delivered_through_production_collectLatest_loop() = runTest {
        val handlerFlow = MutableStateFlow(chat.threads().thread(thread))
        val received = mutableListOf<Popup>()
        val job = launch {
            handlerFlow.collectLatest { handler ->
                handler.actions().popupFlow.collect { received += it }
            }
        }
        advanceUntilIdle()

        serverResponds(MessageCreated(thread, InactivityPopup(thread.id)))
        advanceUntilIdle()

        job.cancel()
        assertTrue(received.any { it is Popup.InactivityPopup }, "popup not delivered: $received")
    }

    /**
     * Production loop where the handler StateFlow re-emits (e.g. reconnect / thread rebuild)
     * before the popup arrives. Re-emission cancels the inner collect, closes the previous
     * actions handler, then re-acquires actions() — which is a *cached* instance.
     */
    @Test
    fun popup_delivered_after_handler_reemission() = runTest {
        val handlerFlow = MutableStateFlow(chat.threads().thread(thread))
        val received = mutableListOf<Popup>()
        val job = launch {
            handlerFlow.collectLatest { handler ->
                handler.actions().popupFlow.collect { received += it }
            }
        }
        advanceUntilIdle()

        // Simulate the repository handing out a fresh handler for the same thread.
        handlerFlow.value = chat.threads().thread(thread)
        advanceUntilIdle()

        serverResponds(MessageCreated(thread, InactivityPopup(thread.id)))
        advanceUntilIdle()

        job.cancel()
        assertTrue(received.any { it is Popup.InactivityPopup }, "popup not delivered after re-emission: $received")
    }

    /**
     * THE production-failure repro (DE-172157): after a close()/reconnect cycle the UI re-acquires
     * the *cached* actions() handler and re-collects popupFlow. With the old hot-SharedFlow design
     * the listener job (launched on the chat scope, cancelled by Chat.close()) was dead and never
     * rebuilt, so popups after reconnect were dropped. With the cold popupFlow, re-collection
     * re-establishes the socket subscription and delivery resumes.
     *
     * Note: a cold flow does not retain popups delivered while no collector is active (mirrors
     * threadFlow). Production collects popupFlow continuously, so that gap does not occur.
     */
    @Test
    fun popup_delivered_after_close_and_reacquire_cached_actions() = runTest {
        val handler = chat.threads().thread(thread)
        // Collect once (a session), end it, then re-acquire the cached actions() handler and
        // re-collect — mirrors the UI re-subscribing after a reconnect. The cold popupFlow
        // re-establishes its socket subscription on each new collection.
        val firstJob = launch { handler.actions().popupFlow.collect { } }
        advanceUntilIdle()
        firstJob.cancel()
        val second = handler.actions()

        val received = mutableListOf<Popup>()
        val job = launch { second.popupFlow.collect { received += it } }
        advanceUntilIdle()

        serverResponds(MessageCreated(thread, InactivityPopup(thread.id)))
        advanceUntilIdle()

        job.cancel()
        assertTrue(received.any { it is Popup.InactivityPopup }, "popup not delivered from re-acquired cached handler: $received")
    }

    /**
     * Return-from-background (DE-172157): the foreground collector is cancelled (app backgrounded),
     * then a new collector subscribes on return. The backend re-delivers the popup via
     * LiveChatThreadRecovered (as observed in production reconnect captures). The cold popupFlow must
     * deliver it to the new collector — guards against "popup missing after returning from background".
     */
    @Test
    fun popup_redelivered_via_recovery_after_background_is_delivered() = runTest {
        val handler = chat.threads().thread(thread)
        val actions = handler.actions()

        // Foreground: a collector subscribes, then the app is backgrounded (collector cancelled).
        val foreground = launch { actions.popupFlow.collect { } }
        advanceUntilIdle()
        foreground.cancel()
        advanceUntilIdle()

        // Return from background: a new collector subscribes and the server re-delivers via recovery.
        val received = mutableListOf<Popup>()
        val resumed = launch { actions.popupFlow.collect { received += it } }
        advanceUntilIdle()
        serverResponds(ServerResponse.LivechatRecovered(thread = thread, messages = arrayOf(InactivityPopup(thread.id))))
        advanceUntilIdle()

        resumed.cancel()
        assertTrue(received.any { it is Popup.InactivityPopup }, "recovery popup after background not delivered: $received")
    }
}
