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
import com.nice.cxonechat.enums.ContactStatus
import java.util.Date
import java.util.UUID

// ContactView

/**
 * Represents all info about a contact (case).
 */

internal data class Contact constructor(
    /** The id of the contact. */
    @SerializedName("id")
    val id: String,

    /** The id of the thread for which this contact applies. */
    @SerializedName("threadIdOnExternalPlatform")
    val threadIdOnExternalPlatform: UUID,

    @SerializedName("status")
    val status: ContactStatus,

    @SerializedName("createdAt")
    val createdAt: Date,
)
