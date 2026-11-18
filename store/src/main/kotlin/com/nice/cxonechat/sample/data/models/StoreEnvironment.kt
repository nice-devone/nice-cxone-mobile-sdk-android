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

import com.nice.cxonechat.enums.CXoneEnvironment
import com.nice.cxonechat.state.Environment

/**
 * Environments a launch intent can name without anything being bundled with the app.
 *
 * **Only production belongs here.** These urls are already published as part of the SDK, so naming
 * them in source discloses nothing. Every other environment is looked up at runtime in the
 * `environment.json` asset, which is deliberately left out of published copies of this repository --
 * see [com.nice.cxonechat.sample.data.repository.LaunchEnvironments]. Do not add a non-production
 * environment here; it would be published along with the source.
 *
 * @property sdkEnvironment Environment definition to connect with.
 */
enum class StoreEnvironment(val sdkEnvironment: SdkEnvironment) {
    /** Production environment in North America. */
    PROD_NA1(CXoneEnvironment.NA1.value.toSdkEnvironment(name = "PROD_NA1")),

    /** Production environment in Europe. */
    PROD_EU1(CXoneEnvironment.EU1.value.toSdkEnvironment(name = "PROD_EU1")),
    ;

    @Suppress(
        "UndocumentedPublicClass", // Companion objects don't require documentation.
    )
    companion object {
        /** Environment name which signals that the definition is supplied on the intent instead. */
        const val CUSTOM = "CUSTOM"

        /**
         * Environment matching [name] exactly, or null if it is not one of the built-in ones.
         */
        fun byName(name: String): StoreEnvironment? = entries.firstOrNull { it.name == name }
    }
}

/**
 * Convert an SDK-supplied [Environment] into the serializable sample-app representation.
 *
 * @param name Name to store, defaulting to the environment's own. The sample app names environments
 * for the deployment (`PROD_NA1`) rather than the data center (`NA1`), so the entries above override
 * it while still sourcing their urls from [CXoneEnvironment].
 */
fun Environment.toSdkEnvironment(name: String = this.name) = SdkEnvironment(
    name = name,
    location = location,
    baseUrl = baseUrl,
    socketUrl = socketUrl,
    originHeader = originHeader,
    chatUrl = chatUrl,
    loggerUrl = loggerUrl,
    tokenUrl = tokenUrl,
)

/**
 * Build an environment from the host stem its urls are all derived from, e.g.
 * `de-na1.niceincontact.com`.
 *
 * Every environment the sample app connects to follows `https://<service>-<stem>`, so the stem is
 * the only part a launch intent actually has to carry. That keeps non-production hostnames in the
 * hands of whoever runs the test rather than in this repository.
 *
 * @param host Host stem, without any service prefix or scheme.
 */
fun sdkEnvironmentFromHost(host: String) = SdkEnvironment(
    name = host,
    location = "",
    baseUrl = "https://channels-$host/",
    socketUrl = "wss://chat-gateway-$host",
    originHeader = "https://livechat-$host",
    chatUrl = "https://channels-$host/chat/",
    loggerUrl = "https://app-$host/logger-public",
    tokenUrl = "https://digital-oauth-$host/",
)
