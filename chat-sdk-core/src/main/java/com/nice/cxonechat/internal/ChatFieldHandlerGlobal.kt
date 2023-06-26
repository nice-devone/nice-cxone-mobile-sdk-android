package com.nice.cxonechat.internal

import com.nice.cxonechat.ChatFieldHandler
import com.nice.cxonechat.event.SetCustomerCustomFieldEvent
import com.nice.cxonechat.internal.model.CustomFieldInternal
import com.nice.cxonechat.internal.model.CustomFieldInternal.Companion.updateWith
import com.nice.cxonechat.state.validate

internal class ChatFieldHandlerGlobal(
    private val chat: ChatWithParameters,
) : ChatFieldHandler {

    override fun add(fields: Map<String, String>) {
        chat.configuration.allCustomFields.validate(fields)
        chat.fields = chat.fields.updateWith(
            fields.map(::CustomFieldInternal)
        )
        chat.events().trigger(SetCustomerCustomFieldEvent(fields))
    }
}
