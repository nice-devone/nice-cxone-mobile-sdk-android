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

package com.nice.cxonechat.sample.data.models

import com.google.gson.annotations.SerializedName
import com.nice.cxonechat.state.Environment

/**
 * Serializable version [SdkEnvironment].
 */
data class SdkEnvironment(
    @SerializedName("name")
    override val name: String,
    @SerializedName("location")
    override val location: String,
    @SerializedName("baseUrl")
    override val baseUrl: String,
    @SerializedName("socketUrl")
    override val socketUrl: String,
    @SerializedName("originHeader")
    override val originHeader: String,
    @SerializedName("chatUrl")
    override val chatUrl: String,
) : Environment
