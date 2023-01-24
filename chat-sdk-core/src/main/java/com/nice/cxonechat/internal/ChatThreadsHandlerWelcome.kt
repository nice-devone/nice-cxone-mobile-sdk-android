package com.nice.cxonechat.internal

import com.nice.cxonechat.ChatThreadHandler
import com.nice.cxonechat.ChatThreadsHandler
import com.nice.cxonechat.event.thread.SendOutboundEvent
import com.nice.cxonechat.thread.CustomField

internal class ChatThreadsHandlerWelcome(
    private val origin: ChatThreadsHandler,
    private val chat: ChatWithParameters,
) : ChatThreadsHandler by origin {

    override fun create(customFields: Map<String, String>): ChatThreadHandler {
        return origin.create(customFields)
            .also(::addWelcomeMessageToThread)
    }

    private fun addWelcomeMessageToThread(handler: ChatThreadHandler) {
        val storedMessage = chat.storage.welcomeMessage
        if (storedMessage.isBlank()) return

        val connection = chat.connection
        val parameters = mapOf(
            "firstName" to connection.firstName,
            "lastName" to connection.lastName,
        )
        val customerFieldMap = chat.fields.toMap()
        val contactFieldMap = handler.get().fields.toMap()
        val message = VariableMessageParser.parse(
            storedMessage,
            parameters,
            customerFieldMap,
            contactFieldMap
        )
        val token = chat.storage.authToken
        handler.events().trigger(SendOutboundEvent(message, token))
    }

    private companion object {
        private fun customFieldAsPair(customField: CustomField): Pair<String, String> = customField.id to customField.value
        private fun List<CustomField>.toMap() = associate(::customFieldAsPair)
    }
}
