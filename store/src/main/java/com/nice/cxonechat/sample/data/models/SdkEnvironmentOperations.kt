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

import com.nice.cxonechat.enums.CXOneEnvironment
import com.nice.cxonechat.state.Environment

/** return the receiver as an cxonechat environment. */
val SdkEnvironment.asEnvironment: Environment
    get() = this

/** return the receiver as an SdkEnvironment. */
val CXOneEnvironment.asSdkEnvironment: SdkEnvironment
    get() = SdkEnvironment(
        name,
        value.location,
        value.baseUrl,
        value.socketUrl,
        value.originHeader,
        value.chatUrl
    )
