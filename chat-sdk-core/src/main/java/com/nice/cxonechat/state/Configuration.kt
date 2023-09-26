/*
 * Copyright (c) 2021-2023. NICE Ltd. All rights reserved.
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

package com.nice.cxonechat.state

import com.nice.cxonechat.Public

/**
 * The various options for how a channel is configured.
 */
@Public
interface Configuration {

    /** Whether the channel supports multiple threads for the same user. */
    val hasMultipleThreadsPerEndUser: Boolean

    /** Whether the channel supports proactive chat features. */
    val isProactiveChatEnabled: Boolean

    /** Whether OAuth authorization is enabled for the channel. */
    val isAuthorizationEnabled: Boolean

    /**
     * Custom fields defined for supplying of additional information about customer,
     * for example data supplied during a pre-chat survey.
     */
    val contactCustomFields: FieldDefinitionList

    /**
     * Definition of possible custom fields which are usable/valid for all
     * contacts with the customer.
     */
    val customerCustomFields: FieldDefinitionList

    /** Return the list of all available customer fields. */
    val allCustomFields: FieldDefinitionList
        get() = contactCustomFields + customerCustomFields

    /**
     * Check if a given field ID is allowed by the receiving [Configuration].
     *
     * @param fieldId Field ID to check for validity.
     * @return Returns true iff [fieldId] is valid with the current configuration, i.e.,
     * is included in either [contactCustomFields] or [customerCustomFields].
     */
    fun allowsFieldId(fieldId: String): Boolean =
        allCustomFields.containsField(fieldId)
}
