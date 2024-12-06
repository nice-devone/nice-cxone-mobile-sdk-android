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

package com.nice.cxonechat

import com.nice.cxonechat.core.BuildConfig
import com.nice.cxonechat.enums.CXOneEnvironment
import com.nice.cxonechat.state.Environment

/**
 * Configuration for the instance that will be invoked by the [ChatBuilder].
 */
@Public
interface SocketFactoryConfiguration {

    /**
     * Current environment to connect to.
     * Consult a representative to discover which is right for you.
     *
     * @see CXOneEnvironment
     */
    val environment: Environment

    /**
     * Brand id to connect under.
     * Consult a representative to discover your given brand id.
     *
     * It Should be a 4-digit number, but it can be updated in the future.
     */
    val brandId: Long

    /**
     * Channel id to connect under. Consult a representative to discover
     * your given channel id.
     */
    val channelId: String

    /**
     * Client version to be reported.
     * The library always supplies its own version, though you might be asked
     * (or willing to try) to change it in case of beta features, for example.
     */
    @Deprecated("This field is deprecated for public usage and will be removed from public API.")
    val version: String

    @Public
    companion object {

        /**
         * Helper method to create a new configuration.
         *
         * @see SocketFactoryConfiguration
         */
        @JvmStatic
        operator fun invoke(
            environment: Environment,
            brandId: Long,
            channelId: String,
        ) = create(environment, brandId, channelId)

        /**
         * Helper method to create a new configuration.
         *
         * @see SocketFactoryConfiguration
         */
        @JvmStatic
        @Deprecated("This method is deprecated for public usage and will be removed from public API.")
        @Suppress("DEPRECATION")
        operator fun invoke(
            environment: Environment,
            brandId: Long,
            channelId: String,
            version: String = BuildConfig.VERSION_NAME,
        ) = create(environment, brandId, channelId, version)

        /**
         * Helper method to create a new configuration.
         *
         * @see SocketFactoryConfiguration
         */
        @JvmStatic
        @Suppress("DEPRECATION")
        fun create(
            environment: Environment,
            brandId: Long,
            channelId: String,
        ): SocketFactoryConfiguration = create(environment, brandId, channelId, BuildConfig.VERSION_NAME)

        /**
         * Helper method to create a new configuration.
         *
         * @see SocketFactoryConfiguration
         */
        @JvmStatic
        @Suppress("DEPRECATION")
        @Deprecated("This method is deprecated for public usage and will be removed from public API.")
        fun create(
            environment: Environment,
            brandId: Long,
            channelId: String,
            version: String = BuildConfig.VERSION_NAME,
        ): SocketFactoryConfiguration = object : SocketFactoryConfiguration {
            override val environment = environment
            override val brandId = brandId
            override val channelId = channelId

            @Deprecated("This field is deprecated for public usage and will be removed from public API.")
            override val version = version
        }
    }
}
