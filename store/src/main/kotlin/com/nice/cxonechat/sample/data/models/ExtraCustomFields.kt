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

package com.nice.cxonechat.sample.data.models

import kotlinx.serialization.Serializable

/**
 * A data class representing extra custom fields for a customer and a contact.
 *
 * This class is serializable using Kotlin Serialization.
 *
 * @property customerCustomFields A map containing custom fields specific to the customer.
 *                                The key represents the field name, and the value represents the field value.
 *                                Defaults to an empty map.
 * @property contactCustomFields A map containing custom fields specific to the contact.
 *                               The key represents the field name, and the value represents the field value.
 *                               Defaults to an empty map.
 */
@Serializable
data class ExtraCustomFields(
    val customerCustomFields: Map<String, String> = emptyMap(),
    val contactCustomFields: Map<String, String> = emptyMap(),
)
