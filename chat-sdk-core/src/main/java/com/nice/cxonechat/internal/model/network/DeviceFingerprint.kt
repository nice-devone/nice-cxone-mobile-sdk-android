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

import android.os.Build
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.Locale

/** Represents fingerprint data about the customer. */
@Serializable
internal data class DeviceFingerprint(
    @SerialName("country")
    val country: String? = Locale.getDefault().country,

    /** Current IP Address. */
    @SerialName("ip")
    val ip: String? = null,

    @SerialName("language")
    val language: String? = Locale.getDefault().language,

    @SerialName("location")
    val location: String? = null,

    /** The type of application the customer is using (native or web app). */
    @SerialName("applicationType")
    val applicationType: String? = "native",

    /** The operating system the customer is currently using. */
    @SerialName("os")
    val os: String? = "Android",

    /** The operating system version that the customer is currently using. */
    @SerialName("osVersion")
    val osVersion: String? = Build.VERSION.RELEASE,

    /** The type of device that the customer is currently using. */
    @SerialName("deviceType")
    val deviceType: String? = "mobile",

    /** Token uniquely identifying this device. This defaults to null since it may be considered PII. */
    @SerialName("deviceToken")
    val deviceToken: String? = null,
)
