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

import com.nice.cxonechat.Public
import com.nice.cxonechat.internal.model.EnvironmentInternal
import com.nice.cxonechat.state.Environment

/**
 * Enumeration of standard production environments, based on the data-center locations.
 *
 * @property value [Environment] instance preset to corresponding location.
 */
@Public
enum class CXOneEnvironment(val value: Environment) {
    /**
     * Location North America - USA.
     */
    NA1(
        EnvironmentInternal(
            name = "NA1",
            location = "North America",
            baseUrl = "https://channels-de-na1.niceincontact.com/",
            socketUrl = "wss://chat-gateway-de-na1.niceincontact.com",
            originHeader = "https://livechat-de-na1.niceincontact.com",
            chatUrl = "https://channels-de-na1.niceincontact.com/chat/"
        )
    ),

    /**
     * Location Europe.
     */
    EU1(
        EnvironmentInternal(
            name = "EU1",
            location = "Europe",
            baseUrl = "https://channels-de-eu1.niceincontact.com/",
            socketUrl = "wss://chat-gateway-de-eu1.niceincontact.com",
            originHeader = "https://livechat-de-eu1.niceincontact.com",
            chatUrl = "https://channels-de-eu1.niceincontact.com/chat/"
        )
    ),

    /**
     * Location Australia.
     */
    AU1(
        EnvironmentInternal(
            name = "AU1",
            location = "Australia",
            baseUrl = "https://channels-de-au1.niceincontact.com/",
            socketUrl = "wss://chat-gateway-de-au1.niceincontact.com",
            originHeader = "https://livechat-de-au1.niceincontact.com",
            chatUrl = "https://channels-de-au1.niceincontact.com/chat/"
        )
    ),

    /**
     * Location Canada.
     */
    CA1(
        EnvironmentInternal(
            name = "CA1",
            location = "Canada",
            baseUrl = "https://channels-de-ca1.niceincontact.com/",
            socketUrl = "wss://chat-gateway-de-ca1.niceincontact.com",
            originHeader = "https://livechat-de-ca1.niceincontact.com",
            chatUrl = "https://channels-de-ca1.niceincontact.com/chat/"
        )
    ),

    /**
     * Location United Kingdom.
     */
    UK1(
        EnvironmentInternal(
            name = "UK1",
            location = "United Kingdom",
            baseUrl = "https://channels-de-uk1.niceincontact.com/",
            socketUrl = "wss://chat-gateway-de-uk1.niceincontact.com",
            originHeader = "https://livechat-de-uk1.niceincontact.com",
            chatUrl = "https://channels-de-uk1.niceincontact.com/chat/"
        )
    ),

    /**
     * Location Japan.
     */
    JP1(
        EnvironmentInternal(
            name = "JP1",
            location = "Japan",
            baseUrl = "https://channels-de-jp1.niceincontact.com/",
            socketUrl = "wss://chat-gateway-de-jp1.niceincontact.com",
            originHeader = "https://livechat-de-jp1.niceincontact.com",
            chatUrl = "https://channels-de-jp1.niceincontact.com/chat/"
        )
    ),
}
