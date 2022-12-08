package com.nice.cxonechat.internal

import com.nice.cxonechat.enums.ActionType
import com.nice.cxonechat.enums.EventType.FireProactiveAction
import com.nice.cxonechat.internal.model.CustomFieldModel
import com.nice.cxonechat.internal.model.network.EventProactiveAction
import com.nice.cxonechat.socket.EventCallback.Companion.addCallback

internal class ChatWelcomeMessageUpdate(
    private val origin: ChatWithParameters,
) : ChatWithParameters by origin {

    private val listener = socket.addCallback<EventProactiveAction>(FireProactiveAction) { model ->
        if (model.type == ActionType.WelcomeMessage) {
            storage.welcomeMessage = model.bodyText
            val customFields = model.customFields.map(CustomFieldModel::toCustomField)
            fields = (customFields + fields).distinctBy { it.id }
        }
    }

    override fun close() {
        listener.cancel()
        origin.close()
    }

}
