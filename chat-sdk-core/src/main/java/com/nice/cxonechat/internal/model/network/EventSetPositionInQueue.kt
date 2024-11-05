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

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class EventSetPositionInQueue(
    @SerialName("data")
    val data: Data
) {
    val consumerContact get() = data.consumerContact.id
    val routingQueue get() = data.routingQueue.id
    val positionInQueue get() = data.positionInQueue
    val hasOnlineAgent get() = data.isAnyAgentOnlineForQueue

    @Serializable
    data class Data(
        @SerialName("consumerContact")
        val consumerContact: Identifier,
        @SerialName("routingQueue")
        val routingQueue: RoutingQueue,
        @SerialName("positionInQueue")
        val positionInQueue: Int,
        @SerialName("isAnyAgentOnlineForQueue")
        val isAnyAgentOnlineForQueue: Boolean,
    ) {
        @Serializable
        data class RoutingQueue(
            @SerialName("id")
            val id: String,
        )
    }
}
