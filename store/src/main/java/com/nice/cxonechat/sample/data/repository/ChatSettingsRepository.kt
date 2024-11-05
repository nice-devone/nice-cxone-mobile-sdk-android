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
import androidx.annotation.Keep
import com.nice.cxonechat.sample.data.models.ChatSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.koin.core.annotation.Single

/**
 * Repository responsible saving, loading, and tracking chat related settings.
 *
 * @param context Application context for file access.
 */
@Single
class ChatSettingsRepository(
    val context: Context,
) : FileRepository<ChatSettings>(
    fileName = "settings.json",
    type = ChatSettings::class,
) {
    private val mutableSettings = MutableStateFlow<ChatSettings?>(null)

    /** Flow of current settings. */
    val settings = mutableSettings.asStateFlow()

    /**
     * Load saved settings.
     *
     * @return newly loaded settings.
     */
    @Keep // Remove once the  DE-117407 is resolved
    fun load() = super.load(context).also {
        mutableSettings.value = it
    }

    /** Clear current and saved settings. */
    fun clear() {
        super.clear(context)

        mutableSettings.value = null
    }

    /** Push and save new settings. */
    fun use(settings: ChatSettings) {
        mutableSettings.value = settings
        save(context, settings)
    }
}
