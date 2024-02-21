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

import com.nice.cxonechat.message.MessageMetadata
import com.nice.cxonechat.message.MessageStatus
import com.nice.cxonechat.message.MessageStatus.READ
import com.nice.cxonechat.message.MessageStatus.SEEN
import com.nice.cxonechat.message.MessageStatus.SENT
import java.util.Date

internal data class MessageMetadataInternal(
    override val seenAt: Date?,
    override val readAt: Date?,
) : MessageMetadata {

    @Transient
    override val status: MessageStatus = when {
        readAt != null -> READ
        seenAt != null -> SEEN
        else -> SENT
    }

    override fun toString(): String = "MessageMetadata(" +
            "seenAt=$seenAt, " +
            "readAt=$readAt, " +
            "status=$status" +
            ")"
}
