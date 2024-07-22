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

import com.google.gson.annotations.SerializedName
import com.nice.cxonechat.SocketFactoryConfiguration

/**
 * A savable configuration for the SDK.
 *
 * @param name Name of configuration with no particular significance, just something
 * nice to display.
 * @param environment sdk Environment details.
 * @param brandId Brand ID for configuration.
 * @param channelId Channel ID to configure.
 */
data class SdkConfiguration(
    @SerializedName("name")
    val name: String,
    @SerializedName("environment")
    val environment: SdkEnvironment,
    @SerializedName("brandId")
    val brandId: Long,
    @SerializedName("channelId")
    val channelId: String,
) {
    /**
     * Convert a saved SdkConfiguration to a SocketFactoryConfiguration for building a chat.
     *
     * @param context Android context, used to fetch the version name.
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
