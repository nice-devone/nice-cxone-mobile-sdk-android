package com.nice.cxonechat.tool

import com.nice.cxonechat.api.RemoteService
import com.nice.cxonechat.internal.ChatEntrails
import com.nice.cxonechat.internal.Threading
import com.nice.cxonechat.log.Logger
import com.nice.cxonechat.state.Environment
import com.nice.cxonechat.storage.ValueStorage

internal class ChatEntrailsMock(
    override val storage: ValueStorage,
    override val service: RemoteService,
    override val logger: Logger,
    override val environment: Environment,
) : ChatEntrails {

    override val threading: Threading = Threading.Identity

}
