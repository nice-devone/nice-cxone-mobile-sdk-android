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

import com.nice.cxonechat.analytics.ActionMetadata
import com.nice.cxonechat.analytics.ActionMetadataInternal
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
internal data class ProactiveActionInfo(
    @SerialName("actionId")
    @Contextual
    val actionId: UUID,
    @SerialName("actionName")
    val actionName: String,
    @SerialName("actionType")
    val actionType: String,
) {

    constructor(
        metadata: ActionMetadataInternal,
    ) : this(
        metadata.id,
        metadata.name,
        metadata.type.value
    )

    companion object {

        operator fun invoke(metadata: ActionMetadata) = when (metadata) {
            is ActionMetadataInternal -> ProactiveActionInfo(metadata)
        }
    }
}
