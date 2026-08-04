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
import kotlin.test.assertNull

/**
 * Tests for [ChannelConfigurationCache].
 *
 * The cache is a process-scoped singleton that stores exactly one entry at a time.
 * Cross-brand and cross-channel isolation are critical: returning a configuration for
 * the wrong brand or channel would expose incorrect channel settings to an SDK session.
 */
internal class ChannelConfigurationCacheTest {

    private val brandA = 1001
    private val brandB = 1002
    private val channelA = "channel-alpha"
    private val channelB = "channel-beta"

    private val configA = mockk<ChannelConfiguration>()
    private val configB = mockk<ChannelConfiguration>()

    @Before
    fun setUp() {
        ChannelConfigurationCache.clear()
    }

    @After
    fun tearDown() {
        ChannelConfigurationCache.clear()
    }

    @Test
    fun `get returns null when cache is empty`() {
        assertNull(ChannelConfigurationCache.get(brandA, channelA))
    }

    @Test
    fun `put then get returns the stored configuration for the exact same key`() {
        ChannelConfigurationCache.put(brandA, channelA, configA)

        assertEquals(configA, ChannelConfigurationCache.get(brandA, channelA))
    }

    @Test
    fun `get returns null for a different brand ID with the same channel`() {
        ChannelConfigurationCache.put(brandA, channelA, configA)

        assertNull(ChannelConfigurationCache.get(brandB, channelA))
    }

    @Test
    fun `get returns null for a different channel ID with the same brand`() {
        ChannelConfigurationCache.put(brandA, channelA, configA)

        assertNull(ChannelConfigurationCache.get(brandA, channelB))
    }

    @Test
    fun `get returns null when both brand and channel differ`() {
        ChannelConfigurationCache.put(brandA, channelA, configA)

        assertNull(ChannelConfigurationCache.get(brandB, channelB))
    }

    @Test
    fun `put overwrites the previous entry so only the latest key is retrievable`() {
        ChannelConfigurationCache.put(brandA, channelA, configA)
        ChannelConfigurationCache.put(brandB, channelB, configB)

        // New entry is retrievable.
        assertEquals(configB, ChannelConfigurationCache.get(brandB, channelB))
        // Old entry was evicted.
        assertNull(ChannelConfigurationCache.get(brandA, channelA))
    }

    @Test
    fun `clear makes a previously stored configuration unretrievable`() {
        ChannelConfigurationCache.put(brandA, channelA, configA)
        ChannelConfigurationCache.clear()

        assertNull(ChannelConfigurationCache.get(brandA, channelA))
    }

    @Test
    fun `put after clear stores a new entry that is then retrievable`() {
        ChannelConfigurationCache.put(brandA, channelA, configA)
        ChannelConfigurationCache.clear()
        ChannelConfigurationCache.put(brandB, channelB, configB)

        assertEquals(configB, ChannelConfigurationCache.get(brandB, channelB))
    }
}
