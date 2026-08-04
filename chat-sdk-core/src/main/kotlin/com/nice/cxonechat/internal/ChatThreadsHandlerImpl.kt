/*
 * Copyright (c) 2021-2026. NICE Ltd. All rights reserved.
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

import com.nice.cxonechat.ChatMode.LiveChat
import com.nice.cxonechat.ChatMode.MultiThread
import com.nice.cxonechat.ChatMode.SingleThread
import com.nice.cxonechat.ChatThreadHandler
import com.nice.cxonechat.ChatThreadsHandler
import com.nice.cxonechat.internal.model.ChatThreadInternal
import com.nice.cxonechat.internal.model.ChatThreadMutable
import com.nice.cxonechat.internal.model.ChatThreadMutable.Companion.asMutable
import com.nice.cxonechat.internal.model.CustomFieldInternal
import com.nice.cxonechat.internal.model.WelcomeMessage
import com.nice.cxonechat.internal.model.network.EventCaseStatusChanged
import com.nice.cxonechat.internal.model.network.EventThreadListFetched
import com.nice.cxonechat.internal.socket.EventCallback.Companion.addCallback
import com.nice.cxonechat.log.LoggerScope
import com.nice.cxonechat.log.scope
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
import com.nice.cxonechat.util.newBufferedSharedFlow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.shareIn

internal class ChatThreadsHandlerImpl(
    private val chat: ChatWithParameters,
    override val preChatSurvey: PreChatSurvey?,
) : ChatThreadsHandler, LoggerScope by LoggerScope("ChatThreadsHandlerImpl", chat.entrails.logger) {

    private val threadsMutable = mutableListOf<ChatThreadMutable>()

    // _creates is a hot MutableSharedFlow so locally-created threads emit immediately inside
    // the synchronized block in create(), before the server confirms them.
    // _socketUpdates is a cold channelFlow; because threadsFlow uses SharingStarted.Lazily, its
    // socket callbacks register only when the first subscriber collects threadsFlow — not at construction.
    // merge() combines them so subscribers see both optimistic local creates and server-driven updates.
    private val _creates = newBufferedSharedFlow<List<ChatThread>>()

    private val _socketUpdates: Flow<List<ChatThread>> = channelFlow {
        val channel = this
        val loggerScope = this@ChatThreadsHandlerImpl
        val onThreadListFetched = chat.socketListener.addCallback(EventThreadListFetched) { event ->
            val newList = event.threads.map { threadData -> threadData.toChatThread().asMutable() }
            val ids = newList.map(ChatThreadMutable::id)
            synchronized(threadsMutable) {
                val pending = threadsMutable.filter { it.threadState === Pending && it.id !in ids }
                threadsMutable.clear()
                threadsMutable.addAll(newList + pending)
                trySendOrLog(
                    channel = channel,
                    value = threadsMutable.toList(),
                    loggerScope = loggerScope,
                    tag = "thread list update"
                )
            }
        }
        val onCaseStatusChanged = if (chat.chatMode !== SingleThread) {
            chat.socketListener.addCallback(EventCaseStatusChanged) { event ->
                val threadCopy = synchronized(threadsMutable) { threadsMutable.toList() }
                threadCopy.asSequence()
                    .filter(event::inThread)
                    .forEach { thread ->
                        CaseStatusChangedHandlerActions.handleCaseClosed(thread, event) { _ ->
                            trySendOrLog(
                                channel = channel,
                                value = synchronized(threadsMutable) { threadsMutable.toList() },
                                loggerScope = loggerScope,
                                tag = "case-status update",
                            )
                        }
                    }
            }
        } else {
            null
        }
        awaitClose {
            onThreadListFetched.cancel()
            onCaseStatusChanged?.cancel()
        }
    }

    override val threadsFlow: Flow<List<ChatThread>> = merge(_socketUpdates, _creates)
        .shareIn(chat.entrails.threading.coroutineScope, SharingStarted.Lazily, replay = 1)

    override fun refresh() = Unit

    override fun create(
        customFields: Map<String, String>,
        preChatSurveyResponse: Sequence<PreChatSurveyResponse<out FieldDefinition, out Any>>,
    ): ChatThreadHandler = scope("create") {
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
        val threadHandler = createHandler(thread, true)
        val snapshot = synchronized(threadsMutable) {
            threadsMutable.add(ChatThreadMutable.from(thread))
            threadsMutable.toList()
        }
        _creates.tryEmit(snapshot)
        return threadHandler
    }

    override fun thread(thread: ChatThread): ChatThreadHandler = createHandler(thread)

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
        handler = ChatThreadHandlerMessageSeenChanged(origin = handler, chat, mutableThread)
        if (chat.chatMode === LiveChat) {
            handler = ChatThreadHandlerWelcomeLiveChat(handler, chat, mutableThread)
            handler = ChatThreadHandlerLiveChat(handler, chat, mutableThread, isThreadCreated)
        } else if (thread.threadState == Pending || thread.messages.firstOrNull() is WelcomeMessage) {
            // Add the welcome message decorator for locally created (Pending) threads or threads
            // recovered from the server that already carry a welcome message — in both cases the
            // decorator ensures the welcome is properly handled.
            handler = ChatThreadHandlerWelcome(handler, chat, mutableThread)
        }
        handler = ChatThreadHandlerFilter(handler, chat)
        handler = ChatThreadHandlerShared(handler, chat.entrails.threading.coroutineScope)
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
