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
import com.nice.cxonechat.enums.EventAction
import com.nice.cxonechat.enums.EventType.AuthorizeCustomer
import com.nice.cxonechat.state.Connection
import java.util.UUID

internal data class ActionAuthorizeCustomer(
    @SerializedName("action")
    val action: EventAction = EventAction.Register,
    @SerializedName("eventId")
    val eventId: UUID = UUID.randomUUID(),
    @SerializedName("payload")
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

    data class Data(
        @SerializedName("authorization")
        val authorization: OAuth,
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

    data class OAuth(
        @SerializedName("authorizationCode")
        val code: String?,
        @SerializedName("codeVerifier")
        val verifier: String?,
    )
}
