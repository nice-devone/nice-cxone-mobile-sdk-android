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

import com.nice.cxonechat.ui.storage.ValueStorage.StringKey.CustomerCustomValuesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.serialization.json.Json

internal suspend fun ValueStorage.getCustomerCustomValues(): Map<String, String> {
    val json = getString(CustomerCustomValuesKey).firstNotBlankOrNull()
    return if (json != null) {
        Json.Default.decodeFromString<Map<String, String>>(json)
    } else {
        emptyMap()
    }
}

private suspend inline fun Flow<String>.firstNotBlankOrNull(): String? = firstOrNull()?.takeIf(String::isNotBlank)
