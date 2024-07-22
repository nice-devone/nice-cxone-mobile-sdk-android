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

package com.nice.cxonechat.thread

import com.nice.cxonechat.Public
import java.util.Date

/**
 * Represents all data about a single custom field. Please note that the
 * implementations that use this field can use distinctions by [id] to
 * pick only a singular value.
 *
 * Never create or pass same [id]s with different values if you don't
 * want the latest values, it can lead to unexpected consequences.
 *
 * Consult the documentation for given implementation.
 * */
@Public
interface CustomField {
    /**
     * Identifier or name of given property in custom field.
     * Consult a representative for more information.
     * */
    val id: String

    /**
     * Value for given identifier.
     * */
    val value: String

    /**
     * Timestamp when the instance was created (for locally created) or submitted to the backend.
     */
    val updatedAt: Date
}
