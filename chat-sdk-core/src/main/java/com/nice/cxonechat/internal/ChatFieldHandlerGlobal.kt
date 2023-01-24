package com.nice.cxonechat.internal

import com.nice.cxonechat.ChatFieldHandler
import com.nice.cxonechat.event.SetCustomerCustomFieldEvent
import com.nice.cxonechat.internal.model.CustomFieldInternal

internal class ChatFieldHandlerGlobal(
    private val chat: ChatWithParameters,
) : ChatFieldHandler {

    override fun add(fields: Map<String, String>) {
        chat.fields = (fields.map(::CustomFieldInternal) + chat.fields).distinctBy { it.id }
        chat.events().trigger(SetCustomerCustomFieldEvent(fields))
    }

}
