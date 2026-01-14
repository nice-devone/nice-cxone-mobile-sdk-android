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
import com.nice.cxonechat.sample.data.models.ExtraCustomFields
import kotlinx.coroutines.CoroutineScope
import org.koin.core.annotation.Single

/**
 * Repository to read/write the extra custom fields from/to file.
 */
@Single
class ExtraCustomFieldRepository(
    private val context: Context,
    applicationScope: CoroutineScope,
) : FileRepository<ExtraCustomFields>(
    fileName = "extra_custom_fields.json",
    type = ExtraCustomFields::class,
    applicationScope,
) {
    /**
     * Load the extra custom fields from assets or from cache.
     *
     * @return Returns a list of extra custom fields.
     */
    suspend fun load(): ExtraCustomFields = super.load(context) ?: ExtraCustomFields()
}
