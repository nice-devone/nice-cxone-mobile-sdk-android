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

package com.nice.cxonechat.ui.api

import com.nice.cxonechat.Public

/**
 * Interface for providing custom fields for contact and customer contexts.
 *
 * This interface is used to retrieve extra custom fields that won't be displayed in the UI, but will be appended
 * to the contact and customer contexts when they are set and sent to the backend.
 *
 * The default internal implementation returns empty maps.
 *
 * Integration is responsible for providing only valid custom fields key&value pairs, invalid values will cause
 * runtime errors reported by the backend and the SDK.
 *
 * The fields will be queried from the background thread.
 */
@Public
fun interface UiCustomFieldsProvider {

    /** Definition of additional custom fields. */
    fun customFields(): Map<String, String>
}

/** The default implementation of [UiCustomFieldsProvider] that returns empty collections. */
@Public
object NoExtraCustomFields : UiCustomFieldsProvider {
    override fun customFields(): Map<String, String> = emptyMap()
}

/** Definition of the possible uses for extra custom field providers. */
@Public
enum class CustomFieldProviderType {
    /**
     * Custom fields for the contact context - these fields will be sent to the backend when the contact (thread) is
     * created in this instance of the SDK client or if the contact custom fields are updated in the UI by the user.
     * The extra custom fields will be appended to the existing map, overwriting any existing values for matching keys.
     */
    Contact,

    /**
     * Custom fields for the customer context - these fields will be sent to the backend each time the customer connects
     * to the chat backend.
     * The extra custom fields will be appended to the existing map, overwriting any existing values for matching keys.
     */
    Customer
}
