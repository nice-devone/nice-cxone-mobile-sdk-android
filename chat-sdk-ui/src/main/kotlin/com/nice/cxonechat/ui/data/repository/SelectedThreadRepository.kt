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

package com.nice.cxonechat.ui.data.repository

import com.nice.cxonechat.Chat
import com.nice.cxonechat.ChatInstanceProvider
import com.nice.cxonechat.ChatState
import com.nice.cxonechat.ChatThreadHandler
import com.nice.cxonechat.Popup
import com.nice.cxonechat.log.Logger
import com.nice.cxonechat.log.LoggerScope
import com.nice.cxonechat.log.warning
import com.nice.cxonechat.ui.UiModule
import com.nice.cxonechat.ui.domain.model.NoThreadHandler
import com.nice.cxonechat.ui.storage.ValueStorage
import com.nice.cxonechat.ui.storage.ValueStorage.StringKey.EndContactShownForThreadKey
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.koin.core.annotation.Named
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * Session-aware holder of the currently selected thread.
 *
 * SDK handlers are scoped to a connected session (one [Chat] instance): [ChatInstanceProvider]
 * rebuilds the instance for each session, and a previous session's handler graph is dead. This
 * repository therefore re-derives the selected thread's handler from the fresh [Chat] on every
 * [ChatInstanceProvider.Listener.onChatChanged], keeping the thread identity but never serving a
 * stale handler — the derived instance re-keys every downstream collector, because the state flow
 * emits on reference inequality.
 */
