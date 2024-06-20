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
import com.nice.cxonechat.internal.model.Contact

internal data class EventSetPositionInQueue(
    @SerializedName("data")
    val data: Data
) {
    val consumerContact get() = data.consumerContact.id
    val routingQueue get() = data.routingQueue.id
    val positionInQueue get() = data.positionInQueue
    val hasOnlineAgent get() = data.isAnyAgentOnlineForQueue

    data class Data(
        @SerializedName("consumerContact")
        val consumerContact: Contact,
        @SerializedName("routingQueue")
        val routingQueue: RoutingQueue,
        @SerializedName("positionInQueue")
        val positionInQueue: Int,
        @SerializedName("isAnyAgentOnlineForQueue")
        val isAnyAgentOnlineForQueue: Boolean,
    ) {
        data class RoutingQueue(
            @SerializedName("id")
            val id: String,
        )
    }
}
