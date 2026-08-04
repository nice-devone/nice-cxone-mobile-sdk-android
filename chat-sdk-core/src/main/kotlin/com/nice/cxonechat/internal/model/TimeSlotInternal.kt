/*
 * Copyright (c) 2021-2026. NICE Ltd. All rights reserved.
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

package com.nice.cxonechat.internal.model

import com.nice.cxonechat.internal.model.network.TimeSlotModel
import com.nice.cxonechat.message.TimeSlot
import kotlin.time.Instant

/**
 * Internal implementation of [TimeSlot] used within the SDK.
 *
 * Wraps the network-layer [TimeSlotModel] for use in message models and event construction.
 */
internal data class TimeSlotInternal(
    override val id: String,
    override val duration: Long,
    override val startTime: Instant,
) : TimeSlot {
    constructor(model: TimeSlotModel) : this(model.id, model.duration, model.startTime)

    override fun toString() = buildString {
        append("TimeSlot(")
        append("id='$id', ")
        append("duration=$duration, ")
        append("startTime=$startTime")
        append(")")
    }
}
