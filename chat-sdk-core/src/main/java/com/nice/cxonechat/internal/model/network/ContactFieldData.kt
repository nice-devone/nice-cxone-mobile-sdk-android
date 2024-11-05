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

package com.nice.cxonechat.internal.model.network

import com.nice.cxonechat.enums.ContactStatus
import com.nice.cxonechat.internal.model.CustomFieldModel
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.Date
import java.util.UUID

@Serializable
internal data class ContactFieldData(
    @SerialName("id")
    val id: String,
    @SerialName("customFields")
    val customFields: List<CustomFieldModel>,
    /** The id of the thread for which this contact applies. */
    @SerialName("threadIdOnExternalPlatform")
    @Contextual
    val threadIdOnExternalPlatform: UUID,

    @SerialName("status")
    val status: ContactStatus,

    @SerialName("createdAt")
    @Contextual
    val createdAt: Date,
)
