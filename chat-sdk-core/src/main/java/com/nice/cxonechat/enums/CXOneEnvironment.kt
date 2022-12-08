package com.nice.cxonechat.enums

import com.nice.cxonechat.Public
import com.nice.cxonechat.internal.model.EnvironmentInternal
import com.nice.cxonechat.state.Environment

@Public
enum class CXOneEnvironment(val value: Environment) {
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
