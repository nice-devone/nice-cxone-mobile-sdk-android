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

package com.nice.cxonechat.internal.model

import com.nice.cxonechat.internal.model.network.MessagePolyElement
import com.nice.cxonechat.message.PluginElement.Countdown
import com.nice.cxonechat.util.plus
import java.util.Date
import kotlin.time.Duration.Companion.seconds as duration

internal data class PluginElementCountdown(
    private val element: MessagePolyElement.Countdown,
) : Countdown() {

    override val endsAt: Date
        get() = element.variables.startedAt + element.variables.seconds.duration.inWholeMilliseconds

    override val isExpired: Boolean
        get() = Date().after(endsAt)

    override fun toString() = buildString {
        append("PluginElement.CountDown(endsAt=")
        append(endsAt)
        append(", isExpired=")
        append(isExpired)
        append(")")
    }
}
