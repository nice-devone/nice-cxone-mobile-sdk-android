package com.nice.cxonechat.internal

import com.nice.cxonechat.ChatFieldHandler
import com.nice.cxonechat.ChatThreadHandler
import com.nice.cxonechat.event.thread.SetContactCustomFieldEvent
import com.nice.cxonechat.internal.copy.ChatThreadCopyable.Companion.asCopyable
import com.nice.cxonechat.internal.model.ChatThreadMutable
import com.nice.cxonechat.internal.model.CustomFieldModel

internal class ChatFieldHandlerThread(
    private val handler: ChatThreadHandler,
    private val thread: ChatThreadMutable,
) : ChatFieldHandler {

    override fun add(fields: Map<String, String>) {
        val customFields = fields.map(::CustomFieldModel)
        handler.events().trigger(SetContactCustomFieldEvent(customFields)) {
            val customFields = customFields.map(CustomFieldModel::toCustomField)
            thread += thread.asCopyable().copy(fields = customFields)
        }
    }

}
