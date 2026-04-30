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

package com.nice.cxonechat.message

import com.nice.cxonechat.Public
import java.util.Date

/**
 * Time slot represents a time interval with a specific duration and start time. It allows the user to choose
 * a time slot for scheduling a message or an appointment.
 */
@Public
interface TimeSlot {
    /** Unique identifier of the time slot. */
    val id: String

    /** Duration of the time slot in seconds. */
    val duration: Long

    /** Start time of the time slot. */
    val startTime: Date
}
