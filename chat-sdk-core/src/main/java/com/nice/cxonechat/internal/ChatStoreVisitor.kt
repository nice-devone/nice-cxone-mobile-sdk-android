package com.nice.cxonechat.internal

import com.nice.cxonechat.internal.model.network.ActionStoreVisitor
import com.nice.cxonechat.internal.socket.send

/**
 * The class which applies effect on supplied origin parameter.
 * The effect will send StoreVisitor event, when this class is initialized.
 *
 * @param origin [ChatWithParameters] instance to which apply the effect.
 */
internal class ChatStoreVisitor(origin: ChatWithParameters) : ChatWithParameters by origin {
    init {
        val event = ActionStoreVisitor(
            connection = connection,
            visitor = storage.visitorId,
            deviceToken = null
        )
        socket.send(event)
    }
}
