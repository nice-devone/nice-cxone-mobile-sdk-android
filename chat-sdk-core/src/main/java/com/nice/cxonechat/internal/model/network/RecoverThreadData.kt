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

import com.nice.cxonechat.internal.model.network.RecoverThreadData.ThreadSpecification.EmptySpecification
import com.nice.cxonechat.internal.model.network.RecoverThreadData.ThreadSpecification.ThreadIdSpecification
import kotlinx.serialization.Contextual
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator
import java.util.UUID

@Serializable
internal data class RecoverThreadData(
    @SerialName("thread")
    val thread: ThreadSpecification,
) {

    @OptIn(ExperimentalSerializationApi::class)
    @Serializable
    @JsonClassDiscriminator("client_type") // Temporary solution to avoid possible conflict with the real type
    internal sealed interface ThreadSpecification {
        @Serializable
        @SerialName("thread_id") // Not required, but it hides internal class name
        data class ThreadIdSpecification(
            @SerialName("idOnExternalPlatform")
            @Contextual
            val idOnExternalPlatform: UUID,
        ) : ThreadSpecification

        @Serializable
        @SerialName("no_thread_id") // Not required, but it hides internal class name
        data object EmptySpecification : ThreadSpecification
    }

    constructor(threadId: UUID?) : this(threadId?.let(::ThreadIdSpecification) ?: EmptySpecification)
}
