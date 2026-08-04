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

import com.nice.cxonechat.internal.model.network.EventCaseStatusChanged.CaseStatus.Closed
import com.nice.cxonechat.model.makeChatThread
import com.nice.cxonechat.server.ServerResponse
import com.nice.cxonechat.server.ServerResponse.Message.InactivityPopup
import com.nice.cxonechat.server.ServerResponse.MessageCreated
import com.nice.cxonechat.thread.ChatThread
import io.mockk.every
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Assumption-validation gate for the "session = Chat instance" architecture (DE-172157).
 *
 * Chat.close() cancels the children of the per-Chat session coroutine scope; handler-internal
 * listener jobs launched at construction die with it. These tests assert, against the SDK baseline,
 * what survives a close()+reconnect cycle and what a per-session RECREATION of the Chat instance
 * (the planned architecture) restores.
 *
 * Predicted matrix (post DE-172915: per-thread threadFlow is now shared, matching threadsFlow):
 *  - recreation popup / recreation threadsFlow ........ PASS (fresh instance graph works)
 *  - same-instance popup ............................... FAIL (memoized dead handler)
 *  - same-instance threadsFlow / per-thread threadFlow . FAIL (dead shareIn — see DE-172915)
 *  - same-instance global Chat.actions() .............. PASS (persistent socket callback)
 *
 * None of the FAIL cases matter in practice: production always goes through
 * [com.nice.cxonechat.ChatInstanceProvider], which rebuilds the [Chat] instance on reconnect rather
 * than reusing it, so a fresh handler graph is fetched instead of reviving a dead one.
 */
internal class SessionHandlerRecreationTest : AbstractLiveChatTest() {

    override fun prepare() {
        // The shared mock WebSocket only stubs send(); Chat.close() also invokes close(code, reason).
        every { socket.close(any(), any()) } returns true
        super.prepare()
    }

    // ---

    @Test
    fun `recreated chat delivers inactivity popup from livechat recovered after close`() {
        connect()
        val existingThread = makeChatThread()
        val returnedThread = recoverThread(existingThread).single()
        assertEquals(existingThread.id, returnedThread.id, "existing livechat thread must be returned")

        // Session 1: handler created, inactivity listener registered, popup delivered.
        val sessionOneActions = chat.threads().thread(returnedThread).actions()
        val sessionOnePopups = mutableListOf<Popup>()
        val sessionOneJob = testScope.launch { sessionOneActions.popupFlow.collect { sessionOnePopups += it } }
        socketServer.sendServerMessage(MessageCreated(returnedThread, InactivityPopup(returnedThread.id)))
        assertTrue(
            sessionOnePopups.any { it is Popup.InactivityPopup },
            "session-1 listener must be notified about the popup"
        )
        sessionOneJob.cancel()

        // Background + return: session ends, architecture recreates the Chat instance.
        closeChatAndAwaitDrain()
        buildChat()
        connect()

        // Session 2: recreated handler with a newly registered listener receives the popup
        // redelivered inside the new LivechatRecovered event.
        val sessionTwoActions = chat.threads().thread(existingThread).actions()
        val sessionTwoPopups = mutableListOf<Popup>()
        val sessionTwoJob = testScope.launch { sessionTwoActions.popupFlow.collect { sessionTwoPopups += it } }
        socketServer.sendServerMessage(
            ServerResponse.LivechatRecovered(
                thread = existingThread,
                messages = arrayOf(InactivityPopup(existingThread.id)),
            )
        )
        sessionTwoJob.cancel()
        assertTrue(
            sessionTwoPopups.any { it is Popup.InactivityPopup },
            "recreated handler must receive the popup from the new LivechatRecovered event"
        )
    }

    @Test
    fun `recreated chat threads flow delivers fresh thread list after close`() {
        connect()
        val firstThread = makeChatThread()
        assertEquals(firstThread.id, recoverThread(firstThread).single().id)

        closeChatAndAwaitDrain()
        buildChat()
        connect()

        val secondThread = makeChatThread()
        val freshList = recoverThread(secondThread)
        assertEquals(
            secondThread.id,
            freshList.single().id,
            "recreated chat's threadsFlow must deliver the fresh post-reconnect thread list"
        )
    }

