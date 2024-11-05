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

import com.nice.cxonechat.enums.EventType.TokenRefreshed
import com.nice.cxonechat.internal.socket.EventCallback.ReceivedEvent
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Event received when a token has been successfully refreshed. */
@Serializable
internal data class EventTokenRefreshed(
    @SerialName("postback")
    val postback: Postback<Data>,
) {

    val token get() = postback.data.accessToken.token
    val expiresAt get() = postback.data.accessToken.expiresAt

    @Serializable
    data class Data(
        @SerialName("accessToken")
        val accessToken: AccessToken,
    )

    companion object : ReceivedEvent<EventTokenRefreshed> {
        override val type = TokenRefreshed
    }
}
