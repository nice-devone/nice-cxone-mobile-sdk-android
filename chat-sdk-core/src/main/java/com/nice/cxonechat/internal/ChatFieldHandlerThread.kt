package com.nice.cxonechat.internal

import com.nice.cxonechat.Chat
import com.nice.cxonechat.ChatFieldHandler
import com.nice.cxonechat.ChatThreadHandler
import com.nice.cxonechat.event.thread.SetContactCustomFieldEvent
import com.nice.cxonechat.internal.copy.ChatThreadCopyable.Companion.asCopyable
import com.nice.cxonechat.internal.model.ChatThreadMutable
import com.nice.cxonechat.internal.model.CustomFieldModel
import com.nice.cxonechat.state.validate

internal class ChatFieldHandlerThread(
    private val chat: Chat,
    private val handler: ChatThreadHandler,
    private val thread: ChatThreadMutable,
) : ChatFieldHandler {

    override fun add(fields: Map<String, String>) {
        chat.configuration.allCustomFields.validate(fields)
        val customFields = fields.map(::CustomFieldModel)
        handler.events().trigger(SetContactCustomFieldEvent(customFields)) {
            val mappedFields = customFields
                .map(CustomFieldModel::toCustomField)
            thread += thread.asCopyable().copy(fields = mappedFields)
        }
    }
}
