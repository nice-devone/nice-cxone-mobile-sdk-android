package com.nice.cxonechat.model

import com.nice.cxonechat.internal.model.Thread
import com.nice.cxonechat.tool.nextString
import java.util.UUID

internal fun makeThread(
    idOnExternalPlatform: UUID = UUID.randomUUID(),
    threadName: String? = nextString()
) = Thread(
    idOnExternalPlatform = idOnExternalPlatform,
    threadName = threadName
)
