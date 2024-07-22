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

package com.nice.cxonechat.sample.data.repository

import android.content.Context
import androidx.compose.runtime.Stable
import com.nice.cxonechat.sample.data.models.SdkConfigurationList
import com.nice.cxonechat.sample.data.models.SdkConfigurations
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.koin.core.annotation.Single

/**
 * Repository to read the SdkConfigurationList from assets.
 *
 * @param context Application context to access assets.
 */
@Single
class SdkConfigurationListRepository(
    val context: Context,
) : AssetRepository<SdkConfigurationList>(
    name = "environment.json",
    type = SdkConfigurationList::class,
) {
    private val configurationListStore = MutableStateFlow<SdkConfigurations>(emptyList())

    /** Predefined configurations from which we can choose. */
    @Stable
    val configurationList = configurationListStore.asStateFlow()

    /**
     * Load the configuration list from assets or from cache.
     *
     * @return Returns a list of predefined Sdk configurations.
     */
    @Stable
    fun load(): SdkConfigurations {
        return configurationList.value.ifEmpty {
            val configurations = super.load(context)
                ?.configurations
                ?: emptyList()
            configurationListStore.value = configurations
            configurations
        }
    }
}
