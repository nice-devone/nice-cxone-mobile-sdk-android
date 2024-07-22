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

package com.nice.cxonechat.state

import com.nice.cxonechat.Public

/**
 * Current SDK environment providing backend (remote) configuration.
 * This can differ from region to region and permits connecting to your
 * own running copy of the service.
 * */
@Public
interface Environment {
    /**
     * Name of the environment. It's defined only for semantic reasons.
     */
    val name: String

    /**
     * Physical location of the environment that we'll connect to.
     */
    val location: String

    /**
     * Url used for fetching base configuration, such as: multi/single
     * thread, auth.
     */
    val baseUrl: String

    /**
     * Socket url used for chat communication.
     */
    val socketUrl: String

    /**
     * Origin header required for live chat.
     */
    val originHeader: String

    /**
     * Chat sub-url. Usually defined as `/chat` suffix to [baseUrl]
     */
    val chatUrl: String
}
