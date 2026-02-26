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

import android.content.Context
import com.nice.cxonechat.log.Logger
import com.nice.cxonechat.log.LoggerAndroid
import com.nice.cxonechat.log.LoggerScope
import com.nice.cxonechat.log.scope
import com.nice.cxonechat.log.verbose
import com.nice.cxonechat.sample.data.models.ChatSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.koin.core.annotation.Single

/**
 * Repository responsible saving, loading, and tracking chat related settings.
 *
 * @param context Application context for file access.
 * @param applicationScope CoroutineScope for file access.
 * @param logger Logger instance, defaulting to [LoggerAndroid].
 */
@Single
class ChatSettingsRepository(
    val context: Context,
    applicationScope: CoroutineScope,
    logger: Logger = LoggerAndroid("SampleApp"),
) : LoggerScope by LoggerScope("ChatSettingsRepository", logger),
    FileRepository<ChatSettings>(
        fileName = "settings.json",
        type = ChatSettings::class,
        applicationScope,
    ) {
    private val mutableSettings = MutableStateFlow<ChatSettings?>(null)

    /** Flow of current settings. */
    val settings = mutableSettings.asStateFlow()

    /**
     * Load saved settings.
     *
     * @return newly loaded settings.
     */
    suspend fun load(): ChatSettings? = scope("load") {
        verbose("Loading settings from storage.")
        return super.load(context).also {
            mutableSettings.value = it
            verbose("Settings loaded: $it")
        }
    }

    /** Clear current and saved settings. */
    fun clear() = scope("clear") {
        super.clear(context)
        mutableSettings.value = null
        verbose("Settings cleared.")
    }

    /** Push and save new settings. */
    fun use(settings: ChatSettings) = scope("use") {
        mutableSettings.value = settings
        save(context, settings)
        verbose("Settings saved: $settings")
    }
}
