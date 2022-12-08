package com.nice.cxonechat.event

import com.nice.cxonechat.internal.model.CustomFieldModel
import com.nice.cxonechat.internal.model.network.ActionSetCustomerCustomFields
import com.nice.cxonechat.state.Connection
import com.nice.cxonechat.storage.ValueStorage

internal class SetCustomerCustomFieldEvent(
    private val fields: Map<String, String>,
) : ChatEvent() {

    override fun getModel(
        connection: Connection,
        storage: ValueStorage,
    ) = ActionSetCustomerCustomFields(
        connection = connection,
        fields = fields.map(::CustomFieldModel)
    )

}
