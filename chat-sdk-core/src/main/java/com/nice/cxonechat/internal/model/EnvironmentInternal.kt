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

import com.nice.cxonechat.state.Environment

internal data class EnvironmentInternal(
    override val name: String,
    override val location: String,
    override val baseUrl: String,
    override val socketUrl: String,
    override val originHeader: String,
    override val chatUrl: String,
) : Environment {

    override fun toString() = buildString {
        append("Environment(name='")
        append(name)
        append("', location='")
        append(location)
        append("', baseUrl='")
        append(baseUrl)
        append("', socketUrl='")
        append(socketUrl)
        append("', originHeader='")
        append(originHeader)
        append("', chatUrl='")
        append(chatUrl)
        append("')")
    }
}
