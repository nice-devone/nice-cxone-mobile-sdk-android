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

import com.nice.cxonechat.enums.CXoneEnvironment
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

/**
 * Verifies the value-equality semantics of [SocketFactoryConfiguration].
 *
 * The underlying implementation is a data class; the CHANGELOG explicitly calls out that
 * "Default SocketFactoryConfiguration implementation is now a data class with value equality,
 * which may cause issues if users rely on previous reference only equality."
 * These tests guard that contract.
 */
internal class SocketFactoryConfigurationTest {

    private val environment = CXoneEnvironment.EU1.value

    @Test
    fun `two instances with identical parameters are equal`() {
        val a = SocketFactoryConfiguration(environment, brandId = 1234L, channelId = "channel-abc")
        val b = SocketFactoryConfiguration(environment, brandId = 1234L, channelId = "channel-abc")

        assertEquals(a, b)
    }

    @Test
    fun `two instances with identical parameters have the same hash code`() {
        val a = SocketFactoryConfiguration(environment, brandId = 1234L, channelId = "channel-abc")
        val b = SocketFactoryConfiguration(environment, brandId = 1234L, channelId = "channel-abc")

        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun `instances with different brandId are not equal`() {
        val a = SocketFactoryConfiguration(environment, brandId = 1234L, channelId = "channel-abc")
        val b = SocketFactoryConfiguration(environment, brandId = 9999L, channelId = "channel-abc")

        assertNotEquals(a, b)
    }

    @Test
    fun `instances with different channelId are not equal`() {
        val a = SocketFactoryConfiguration(environment, brandId = 1234L, channelId = "channel-abc")
        val b = SocketFactoryConfiguration(environment, brandId = 1234L, channelId = "channel-xyz")

        assertNotEquals(a, b)
    }

    @Test
    fun `instances with different environments are not equal`() {
        val a = SocketFactoryConfiguration(CXoneEnvironment.EU1.value, brandId = 1234L, channelId = "channel-abc")
        val b = SocketFactoryConfiguration(CXoneEnvironment.NA1.value, brandId = 1234L, channelId = "channel-abc")

        assertNotEquals(a, b)
    }

    @Test
    fun `create factory method produces equal instances for same parameters`() {
        val a = SocketFactoryConfiguration.create(environment, brandId = 1234L, channelId = "channel-abc")
        val b = SocketFactoryConfiguration.create(environment, brandId = 1234L, channelId = "channel-abc")

        assertEquals(a, b)
    }

    @Test
    fun `invoke operator and create produce equal instances`() {
        val a = SocketFactoryConfiguration(environment, brandId = 1234L, channelId = "channel-abc")
        val b = SocketFactoryConfiguration.create(environment, brandId = 1234L, channelId = "channel-abc")

        assertEquals(a, b)
    }
}
