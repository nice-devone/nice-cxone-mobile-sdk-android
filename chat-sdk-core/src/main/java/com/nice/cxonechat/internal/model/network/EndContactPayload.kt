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

import com.nice.cxonechat.internal.model.Thread
import com.nice.cxonechat.thread.ChatThread
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class EndContactPayload(
    @SerialName("thread")
    val thread: Thread,
    @SerialName("contact")
    val contact: Identifier?,
) {
    constructor(thread: ChatThread) : this(
        thread = Thread(thread),
        contact = thread.contactId?.let(::Identifier)
    )
}
