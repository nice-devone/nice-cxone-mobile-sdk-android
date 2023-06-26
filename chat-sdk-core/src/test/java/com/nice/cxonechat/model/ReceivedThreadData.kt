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
