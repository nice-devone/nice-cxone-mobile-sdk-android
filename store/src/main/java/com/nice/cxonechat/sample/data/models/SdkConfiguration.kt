/*
 * Copyright (c) 2021-2024. NICE Ltd. All rights reserved.
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

import com.nice.cxonechat.SocketFactoryConfiguration
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A savable configuration for the SDK.
 *
 * @param name Name of configuration with no particular significance, just something
 * nice to display.
 * @param environment sdk Environment details.
 * @param brandId Brand ID for configuration.
 * @param channelId Channel ID to configure.
 */
@Serializable
data class SdkConfiguration(
    @SerialName("name")
    val name: String,
    @SerialName("environment")
    val environment: SdkEnvironment,
    @SerialName("brandId")
    val brandId: Long,
    @SerialName("channelId")
    val channelId: String,
) {
    /**
     * Convert a saved SdkConfiguration to a SocketFactoryConfiguration for building a chat.
     *
     * @return Appropriately constructed SdkConfiguration.
     */
    val asSocketFactoryConfiguration: SocketFactoryConfiguration
        get() = SocketFactoryConfiguration(
            environment.asEnvironment,
            brandId,
            channelId
        )
}

/** Shorthand for a list of [SdkConfiguration]. */
typealias SdkConfigurations = List<SdkConfiguration>
