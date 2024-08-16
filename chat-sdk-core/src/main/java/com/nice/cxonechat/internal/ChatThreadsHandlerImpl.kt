/*
 * Copyright (c) 2021-2024. NICE Ltd. All rights reserved.
 *
 * Licensed under the NICE License;
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    https://github.com/nice-devone/nice-cxone-mobile-sdk-android/blob/main/LICENSE
 *
 * TO THE EXTENT PERMITTED BY APPLICABLE LAW, THE CXONE MOBILE SDK IS PROVIDED ON
 * AN “AS IS” BASIS. NICE HEREBY DISCLAIMS ALL WARRANTIES AND CONDITIONS, EXPRESS
 * OR IMPLIED, INCLUDING (WITHOUT LIMITATION) WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE, NON-INFRINGEMENT, AND TITLE.
 */

package com.nice.cxonechat.internal

import com.nice.cxonechat.Cancellable
import com.nice.cxonechat.ChatMode.LiveChat
import com.nice.cxonechat.ChatMode.MultiThread
import com.nice.cxonechat.ChatMode.SingleThread
import com.nice.cxonechat.ChatThreadHandler
import com.nice.cxonechat.ChatThreadsHandler
import com.nice.cxonechat.internal.model.ChatThreadInternal
import com.nice.cxonechat.internal.model.ChatThreadMutable
import com.nice.cxonechat.internal.model.ChatThreadMutable.Companion.asMutable
import com.nice.cxonechat.internal.model.CustomFieldInternal
import com.nice.cxonechat.internal.model.network.EventCaseStatusChanged
import com.nice.cxonechat.internal.model.network.EventThreadListFetched
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
import com.nice.cxonechat.thread.ChatThreadState.Pending
import com.nice.cxonechat.util.UUIDProvider

internal class ChatThreadsHandlerImpl(
    private val chat: ChatWithParameters,
    override val preChatSurvey: PreChatSurvey?,
) : ChatThreadsHandler {

    override fun refresh() = Unit

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

        val uuid = UUIDProvider.next()
        val thread = ChatThreadInternal(
            id = uuid,
            fields = combinedCustomFieldMap.map(::CustomFieldInternal),
            threadState = Pending,
        )
        return createHandler(thread, true)
    }

    override fun threads(listener: ChatThreadsHandler.OnThreadsUpdatedListener): Cancellable {
        var threads: List<ChatThreadMutable> = emptyList()
        val threadListFetched = chat.socketListener.addCallback(EventThreadListFetched) { event ->
            threads = event.threads.map { threadData -> threadData.toChatThread().asMutable() }
            listener.onThreadsUpdated(threads)
        }
        if (chat.chatMode === SingleThread) {
            return threadListFetched
        }
        val threadArchived = chat.socketListener.addCallback(EventCaseStatusChanged) { event ->
            threads.asSequence()
                .filter(event::inThread)
                .forEach { thread ->
                    CaseStatusChangedHandlerActions.handleCaseClosed(thread, event) { _ ->
                        listener.onThreadsUpdated(threads)
                    }
                }
        }

        return Cancellable(
            threadListFetched,
            threadArchived
        )
    }

    override fun thread(thread: ChatThread): ChatThreadHandler = createHandler(thread)

    // ---

    private fun createHandler(
        thread: ChatThread,
        isThreadCreated: Boolean = false,
    ): ChatThreadHandler {
        val mutableThread = thread as? ChatThreadMutable ?: ChatThreadMutable.from(thread)
        var handler: ChatThreadHandler
        handler = ChatThreadHandlerImpl(chat, mutableThread)
        if (chat.chatMode === MultiThread) {
            handler = ChatThreadHandlerMulti(chat, mutableThread, handler)
        }
        handler = ChatThreadHandlerMetadata(handler, chat, mutableThread)
        handler = ChatThreadHandlerMessages(handler, chat, mutableThread)
        handler = ChatThreadHandlerAgentUpdate(handler, chat, mutableThread)
        handler = ChatThreadHandlerAgentTyping(handler, chat)
        handler = ChatThreadHandlerMessageReadByAgent(handler, chat, mutableThread)
        if (chat.chatMode === LiveChat) handler = ChatThreadHandlerLiveChat(handler, chat, mutableThread, isThreadCreated)
        if (isThreadCreated) handler = ChatThreadHandlerWelcome(handler, chat, mutableThread)
        handler = ChatThreadHandlerAggregatingListener(handler)
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
