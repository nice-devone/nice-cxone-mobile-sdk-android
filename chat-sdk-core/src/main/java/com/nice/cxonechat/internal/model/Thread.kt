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

import com.google.gson.annotations.SerializedName
import com.nice.cxonechat.thread.ChatThread
import java.util.UUID

// ThreadView

/** Represents info about a thread from the socket. */
internal data class Thread(
    /** The unique id for the thread. */
    @SerializedName("idOnExternalPlatform")
    val idOnExternalPlatform: UUID,

    /** The name given to the thread (for multi-chat channels only). */
    @SerializedName("threadName")
    val threadName: String? = null,
) {

    constructor(
        thread: ChatThread,
    ) : this(
        idOnExternalPlatform = thread.id,
        threadName = thread.threadName
    )
}
