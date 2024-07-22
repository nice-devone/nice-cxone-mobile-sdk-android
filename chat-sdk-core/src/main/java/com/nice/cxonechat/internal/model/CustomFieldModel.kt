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

package com.nice.cxonechat.internal.model

import com.google.gson.annotations.SerializedName
import com.nice.cxonechat.thread.CustomField
import java.util.Date

internal data class CustomFieldModel(
    @SerializedName("ident")
    val id: String,
    @SerializedName("value")
    val value: String,
    @SerializedName("updatedAt")
    val updatedAt: Date,
) {

    constructor(
        entry: Map.Entry<String, String>,
    ) : this(
        id = entry.key,
        value = entry.value,
        updatedAt = Date(),
    )

    constructor(
        field: CustomField,
    ) : this(
        id = field.id,
        value = field.value,
        updatedAt = field.updatedAt
    )

    fun toCustomField() = CustomFieldInternal(
        id = id,
        value = value,
        updatedAt = updatedAt
    )
}
