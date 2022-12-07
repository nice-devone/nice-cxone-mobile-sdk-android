package com.nice.cxonechat.internal.model

import com.nice.cxonechat.message.MessageAuthor
import java.util.UUID

internal object MessageAuthorDefaults {

    val User: MessageAuthor = MessageAuthorInternal(
        id = UUID.randomUUID().toString(),
        firstName = "Unknown",
        lastName = "Customer"
    )

    val Agent: MessageAuthor = MessageAuthorInternal(
        id = "0",
        firstName = "Automated",
        lastName = "Agent"
    )

}
