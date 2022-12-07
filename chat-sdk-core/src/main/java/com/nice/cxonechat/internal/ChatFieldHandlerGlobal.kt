package com.nice.cxonechat.internal

import com.nice.cxonechat.ChatFieldHandler
import com.nice.cxonechat.event.SetCustomerCustomFieldEvent

internal class ChatFieldHandlerGlobal(
    private val chat: ChatWithParameters,
) : ChatFieldHandler {

    override fun add(fields: Map<String, String>) {
        chat.events().trigger(SetCustomerCustomFieldEvent(fields))
    }

}
