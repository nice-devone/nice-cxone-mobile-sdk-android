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

import com.nice.cxonechat.internal.model.ChannelConfiguration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.TimeMark
import kotlin.time.TimeSource

/**
 * In-memory cache of the last fetched [ChannelConfiguration], keyed by environment, brand and
 * channel. The environment is part of the key because `getChannel` is called against
 * [com.nice.cxonechat.state.Environment.chatUrl] — a self-hosted or region-specific environment can
 * reuse the same brand/channel IDs as another one, and without the environment in the key a build
 * targeting one environment could be served a cached configuration fetched for a different one.
 *
 * Every successful build stores the fetched configuration here and every build checks it first,
 * avoiding a repeated getChannel REST call on every app open/config change/prepare(). [get] has two
 * lookup modes:
 * - `ignoreTtl = false` (the default, used by a regular [com.nice.cxonechat.ChatInstanceProvider.prepare]):
 *   the entry is only reused while younger than [TTL]; once it expires, [get] returns null so the
 *   next build re-fetches and [put] refreshes the entry with a new TTL window.
 * - `ignoreTtl = true` (used for per-session Chat rebuilds, see
 *   [ChatBuilderInternal.setPreferCachedConfiguration]): the entry is reused no matter its age. That
 *   call is a stronger guarantee than mere freshness — the configuration is fixed for the lifetime
 *   of a prepared provider, and a rebuild (e.g. during a reconnect on a flaky connection) should not
 *   trade that guarantee for an extra network dependency.
 *
 * The cache is process-scoped and cleared on [com.nice.cxonechat.ChatInstanceProvider.signOut]. [put]
 * silently skips a write started before the most recent [clear] — `fetchChannelConfiguration`'s
 * network call is blocking and not cancellable mid-flight, so a build in progress when
 * [signOut][com.nice.cxonechat.ChatInstanceProvider.signOut] runs could otherwise complete afterward
 * and undo the clear. Callers capture [currentGeneration] before fetching and pass it to [put]; a
 * mismatch means a [clear] happened while the fetch was in flight, and the stale write is dropped.
 *
 * [clear] is not scoped to any one build or provider — [generation] is process-wide, so an unrelated
 * [com.nice.cxonechat.ChatInstanceProvider.signOut] can also make a [put] stale even though this
 * build's own coroutine was never cancelled. For that reason [put] must not throw here: whether this
 * particular build should actually be treated as cancelled is a question about its own coroutine
 * [Job][kotlinx.coroutines.Job], not about this cache's generation, and is answered by the caller
 * checking [kotlinx.coroutines.ensureActive] instead.
 */
internal object ChannelConfigurationCache {

    private val TTL = 5.minutes

    // Overridable only for tests; production always uses the monotonic system time source. Deliberately
    // NOT Clock/Instant: this TTL is a purely relative "how long ago was this fetched", never compared
    // against a server-issued deadline, so it doesn't need wall-clock time — and using it would make the
    // TTL vulnerable to the device clock jumping backward (NTP sync, manual change), which would keep an
    // entry looking "fresh" until the clock caught back up, possibly hours or days later.
    @Volatile
    internal var timeSource: TimeSource = TimeSource.Monotonic

    @Volatile
    private var generation: Long = 0L

    private data class Entry(
        val chatUrl: String,
        val brandId: Int,
        val channelId: String,
        val configuration: ChannelConfiguration,
        val markedAt: TimeMark,
    )

    @Volatile
    private var entry: Entry? = null

    /** Snapshot to pass to [put] once the caller's fetch, started now, completes. */
    fun currentGeneration(): Long = generation

    fun get(chatUrl: String, brandId: Int, channelId: String, ignoreTtl: Boolean = false): ChannelConfiguration? =
        entry
            ?.takeIf { it.chatUrl == chatUrl && it.brandId == brandId && it.channelId == channelId }
            ?.takeIf { ignoreTtl || it.markedAt.elapsedNow() < TTL }
            ?.configuration

    /**
     * @param chatUrl the environment's [com.nice.cxonechat.state.Environment.chatUrl] the configuration was fetched from.
     * @param brandId the brand ID the configuration was fetched for.
     * @param channelId the channel ID the configuration was fetched for.
     * @param configuration the fetched [ChannelConfiguration] to store.
     * @param startedAtGeneration the value [currentGeneration] returned before this write's fetch began.
     * Silently does nothing if [clear] ran since — a [generation] mismatch only means *some* build's
     * result should no longer be cached, not that this specific caller was cancelled; callers that
     * need to know whether their own build was cancelled must check their own coroutine `Job` (e.g.
     * via [kotlinx.coroutines.ensureActive]) rather than rely on this write silently succeeding or not.
     */
    fun put(chatUrl: String, brandId: Int, channelId: String, configuration: ChannelConfiguration, startedAtGeneration: Long) {
        // The generation check and the entry write must be one atomic step: without the lock, clear()
        // could run on another thread between them and have its clear silently overwritten by this
        // write, reopening the exact stale-write race this generation check exists to close.
        synchronized(this) {
            if (startedAtGeneration == generation) {
                entry = Entry(chatUrl, brandId, channelId, configuration, timeSource.markNow())
            }
        }
    }

    fun clear() {
        synchronized(this) {
            entry = null
            generation++
        }
    }
}
