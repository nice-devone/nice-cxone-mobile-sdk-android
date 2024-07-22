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

package com.nice.cxonechat.internal.model

import com.nice.cxonechat.message.MessageMetadata
import com.nice.cxonechat.message.MessageStatus
import com.nice.cxonechat.message.MessageStatus.Read
import com.nice.cxonechat.message.MessageStatus.Seen
import com.nice.cxonechat.message.MessageStatus.Sent
import java.util.Date

internal data class MessageMetadataInternal(
    override val seenAt: Date?,
    override val readAt: Date?,
) : MessageMetadata {

    @Transient
    override val status: MessageStatus = when {
        readAt != null -> Read
        seenAt != null -> Seen
        else -> Sent
    }

    override fun toString(): String = "MessageMetadata(" +
            "seenAt=$seenAt, " +
            "readAt=$readAt, " +
            "status=$status" +
            ")"
}
