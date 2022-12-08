package com.nice.cxonechat.internal

import com.nice.cxonechat.api.RemoteService
import com.nice.cxonechat.log.Logger
import com.nice.cxonechat.state.Environment
import com.nice.cxonechat.storage.ValueStorage

internal interface ChatEntrails {

    val storage: ValueStorage
    val service: RemoteService
    val threading: Threading
    val environment: Environment
    val logger: Logger

}
