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

package com.nice.cxonechat.internal.model.network

import com.google.gson.annotations.SerializedName

private val RELEASE: String? = android.os.Build.VERSION.RELEASE

/** Represents fingerprint data about the customer. */
internal data class DeviceFingerprint(
    @SerializedName("browser")
    val browser: String = "",
    @SerializedName("browserVersion")
    val browserVersion: String = "",
    @SerializedName("country")
    val country: String = "",
    @SerializedName("ip")
    val ip: String = "",
    @SerializedName("language")
    val language: String = "",
    @SerializedName("location")
    val location: String = "",

    /** The type of application the customer is using (native or web app). */
    @SerializedName("applicationType")
    private val applicationType: String = "native",

    /** The operating system the customer is currently using. */
    @SerializedName("os")
    private val os: String = "Android",

    /** The operating system version that the customer is currently using. */
    @SerializedName("osVersion")
    private val osVersion: String = RELEASE ?: "Unknown",

    /** The type of device that the customer is currently using. */
    @SerializedName("deviceType")
    val deviceType: String = "mobile",
    @SerializedName("deviceToken")
    val deviceToken: String = "",
)
