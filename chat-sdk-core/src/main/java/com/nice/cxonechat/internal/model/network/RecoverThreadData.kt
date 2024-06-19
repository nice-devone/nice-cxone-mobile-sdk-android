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
import com.nice.cxonechat.internal.model.network.RecoverThreadData.ThreadSpecification.EmptySpecification
import com.nice.cxonechat.internal.model.network.RecoverThreadData.ThreadSpecification.ThreadIdSpecification
import java.util.UUID

internal data class RecoverThreadData(
    @SerializedName("thread")
    val thread: ThreadSpecification,
) {

    internal sealed interface ThreadSpecification {
        data class ThreadIdSpecification(
            @SerializedName("idOnExternalPlatform")
            val idOnExternalPlatform: UUID,
        ) : ThreadSpecification

        data object EmptySpecification : ThreadSpecification
    }

    constructor(threadId: UUID?) : this(threadId?.let(::ThreadIdSpecification) ?: EmptySpecification)
}
