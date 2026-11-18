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

import com.nice.cxonechat.internal.ChannelConfigurationCache
import com.nice.cxonechat.internal.ChatBuilderInternal
import com.nice.cxonechat.internal.model.ChannelConfiguration
import com.nice.cxonechat.state.Connection
import com.nice.cxonechat.tool.SocketFactoryMock
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Test
import retrofit2.Call
import retrofit2.Response
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.TimeMark
import kotlin.time.TimeSource

/**
 * Tests for [ChatBuilder.build]'s interaction with [ChannelConfigurationCache]: cache-first reuse
 * (both the internal preferCachedConfiguration path and the public TTL-bounded path), TTL expiry,
 * and two distinct outcomes when a fetch races a [ChannelConfigurationCache.clear]:
 * - an unrelated build's [ChannelConfigurationCache.clear] only skips caching this fetch's result,
 *   since this build's own coroutine was never cancelled by it;
 * - this build's own coroutine being genuinely cancelled must both discard its result and keep the
 *   fetch from ever being cached, no matter when the cancellation happened relative to the fetch.
 */
internal class ChatBuilderChannelConfigurationCacheTest : AbstractChatTestSubstrate() {

    override fun prepare() = Unit

    @Test
    fun build_reusesCachedChannelConfiguration_whenPreferred() {
        ChannelConfigurationCache.clear()
        val call = mockk<Call<ChannelConfiguration?>> {
            every { execute() } returns Response.success(config)
        }
        every { service.getChannel(any(), any()) } returns call

        // First build fetches the configuration and populates the cache (a provider prepare()).
        assert(build().isSuccess)
        // A per-session rebuild preferring the cache must not repeat the costly getChannel call.
        val rebuild = build {
            (this as? ChatBuilderInternal)?.setPreferCachedConfiguration(true)
            this
        }
        assert(rebuild.isSuccess)
        verify(exactly = 1) { call.execute() }
    }

    @Test
    fun build_reusesCachedChannelConfiguration_withinTtl_evenWhenNotPreferred() {
        ChannelConfigurationCache.clear()
        val call = mockk<Call<ChannelConfiguration?>> {
            every { execute() } returns Response.success(config)
        }
        every { service.getChannel(any(), any()) } returns call

        // Public build() is cache-first too: a second build within the cache's TTL reuses the
        // configuration fetched by the first instead of repeating the costly getChannel call.
        assert(build().isSuccess)
        assert(build().isSuccess)
        verify(exactly = 1) { call.execute() }
    }

    @Test
    fun build_fetchesConfigurationAgain_afterTtlExpires_whenNotPreferred() {
        ChannelConfigurationCache.clear()
        val call = mockk<Call<ChannelConfiguration?>> {
            every { execute() } returns Response.success(config)
        }
        every { service.getChannel(any(), any()) } returns call
        val originalTimeSource = ChannelConfigurationCache.timeSource
        val fakeTimeSource = FakeTimeSource()
        ChannelConfigurationCache.timeSource = fakeTimeSource

        try {
            assert(build().isSuccess)
            fakeTimeSource.elapsed += 5.minutes
            // Once the cache entry is older than the TTL, a build without the internal preference
            // must fetch again rather than reuse the stale entry.
            assert(build().isSuccess)
            verify(exactly = 2) { call.execute() }
        } finally {
            ChannelConfigurationCache.timeSource = originalTimeSource
        }
    }

    @Test
    fun build_skipsCachingStaleFetch_whenClearRacesInFlightFetch() {
        ChannelConfigurationCache.clear()
        val (connection, _) = prepareBuilder()
        val fetchStarted = CountDownLatch(1)
        val releaseFetch = CountDownLatch(1)
        val call = mockk<Call<ChannelConfiguration?>> {
            every { execute() } answers {
                fetchStarted.countDown()
                releaseFetch.await()
                Response.success(config)
            }
        }
        every { service.getChannel(any(), any()) } returns call

        var result: Result<Chat>? = null
        val thread = Thread { result = build() }.apply { isDaemon = true }
        thread.start()

        try {
            assert(fetchStarted.await(5, TimeUnit.SECONDS))
            // Simulates an unrelated provider's signOut() landing while this build's fetch is still
            // in flight -- this build's own coroutine is never cancelled by it.
            ChannelConfigurationCache.clear()
        } finally {
            releaseFetch.countDown()
        }
        thread.join(5_000)

        assertFalse(thread.isAlive)
        assert(result?.isSuccess == true) {
            "Expected build() to succeed since its own coroutine was never cancelled, got ${result?.exceptionOrNull()}"
        }
        // The fetch started before the intervening clear(), so its write is skipped rather than
        // resurrecting a cache the concurrent clear() was meant to leave empty.
        assertNull(ChannelConfigurationCache.get(connection.environment.chatUrl, connection.brandId, connection.channelId))
    }

    @Test
    fun build_neverCachesFetch_whenOwnCoroutineIsCancelledDuringFetch() {
        ChannelConfigurationCache.clear()
        val (connection, builder) = prepareBuilder()
        val fetchStarted = CountDownLatch(1)
        val releaseFetch = CountDownLatch(1)
        val call = mockk<Call<ChannelConfiguration?>> {
            every { execute() } answers {
                fetchStarted.countDown()
                releaseFetch.await()
                Response.success(config)
            }
        }
        every { service.getChannel(any(), any()) } returns call

        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        var result: Result<Chat>? = null
        val job = scope.launch {
            result = runCatching { builder.setDevelopmentMode(true).build() }
        }

        try {
            assert(fetchStarted.await(5, TimeUnit.SECONDS))
            // Simulates THIS build's own provider calling signOut() (job.cancel()) while its fetch
            // is still in flight -- unlike the unrelated-clear() case above, ensureActive() must
            // catch this one, both to discard the build's own result and to keep the fetch from
            // being cached at all.
            job.cancel()
        } finally {
            releaseFetch.countDown()
        }
        runBlocking { withTimeout(5_000) { job.join() } }

        assert(result?.exceptionOrNull() is CancellationException) {
            "Expected CancellationException, got ${result?.exceptionOrNull()}"
        }
        assertNull(ChannelConfigurationCache.get(connection.environment.chatUrl, connection.brandId, connection.channelId))
    }

    private class FakeTimeSource : TimeSource {
        var elapsed: Duration = Duration.ZERO

        override fun markNow(): TimeMark {
            val markedAt = elapsed
            return object : TimeMark {
                override fun elapsedNow(): Duration = elapsed - markedAt
            }
        }
    }

    private fun prepareBuilder(): Pair<Connection, ChatBuilder> {
        val factory = SocketFactoryMock(socket, proxyListener)
        val connection = factory.getConfiguration(storage)
        return connection to ChatBuilder(entrails, factory)
    }

    private fun build(
        builder: ChatBuilder = prepareBuilder().second,
        body: ChatBuilder.() -> ChatBuilder = { this },
    ): Result<Chat> = runBlocking {
        runCatching {
            builder
                .setDevelopmentMode(true)
                .body()
                .build()
        }
    }
}