    @Test
    fun `recreated chat per-thread thread flow delivers fresh case status change after close`() {
        connect()
        val existingThread = makeChatThread()
        val returnedThread = recoverThread(existingThread).single()
        // Production fidelity: acquire during session 1 (memoized), same as the same-instance tests.
        chat.threads().thread(returnedThread)

        closeChatAndAwaitDrain()
        buildChat()
        connect()

        // Session 2: recreated Chat instance yields a fresh handler graph (new ChatThreadsHandlerImpl,
        // new memoization map, new ChatThreadHandlerShared subscription) — unlike the same-instance
        // case, this one must deliver the live update.
        val handler = chat.threads().thread(existingThread)
        val updates = mutableListOf<ChatThread>()
        val updatesJob = testScope.launch { handler.threadFlow.collect { updates += it } }
        socketServer.sendServerMessage(ServerResponse.CaseStatusChanged(existingThread, Closed))
        updatesJob.cancel()
        assertTrue(
            updates.any { !it.canAddMoreMessages },
            "recreated chat's per-thread threadFlow must deliver the fresh post-reconnect case status change"
        )
    }

    @Test
    fun `same chat instance delivers inactivity popup after reconnect`() {
        connect()
        val existingThread = makeChatThread()
        val returnedThread = recoverThread(existingThread).single()

        // Production fidelity: the handler is acquired DURING session 1 (memoized per thread id),
        // so the post-reconnect re-acquisition returns the cached instance, not a fresh one.
        val sessionOneActions = chat.threads().thread(returnedThread).actions()
        val sessionOnePopups = mutableListOf<Popup>()
        val sessionOneJob = testScope.launch { sessionOneActions.popupFlow.collect { sessionOnePopups += it } }
        socketServer.sendServerMessage(MessageCreated(returnedThread, InactivityPopup(returnedThread.id)))
        assertTrue(sessionOnePopups.any { it is Popup.InactivityPopup }, "session-1 popup must work")
        sessionOneJob.cancel()

        closeChatAndAwaitDrain()
        connect() // reconnect the SAME instance — no recreation

        val reacquiredActions = chat.threads().thread(returnedThread).actions()
        val popups = mutableListOf<Popup>()
        val popupJob = testScope.launch { reacquiredActions.popupFlow.collect { popups += it } }
        // popupFlow is hot with replay=1 — the session-1 popup is redelivered on subscription.
        // Only an emission BEYOND the replayed baseline proves the listener is alive.
        val replayedCount = popups.size
        socketServer.sendServerMessage(
            ServerResponse.LivechatRecovered(
                thread = existingThread,
                messages = arrayOf(InactivityPopup(existingThread.id)),
            )
        )
        popupJob.cancel()
        assertTrue(
            popups.size > replayedCount,
            "re-acquired handler on the same instance must receive a NEW popup beyond any replayed one " +
                "(cold popupFlow re-subscribes per collection; on the pre-fix baseline this failed — " +
                "memoized handler with a dead constructor-launched listener job)"
        )
    }

    @Test
    fun `same chat instance threads flow only replays the stale list after reconnect`() {
        connect()
        val firstThread = makeChatThread()
        assertEquals(firstThread.id, recoverThread(firstThread).single().id)

        closeChatAndAwaitDrain()
        connect() // reconnect the SAME instance — no recreation

        // Documents the constraint that motivates per-session Chat recreation: threadsFlow's
        // shareIn coroutine is a session-scope child killed by close() and Lazily never restarts,
        // so a post-reconnect collector only ever sees the stale pre-close replay. If this test
        // ever fails, core threadsFlow became restartable and the recreation architecture can be
        // revisited.
        val secondThread = makeChatThread()
        val staleList = recoverThread(secondThread)
        assertEquals(
            firstThread.id,
            staleList.single().id,
            "same-instance threadsFlow is expected to replay only the stale pre-close list"
        )
    }

