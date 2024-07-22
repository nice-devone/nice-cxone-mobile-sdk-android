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

import com.google.gson.annotations.SerializedName
import com.nice.cxonechat.enums.EventType.CustomerAuthorized
import com.nice.cxonechat.internal.model.CustomerIdentityModel
import com.nice.cxonechat.internal.socket.EventCallback.ReceivedEvent

/** Event received when a customer is successfully authorized. */
internal data class EventCustomerAuthorized(
    @SerializedName("postback")
    val postback: Postback<Data>,
) {

    val id get() = postback.data.consumerIdentity.idOnExternalPlatform
    val firstName get() = postback.data.consumerIdentity.firstName
    val lastName get() = postback.data.consumerIdentity.lastName

    val token get() = postback.data.accessToken?.token
    val tokenExpiresAt get() = postback.data.accessToken?.expiresAt

    data class Data(
        @SerializedName("consumerIdentity")
        val consumerIdentity: CustomerIdentityModel,
        @SerializedName("accessToken")
        val accessToken: AccessToken?,
    )

    companion object : ReceivedEvent<EventCustomerAuthorized> {
        override val type = CustomerAuthorized
    }
}
