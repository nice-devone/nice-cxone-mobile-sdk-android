package com.nice.cxonechat.event.thread

import com.nice.cxonechat.internal.model.CustomFieldModel
import com.nice.cxonechat.internal.model.network.ActionSetContactCustomFields
import com.nice.cxonechat.state.Connection
import com.nice.cxonechat.thread.ChatThread

internal class SetContactCustomFieldEvent(
    private val fields: List<CustomFieldModel>,
) : ChatThreadEvent() {

    override fun getModel(
        thread: ChatThread,
        connection: Connection,
    ) = ActionSetContactCustomFields(
        connection = connection,
        thread = thread,
        fields = fields
    )
}
