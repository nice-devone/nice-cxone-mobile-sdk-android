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

package com.nice.cxonechat.sample.data.repository

import com.nice.cxonechat.sample.data.models.SdkEnvironment
import com.nice.cxonechat.sample.data.models.StoreEnvironment
import org.koin.core.annotation.Single

/**
 * The environments a launch intent is allowed to name.
 *
 * Exists so that where those environments come from is a single, swappable decision. The
 * implementation below reads them from a bundled asset which published copies of this repository do
 * not carry; in such a copy it degrades by itself to the built-in production environments, which is
 * the no-op variant -- no non-production hostname is disclosed, and a run that names one is told so.
 */
interface LaunchEnvironments {
    /** Names which can currently be resolved, for telling somebody what they could have used. */
    val availableNames: List<String>

    /**
     * Environment called [name], or null when this copy of the app has no definition for it.
     */
    fun byName(name: String): SdkEnvironment?
}

/**
 * Resolves launch environments from the built-in production set plus the `environment.json` asset.
 *
 * The asset is optional by construction: [AssetRepository] reports a missing file as no content, so
 * [SdkConfigurationListRepository] simply holds an empty list and only production remains nameable.
 *
 * @param sdkConfigurationListRepository Source of the bundled configurations, already loaded.
 */
@Single
class BundledLaunchEnvironments(
    private val sdkConfigurationListRepository: SdkConfigurationListRepository,
) : LaunchEnvironments {

    override val availableNames: List<String>
        get() = (StoreEnvironment.entries.map(StoreEnvironment::name) + bundled().map(SdkEnvironment::name))
            .distinct()

    override fun byName(name: String): SdkEnvironment? =
        StoreEnvironment.byName(name)?.sdkEnvironment ?: bundled().firstOrNull { it.name == name }

    private fun bundled() = sdkConfigurationListRepository.configurationList.value.map { it.environment }
}
