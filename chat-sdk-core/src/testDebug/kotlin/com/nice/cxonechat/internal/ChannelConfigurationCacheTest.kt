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
import io.mockk.mockk
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeMark
import kotlin.time.TimeSource

/**
 * Tests for [ChannelConfigurationCache].
 *
 * The cache is a process-scoped singleton that stores exactly one entry at a time.
 * Cross-environment, cross-brand and cross-channel isolation are critical: returning a
 * configuration fetched for the wrong environment, brand or channel would expose incorrect
 * channel settings to an SDK session.
 */
internal class ChannelConfigurationCacheTest {

    private val urlA = "https://channels-de-eu1.niceincontact.com"
    private val urlB = "https://channels-de-na1.niceincontact.com"
    private val brandA = 1001
    private val brandB = 1002
    private val channelA = "channel-alpha"
    private val channelB = "channel-beta"

    private val configA = mockk<ChannelConfiguration>()
    private val configB = mockk<ChannelConfiguration>()

    private val originalTimeSource = ChannelConfigurationCache.timeSource
    private val fakeTimeSource = FakeTimeSource()

    @Before
    fun setUp() {
        ChannelConfigurationCache.timeSource = fakeTimeSource
        ChannelConfigurationCache.clear()
    }

    @After
    fun tearDown() {
        ChannelConfigurationCache.clear()
        ChannelConfigurationCache.timeSource = originalTimeSource
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

    @Test
    fun `get returns null when cache is empty`() {
        assertNull(ChannelConfigurationCache.get(urlA, brandA, channelA))
    }

    @Test
    fun `put then get returns the stored configuration for the exact same key`() {
        ChannelConfigurationCache.put(urlA, brandA, channelA, configA, ChannelConfigurationCache.currentGeneration())

        assertEquals(configA, ChannelConfigurationCache.get(urlA, brandA, channelA))
    }

    @Test
    fun `get returns null for a different brand ID with the same channel`() {
        ChannelConfigurationCache.put(urlA, brandA, channelA, configA, ChannelConfigurationCache.currentGeneration())

        assertNull(ChannelConfigurationCache.get(urlA, brandB, channelA))
    }

    @Test
    fun `get returns null for a different channel ID with the same brand`() {
        ChannelConfigurationCache.put(urlA, brandA, channelA, configA, ChannelConfigurationCache.currentGeneration())

        assertNull(ChannelConfigurationCache.get(urlA, brandA, channelB))
    }

    @Test
    fun `get returns null when both brand and channel differ`() {
        ChannelConfigurationCache.put(urlA, brandA, channelA, configA, ChannelConfigurationCache.currentGeneration())

        assertNull(ChannelConfigurationCache.get(urlA, brandB, channelB))
    }

    @Test
    fun `get returns null for the same brand and channel on a different environment`() {
        ChannelConfigurationCache.put(urlA, brandA, channelA, configA, ChannelConfigurationCache.currentGeneration())

        assertNull(ChannelConfigurationCache.get(urlB, brandA, channelA))
    }

    @Test
    fun `put overwrites the previous entry so only the latest key is retrievable`() {
        ChannelConfigurationCache.put(urlA, brandA, channelA, configA, ChannelConfigurationCache.currentGeneration())
        ChannelConfigurationCache.put(urlA, brandB, channelB, configB, ChannelConfigurationCache.currentGeneration())

        // New entry is retrievable.
        assertEquals(configB, ChannelConfigurationCache.get(urlA, brandB, channelB))
        // Old entry was evicted.
        assertNull(ChannelConfigurationCache.get(urlA, brandA, channelA))
    }

    @Test
    fun `clear makes a previously stored configuration unretrievable`() {
        ChannelConfigurationCache.put(urlA, brandA, channelA, configA, ChannelConfigurationCache.currentGeneration())
        ChannelConfigurationCache.clear()

        assertNull(ChannelConfigurationCache.get(urlA, brandA, channelA))
    }

    @Test
    fun `put after clear stores a new entry that is then retrievable`() {
        ChannelConfigurationCache.put(urlA, brandA, channelA, configA, ChannelConfigurationCache.currentGeneration())
        ChannelConfigurationCache.clear()
        ChannelConfigurationCache.put(urlA, brandB, channelB, configB, ChannelConfigurationCache.currentGeneration())

        assertEquals(configB, ChannelConfigurationCache.get(urlA, brandB, channelB))
    }

    @Test
    fun `get returns the stored configuration just before the 5-minute TTL elapses`() {
        ChannelConfigurationCache.put(urlA, brandA, channelA, configA, ChannelConfigurationCache.currentGeneration())
        fakeTimeSource.elapsed += 4.minutes + 59.seconds

        assertEquals(configA, ChannelConfigurationCache.get(urlA, brandA, channelA))
    }

    @Test
    fun `get returns null once the 5-minute TTL has elapsed`() {
        ChannelConfigurationCache.put(urlA, brandA, channelA, configA, ChannelConfigurationCache.currentGeneration())
        fakeTimeSource.elapsed += 5.minutes

        assertNull(ChannelConfigurationCache.get(urlA, brandA, channelA))
    }

    @Test
    fun `put after TTL expiry refreshes the entry so it is retrievable again`() {
        ChannelConfigurationCache.put(urlA, brandA, channelA, configA, ChannelConfigurationCache.currentGeneration())
        fakeTimeSource.elapsed += 5.minutes
        ChannelConfigurationCache.put(urlA, brandA, channelA, configB, ChannelConfigurationCache.currentGeneration())

        assertEquals(configB, ChannelConfigurationCache.get(urlA, brandA, channelA))
    }

    @Test
    fun `get with ignoreTtl still returns the entry after the TTL has elapsed`() {
        ChannelConfigurationCache.put(urlA, brandA, channelA, configA, ChannelConfigurationCache.currentGeneration())
        fakeTimeSource.elapsed += 5.minutes

        assertEquals(configA, ChannelConfigurationCache.get(urlA, brandA, channelA, ignoreTtl = true))
    }

    @Test
    fun `clear advances the current generation`() {
        val generationBeforeClear = ChannelConfigurationCache.currentGeneration()
        ChannelConfigurationCache.clear()

        assertNotEquals(generationBeforeClear, ChannelConfigurationCache.currentGeneration())
    }

    @Test
    fun `put silently skips a write started before an intervening clear`() {
        // Simulates a fetch that started, then signOut() (clear) ran before the fetch completed.
        val generationBeforeClear = ChannelConfigurationCache.currentGeneration()
        ChannelConfigurationCache.clear()

        ChannelConfigurationCache.put(urlA, brandA, channelA, configA, generationBeforeClear)

        assertNull(ChannelConfigurationCache.get(urlA, brandA, channelA))
    }

    @Test
    fun `put with the current generation after a clear still succeeds`() {
        ChannelConfigurationCache.clear()

        ChannelConfigurationCache.put(urlA, brandA, channelA, configA, ChannelConfigurationCache.currentGeneration())

        assertEquals(configA, ChannelConfigurationCache.get(urlA, brandA, channelA))
    }
}
