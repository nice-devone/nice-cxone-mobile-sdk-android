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

package com.nice.cxonechat.enums

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The list of all statuses on a contact.
 */
@Serializable
internal enum class ContactStatus(val value: String) {
    /** The contact is newly opened. */
    @SerialName("new")
    New("new"),

    /** The contact is currently open. */
    @SerialName("open")
    Open("open"),

    /** The contact is pending. */
    @SerialName("pending")
    Pending("pending"),

    /** The contact has been escalated. */
    @SerialName("escalated")
    Escalated("escalated"),

    /** The contact has been resolved. */
    @SerialName("resolved")
    Resolved("resolved"),

    /** The contact is closed. */
    @SerialName("closed")
    Closed("closed"),

    /** The contact contains some unknown status string. */
    Unknown("???")
}