@Single
internal class SelectedThreadRepository(
    chatProvider: ChatInstanceProvider,
    private val valueStorage: ValueStorage,
    @Provided
    @Named(UiModule.LOGGER_NAME)
    logger: Logger,
) : ChatInstanceProvider.Listener, LoggerScope by LoggerScope<SelectedThreadRepository>(logger) {
    private val mutableChatThreadHandlerFlow: MutableStateFlow<ChatThreadHandler> = MutableStateFlow(NoThreadHandler)

    var chatThreadHandler: ChatThreadHandler
        get() = mutableChatThreadHandlerFlow.value
        set(value) {
            mutableChatThreadHandlerFlow.value = value
        }
    val chatThreadHandlerFlow = mutableChatThreadHandlerFlow.asStateFlow()

    // Tracked per owner (e.g. a ChatThreadViewModel instance) rather than as a single last-writer-
    // wins flag - ChatActivity is singleTop, so two instances (and two owners) can briefly overlap,
    // and one clearing its own state must not clear a still-showing owner's. A single StateFlow
    // (updated atomically) rather than a separate set + flag avoids two callers interleaving a
    // stale write.
    private val mutableSurveyShowingOwners = MutableStateFlow<Set<Any>>(emptySet())

    /**
     * Whether a satisfaction survey is currently shown for any thread, by any owner.
     *
     * Shared across view models so a pre-chat survey needed to start a new thread can wait for a
     * satisfaction survey recovered for a previous one to be seen first, instead of covering it.
     */
    val isSatisfactionSurveyShowing: Boolean
        get() = mutableSurveyShowingOwners.value.isNotEmpty()
    val satisfactionSurveyShowingFlow: Flow<Boolean> = mutableSurveyShowingOwners
        .map { it.isNotEmpty() }
        .distinctUntilChanged()

    /** Exposed so tests can wait for a real subscriber to actually attach, instead of guessing. */
    val surveyShowingSubscriptionCount: StateFlow<Int> = mutableSurveyShowingOwners.subscriptionCount

    // Lives here, not on ChatThreadViewModel, for the same reason as mutableSurveyShowingOwners
    // above: the persisted EndContactShownForThreadKey is shared across potentially-overlapping
    // owners, so a per-instance mutex/cache can neither serialize nor stay consistent with a
    // sibling instance's writes to the same key.
    private val endContactMutex = Mutex()
    private var endContactShownForThreadCache: String? = null
    private var endContactCacheLoaded = false

    // Falls back to memory only when the DataStore write in stashSurvey() itself fails - core has
    // already marked the survey delivered, so losing it here would lose it for good.
    private val inMemorySurveyFallback = ConcurrentHashMap<UUID, Popup.SatisfactionSurvey>()

    // Bumped on sign-out, so an in-flight stashSurvey() write can tell its session just ended.
    private val signOutGeneration = AtomicInteger(0)

    private val sessionGeneration = AtomicInteger(0)

    /** Whether this repository saw any chat state, so a prepare as the first state seen is not a new session. */
    private val sawChatState = AtomicBoolean(false)

    /**
     * Changes whenever the session may have ended, so work that outlives it (e.g. a pending attachment pick) can tell.
     * A sign-out is not always notified, so a return to [ChatState.Initial] and any prepare after another state count too.
     */
    val currentSessionGeneration: Int
        get() = sessionGeneration.get()

    // Fire-and-forget only: onChatChanged() is a synchronous listener callback, not a suspend
    // function, and sign-out cleanup must not block or fail it.
    private val repositoryScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    init {
        // The repository is a Koin singleton, so this instance stays strongly reachable for the
        // whole process despite the provider's weak listener registration.
        chatProvider.addListener(this)
    }

    /** Serializes every read-decide-write sequence on the EndContact shown-for-thread mark. */
    suspend fun <T> withEndContactLock(action: suspend () -> T): T = endContactMutex.withLock { action() }

    // A read failure must not abort the caller's whole action (bestEffortEndContactUpdate would
    // otherwise skip showing EndContact entirely) - treat it the same as an absent marker instead.
    suspend fun getEndContactShownForThread(): String? {
        if (!endContactCacheLoaded) {
            endContactShownForThreadCache = try {
                valueStorage.getString(EndContactShownForThreadKey).firstOrNull()?.takeIf(String::isNotEmpty)
            } catch (expected: CancellationException) {
                throw expected
            } catch (logged: Exception) {
                warning("Failed to read the EndContact shown-for-thread mark", logged)
                null
            }
            endContactCacheLoaded = true
        }
        return endContactShownForThreadCache
    }

    // The persisted write failing must not stop the caller - the cache is updated either way, so
    // the caller's follow-up (e.g. showing EndContact) stays reachable even though the mark didn't
    // make it to disk. The cache update sits after the suspending call, not before it, so a
    // cancellation here (the caller being torn down, e.g. by a thread switch) leaves the cache
    // untouched instead of recording "shown" for a dialog this run never got to show.
    suspend fun setEndContactShownForThread(shownForThreadId: String?) {
        try {
            valueStorage.setString(EndContactShownForThreadKey, shownForThreadId.orEmpty())
        } catch (expected: CancellationException) {
            throw expected
        } catch (logged: Exception) {
            warning("Failed to persist the EndContact shown-for-thread mark", logged)
        }
        endContactShownForThreadCache = shownForThreadId
        endContactCacheLoaded = true
    }

    // A read failure must not kill the caller's flow/coroutine (e.g. ChatThreadViewModel's
    // isArchived sharing coroutine) - log and continue. setEndContactShownForThread already
    // protects its own write, so this only has to guard getEndContactShownForThread's read and the
    // surrounding logic.
    suspend fun bestEffortEndContactUpdate(action: suspend () -> Unit) {
        try {
            action()
        } catch (expected: CancellationException) {
            throw expected
        } catch (logged: Exception) {
            warning("Failed to update the EndContact shown-for-thread mark", logged)
        }
    }

    /** Records whether [owner] is currently showing a satisfaction survey. */
    fun setSatisfactionSurveyShowing(owner: Any, showing: Boolean) {
        mutableSurveyShowingOwners.update { if (showing) it + owner else it - owner }
    }

    /**
     * Stashes [survey], consumed for [threadId] in the background, for later recovery via [takeSurvey].
     *
     * Persisted, not just held in memory: the SDK marks a Satisfaction Survey delivered in its own
     * persistent storage the instant it's consumed (`ChatThreadActionHandlerImpl.
     * surveyIfUndelivered`), which is a one-shot, cross-collector gate - no other collector will ever
     * see it again. If this stash were in-memory only, a process death between that background
     * consumption (e.g. to post a local notification) and the user opening the thread would lose the
     * survey permanently: the marker says "delivered", but nothing could ever recover what to show.
     * Stored encrypted and excluded from backups ([ValueStorage.updateEncryptedPendingSurvey]), since
     * [Popup.SatisfactionSurvey.surveyUrl] can carry customer/session identifiers.
     */
    suspend fun stashSurvey(threadId: UUID, survey: Popup.SatisfactionSurvey) {
        val generationAtStart = signOutGeneration.get()
        // The core already marked this survey delivered before the caller got it - a write failure
        // here must not propagate and crash the caller's long-lived collector, or the notification
        // this stash exists for would also never be posted.
        try {
            valueStorage.updateEncryptedPendingSurvey { raw ->
                Json.encodeToString(decodePendingSurveys(raw) + (threadId.toString() to PendingSurveyData(survey.text, survey.surveyUrl)))
            }
        } catch (expected: CancellationException) {
            throw expected
        } catch (logged: Exception) {
            warning("Failed to persist the stashed Satisfaction Survey", logged)
            // A sign-out racing in while this write was in flight already cleared the fallback for
            // this exact reason - don't resurrect a signed-out session's survey into it.
            if (signOutGeneration.get() == generationAtStart) {
                inMemorySurveyFallback[threadId] = survey
            }
        }
    }

    /** Returns and clears the survey stashed for [threadId], or `null` if none is pending. */
    suspend fun takeSurvey(threadId: UUID): Popup.SatisfactionSurvey? {
        inMemorySurveyFallback.remove(threadId)?.let { return it }
        var taken: PendingSurveyData? = null
        try {
            valueStorage.updateEncryptedPendingSurvey { raw ->
                val pending = decodePendingSurveys(raw)
                taken = pending[threadId.toString()]
                if (taken == null) raw else Json.encodeToString(pending - threadId.toString())
            }
        } catch (expected: CancellationException) {
            throw expected
        } catch (logged: Exception) {
            warning("Failed to read the stashed Satisfaction Survey", logged)
            // The transform above may have already run (capturing taken) before the commit itself
            // failed, leaving the entry still persisted - returning it here would show it now and
            // let a later successful call return the same survey again.
            taken = null
        }
        return taken?.let { RecoveredSatisfactionSurvey(text = it.text, surveyUrl = it.surveyUrl) }
    }

    override fun onChatChanged(chat: Chat?) {
        if (chat == null) {
            signOutGeneration.incrementAndGet()
            sessionGeneration.incrementAndGet()
            inMemorySurveyFallback.clear()
            repositoryScope.launch {
                try {
                    valueStorage.clearEncryptedPendingSurveys()
                } catch (expected: CancellationException) {
                    throw expected
                } catch (logged: Exception) {
                    warning("Failed to clear the Satisfaction Survey stash on sign-out", logged)
                }
            }
        }
        val currentHandler = mutableChatThreadHandlerFlow.value
        if (currentHandler === NoThreadHandler) return
        mutableChatThreadHandlerFlow.value = when (chat) {
            null -> NoThreadHandler
            else -> chat.threads().thread(currentHandler.get())
        }
    }

    override fun onChatStateChanged(chatState: ChatState) {
        val seenBefore = sawChatState.getAndSet(true)
        if (chatState == ChatState.Initial || chatState == ChatState.Preparing && seenBefore) {
            sessionGeneration.incrementAndGet()
        }
    }

    @Serializable
    private data class PendingSurveyData(val text: String, val surveyUrl: String)

    /** Reconstructs just enough of [Popup.SatisfactionSurvey] to recover the dialog from persisted data. */
    private data class RecoveredSatisfactionSurvey(
        override val text: String,
        override val surveyUrl: String,
    ) : Popup.SatisfactionSurvey {

        /** Redacts [surveyUrl] - it can carry customer/session identifiers, and this is logged (showDialog()). */
        override fun toString() = "RecoveredSatisfactionSurvey(text=$text, surveyUrl=<redacted>)"
    }

    private companion object {
        fun decodePendingSurveys(raw: String): Map<String, PendingSurveyData> =
            raw.takeIf(String::isNotEmpty)
                ?.let { runCatching { Json.decodeFromString<Map<String, PendingSurveyData>>(it) }.getOrNull() }
                .orEmpty()
    }
}
