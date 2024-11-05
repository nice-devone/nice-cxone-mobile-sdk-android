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

import com.nice.cxonechat.core.BuildConfig
import com.nice.cxonechat.enums.EventAction
import com.nice.cxonechat.enums.EventType.AuthorizeCustomer
import com.nice.cxonechat.state.Connection
import com.nice.cxonechat.util.UUIDProvider
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
internal data class ActionAuthorizeCustomer(
    @SerialName("action")
    val action: EventAction = EventAction.Register,
    @SerialName("eventId")
    @Contextual
    val eventId: UUID = UUIDProvider.next(),
    @SerialName("payload")
    val payload: Payload<Data>,
) {

    constructor(
        connection: Connection,
        code: String,
        verifier: String,
    ) : this(
        payload = Payload(
            eventType = AuthorizeCustomer,
            connection = connection,
            data = Data(code = code, verifier = verifier)
        )
    )

    @Serializable
    data class Data(
        @SerialName("authorization")
        val authorization: OAuth,
        @SerialName("disableChannelInfo")
        val disableChannelInfo: Boolean = true,
        @SerialName("sdkPlatform")
        val platform: String = "android",
        @SerialName("sdkVersion")
        val version: String = BuildConfig.VERSION_NAME,
    ) {

        constructor(
            code: String,
            verifier: String,
        ) : this(
            OAuth(
                code = code.ifBlank { null },
                verifier = verifier.ifBlank { null }
            )
        )
    }

    @Serializable
    data class OAuth(
        @SerialName("authorizationCode")
        val code: String?,
        @SerialName("codeVerifier")
        val verifier: String?,
    )
}
