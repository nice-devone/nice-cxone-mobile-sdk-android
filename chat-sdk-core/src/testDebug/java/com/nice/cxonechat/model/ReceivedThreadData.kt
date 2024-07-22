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

package com.nice.cxonechat.model

import com.nice.cxonechat.internal.model.Thread
import com.nice.cxonechat.internal.model.network.ReceivedThreadData
import com.nice.cxonechat.thread.ChatThread
import java.util.Date

internal fun ChatThread.toReceived() = ReceivedThreadData(
    id = "channelId_$id",
    idOnExternalPlatform = id,
    channelId = "channelId",
    threadName = threadName.toString(),
    createdAt = Date(0),
    updatedAt = Date(0),
    canAddMoreMessages = canAddMoreMessages,
    thread = Thread(this),
)
