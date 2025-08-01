/*
 * Copyright (c) 2021-2025. NICE Ltd. All rights reserved.
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

package com.nice.cxonechat.model

import com.nice.cxonechat.internal.model.CustomFieldInternal
import com.nice.cxonechat.thread.CustomField
import com.nice.cxonechat.tool.nextString
import java.util.Date

internal fun makeCustomField(
    id: String = nextString(),
    value: String = nextString(),
    updatedAt: Date = Date(0),
) : CustomField = CustomFieldInternal(
    id = id,
    value = value,
    updatedAt = updatedAt,
)
