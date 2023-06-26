package com.nice.cxonechat.internal

import com.nice.cxonechat.Cancellable
import com.nice.cxonechat.ChatThreadHandler
import com.nice.cxonechat.ChatThreadsHandler
import com.nice.cxonechat.enums.EventType.ThreadArchived
import com.nice.cxonechat.enums.EventType.ThreadListFetched
import com.nice.cxonechat.event.FetchThreadEvent
import com.nice.cxonechat.internal.model.ChatThreadInternal
import com.nice.cxonechat.internal.model.ChatThreadMutable
import com.nice.cxonechat.internal.model.CustomFieldInternal
import com.nice.cxonechat.internal.model.network.EventThreadListFetched
import com.nice.cxonechat.internal.model.network.ReceivedThreadData
import com.nice.cxonechat.internal.socket.EventCallback.Companion.addCallback
import com.nice.cxonechat.prechat.PreChatSurvey
import com.nice.cxonechat.prechat.PreChatSurveyResponse
import com.nice.cxonechat.prechat.PreChatSurveyResponse.Hierarchy
import com.nice.cxonechat.prechat.PreChatSurveyResponse.Selector
import com.nice.cxonechat.prechat.PreChatSurveyResponse.Text
import com.nice.cxonechat.state.FieldDefinition
import com.nice.cxonechat.state.checkRequired
import com.nice.cxonechat.state.validate
import com.nice.cxonechat.thread.ChatThread
import java.util.UUID

internal class ChatThreadsHandlerImpl(
    private val chat: ChatWithParameters,
    override val preChatSurvey: PreChatSurvey?,
) : ChatThreadsHandler {

    override fun refresh() {
        chat.events().trigger(FetchThreadEvent)
    }

    override fun create(
        customFields: Map<String, String>,
        preChatSurveyResponse: Sequence<PreChatSurveyResponse<out FieldDefinition, out Any>>,
    ): ChatThreadHandler {
        val combinedCustomFieldMap = preChatSurveyResponse
            .mapNotNull(::toStringPair)
            .toMap(customFields.toMutableMap())
            .filterValues(String::isNotEmpty)

        preChatSurvey?.fields?.run {
            validate(combinedCustomFieldMap)
            checkRequired(combinedCustomFieldMap)
        }

        val uuid = UUID.randomUUID()
        val thread = ChatThreadInternal(
            id = uuid,
            fields = combinedCustomFieldMap.map(::CustomFieldInternal)
        )
        return createHandler(thread)
    }

    override fun threads(listener: ChatThreadsHandler.OnThreadsUpdatedListener): Cancellable {
        val threadListFetched = chat.socketListener.addCallback<EventThreadListFetched>(ThreadListFetched) { event ->
            listener.onThreadsUpdated(event.threads.map(ReceivedThreadData::toChatThread))
        }
        val threadArchived = chat.socketListener.addCallback<Any>(ThreadArchived) {
            refresh()
        }
        return Cancellable(
            threadListFetched,
            threadArchived
        )
    }

    override fun thread(thread: ChatThread): ChatThreadHandler = createHandler(ChatThreadMutable.from(thread))

    // ---

    private fun createHandler(
        thread: ChatThread,
    ): ChatThreadHandler {
        val mutableThread = thread as? ChatThreadMutable ?: ChatThreadMutable.from(thread)
        var handler: ChatThreadHandler
        handler = ChatThreadHandlerImpl(chat, mutableThread)
        handler = ChatThreadHandlerMetadata(handler, chat, mutableThread)
        handler = ChatThreadHandlerMessages(handler, chat, mutableThread)
        handler = ChatThreadHandlerAgentUpdate(handler, chat, mutableThread)
        handler = ChatThreadHandlerAgentTyping(handler, chat)
        return handler
    }

    /**
     * Combine id of an answered survey with response (either response value or id of a selectable element).
     * Illegal answers are converted to null values.
     *
     * @return Pair consisting of id of question and value/id of response.
     * Null if the hierarchical response is not a leaf node.
     */
    private fun toStringPair(it: PreChatSurveyResponse<out FieldDefinition, out Any>) = when (it) {
        is Text -> it.question.fieldId to it.response
        is Selector -> it.question.fieldId to it.response.nodeId
        is Hierarchy ->
            if (!it.response.isLeaf) {
                null // only leaf nodes are valid responses
            } else {
                it.question.fieldId to it.response.nodeId
            }
    }
}
