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

import com.nice.cxonechat.event.thread.ChatThreadEvent
import com.nice.cxonechat.event.thread.TimeSlotEvent
import com.nice.cxonechat.message.TimeSlot

/**
 * Converts this [TimeSlot] to a [ChatThreadEvent] that can be sent to the server as a postback reply.
 *
 * The resulting event carries the localized [text] the user saw and the slot's [TimeSlot.id] as
 * the postback value, which the server uses to identify which slot was selected.
 *
 * @param text The human-readable label representing this slot as presented to the user.
 */
internal fun TimeSlot.toEvent(text: String): ChatThreadEvent {
    return TimeSlotEvent(
        time = text,
        postback = id,
    )
}
