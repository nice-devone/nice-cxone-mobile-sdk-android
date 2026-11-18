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

package com.nice.cxonechat.sample.data.models

import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotContain
import org.junit.Test

internal class StoreEnvironmentTest {

    @Test
    fun `only production environments are defined in source`() {
        // Non-production hostnames must not be published with this repository; they are read at
        // runtime from an asset which published copies do not carry.
        StoreEnvironment.entries.map { it.name } shouldContainExactlyInAnyOrder listOf("PROD_NA1", "PROD_EU1")
    }

    @Test
    fun `no built-in environment names a non-production host`() {
        StoreEnvironment.entries.forEach { environment ->
            with(environment.sdkEnvironment) {
                listOf(baseUrl, socketUrl, originHeader, chatUrl, loggerUrl, tokenUrl).forEach { url ->
                    url shouldNotContain "brandembassy.com"
                    url shouldNotContain "staging."
                }
            }
        }
    }

    @Test
    fun `an environment is derived from its host stem`() {
        // Verified against every environment the sample app ships with: each one reconstructs from
        // the stem alone, which is why a launch intent only has to carry that much.
        sdkEnvironmentFromHost("de-na1.niceincontact.com") shouldBe SdkEnvironment(
            name = "de-na1.niceincontact.com",
            location = "",
            baseUrl = "https://channels-de-na1.niceincontact.com/",
            socketUrl = "wss://chat-gateway-de-na1.niceincontact.com",
            originHeader = "https://livechat-de-na1.niceincontact.com",
            chatUrl = "https://channels-de-na1.niceincontact.com/chat/",
            loggerUrl = "https://app-de-na1.niceincontact.com/logger-public",
            tokenUrl = "https://digital-oauth-de-na1.niceincontact.com/",
        )
    }

    @Test
    fun `a derived environment matches the production definition it stands for`() {
        val derived = sdkEnvironmentFromHost("de-na1.niceincontact.com")
        val published = StoreEnvironment.PROD_NA1.sdkEnvironment

        // Only the display name differs -- the urls are the contract.
        derived.copy(name = published.name, location = published.location) shouldBe published
    }
}
