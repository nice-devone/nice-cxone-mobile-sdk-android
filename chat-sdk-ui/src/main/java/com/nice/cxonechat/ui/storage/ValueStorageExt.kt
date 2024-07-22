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

package com.nice.cxonechat.ui.storage

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.nice.cxonechat.ui.storage.ValueStorage.StringKey.CustomerCustomValuesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

internal suspend fun ValueStorage.getCustomerCustomValues(): Map<String, String> {
    @Suppress("UNCHECKED_CAST")
    val parameterized = TypeToken.getParameterized(
        Map::class.java,
        String::class.java,
        String::class.java
    ) as? TypeToken<Map<String, String>>?
    val json = getString(CustomerCustomValuesKey).firstNotBlankOrNull()
    return if (json != null) {
        Gson().fromJson(json, parameterized) ?: emptyMap()
    } else {
        emptyMap()
    }
}

private suspend inline fun Flow<String>.firstNotBlankOrNull(): String? = firstOrNull()?.takeIf(String::isNotBlank)