    @Test
    fun `same chat instance per-thread thread flow only replays the stale state after reconnect`() {
        connect()
        val existingThread = makeChatThread()
        val returnedThread = recoverThread(existingThread).single()
        // Acquire during session 1 so the post-reconnect call returns the memoized instance.
        val handler = chat.threads().thread(returnedThread)

        // Seed the shareIn's replay cache with the pre-close snapshot. SharingStarted.Lazily only
        // starts the upstream subscription on first collection — without collecting here, the
        // post-reconnect collector below would be the FIRST-EVER collector and could start a
        // genuinely fresh subscription instead of observing a stale replay, making the assertions
        // pass vacuously (on an empty list) regardless of whether staleness actually held.
        // join() (not cancel()) so the seed collection is deterministically complete before
        // proceeding, rather than relying on the test dispatcher's eager-execution timing.
        val seedJob = testScope.launch { handler.threadFlow.first() }
        runBlocking { seedJob.join() }

        closeChatAndAwaitDrain()
        connect() // reconnect the SAME instance — no recreation

        // Documents the same constraint as "same chat instance threads flow only replays the stale
        // list after reconnect" (DE-172915): threadFlow's shareIn coroutine is a session-scope child
        // killed by close() and Lazily never restarts, so a post-reconnect collector only ever sees
        // the stale pre-close replay, never the CaseStatusChanged sent below. If this test ever
        // fails, per-thread threadFlow became restartable and the recreation architecture can be
        // revisited.
        val updates = mutableListOf<ChatThread>()
        val updatesJob = testScope.launch { handler.threadFlow.collect { updates += it } }
        socketServer.sendServerMessage(ServerResponse.CaseStatusChanged(returnedThread, Closed))
        updatesJob.cancel()
        assertTrue(updates.isNotEmpty(), "replay cache must have delivered the stale pre-close snapshot")
        assertTrue(
            updates.none { !it.canAddMoreMessages },
            "same-instance threadFlow is expected to replay only the stale pre-close state"
        )
    }

    @Test
    fun `same chat instance global actions deliver popup after reconnect`() {
        connect()
        val globalActions = chat.actions()

        closeChatAndAwaitDrain()
        connect()

        val popups = mutableListOf<PopupEvent>()
        val popupJob = testScope.launch { globalActions.popupFlow.collect { popups += it } }
        socketServer.sendServerMessage(ServerResponse.ActionPopup(mapOf("headline" to "gate")))
        popupJob.cancel()
        assertTrue(
            popups.isNotEmpty(),
            "global Chat.actions() popup delivery must keep working on the same instance after reconnect"
        )
    }

    // ---

    /**
     * Recovers [thread] on the server side and returns the resulting thread-list emission.
     * A collector must be active before the recovery event arrives (threadsFlow is shared lazily).
     */
    private fun recoverThread(thread: ChatThread): List<ChatThread> {
        val emissions = mutableListOf<List<ChatThread>>()
        val collectJob = testScope.launch { chat.threads().threadsFlow.collect { emissions += it } }
        socketServer.sendServerMessage(ServerResponse.LivechatRecovered(thread = thread))
        collectJob.cancel()
        return emissions.last()
    }

    /**
     * Simulates the app going to background: closes the chat and waits for the asynchronous
     * storage-drain sentinel to finish before the next session starts — deterministic equivalent
     * of the real background pause. (The sentinel only sweeps the close-time snapshot, so awaiting
     * it is belt-and-braces; the storage scope itself intentionally stays alive.)
     */
    private fun closeChatAndAwaitDrain() {
        val storageJob = requireNotNull(entrails.threading.storageWriteScope.coroutineContext[Job])
        val childrenBeforeClose = storageJob.children.toSet()
        chat.close()
        val drainSentinels = storageJob.children.filterNot(childrenBeforeClose::contains).toList()
        runBlocking { drainSentinels.forEach { it.join() } }
    }
}
