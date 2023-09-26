/*
 * Copyright (c) 2021-2023. NICE Ltd. All rights reserved.
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

package com.nice.cxonechat.ui.main

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nice.cxonechat.Chat
import com.nice.cxonechat.ChatActionHandler.OnPopupActionListener
import com.nice.cxonechat.ChatEventHandlerActions.proactiveActionClick
import com.nice.cxonechat.ChatEventHandlerActions.proactiveActionDisplay
import com.nice.cxonechat.ChatEventHandlerActions.proactiveActionFailure
import com.nice.cxonechat.ChatEventHandlerActions.proactiveActionSuccess
import com.nice.cxonechat.ChatThreadMessageHandler
import com.nice.cxonechat.ChatThreadMessageHandler.OnMessageTransferListener
import com.nice.cxonechat.analytics.ActionMetadata
import com.nice.cxonechat.event.thread.MarkThreadReadEvent
import com.nice.cxonechat.event.thread.TypingEndEvent
import com.nice.cxonechat.event.thread.TypingStartEvent
import com.nice.cxonechat.message.Attachment
import com.nice.cxonechat.message.MessageAuthor
import com.nice.cxonechat.message.MessageDirection
import com.nice.cxonechat.message.MessageDirection.ToAgent
import com.nice.cxonechat.message.MessageMetadata
import com.nice.cxonechat.message.OutboundMessage
import com.nice.cxonechat.thread.Agent
import com.nice.cxonechat.thread.ChatThread
import com.nice.cxonechat.ui.data.ContentDataSourceList
import com.nice.cxonechat.ui.data.flow
import com.nice.cxonechat.ui.domain.SelectedThreadRepository
import com.nice.cxonechat.ui.main.ChatThreadViewModel.ChatMetadata.Companion.asMetadata
import com.nice.cxonechat.ui.main.ChatThreadViewModel.OnPopupActionState.Empty
import com.nice.cxonechat.ui.main.ChatThreadViewModel.OnPopupActionState.ReceivedOnPopupAction
import com.nice.cxonechat.ui.main.ChatThreadViewModel.ReportOnPopupAction.CLICKED
import com.nice.cxonechat.ui.main.ChatThreadViewModel.ReportOnPopupAction.DISPLAYED
import com.nice.cxonechat.ui.main.ChatThreadViewModel.ReportOnPopupAction.FAILURE
import com.nice.cxonechat.ui.main.ChatThreadViewModel.ReportOnPopupAction.SUCCESS
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference
import java.util.*
import javax.inject.Inject
import com.nice.cxonechat.message.Message as SdkMessage

@Suppress("TooManyFunctions")
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
internal class ChatThreadViewModel @Inject constructor(
    private val contentDataSource: ContentDataSourceList,
    private val selectedThreadRepository: SelectedThreadRepository,
    private val chat: Chat,
) : ViewModel() {

    private val isMultiThreadEnabled = chat.configuration.hasMultipleThreadsPerEndUser
    private val chatThreadHandler by lazy { selectedThreadRepository.chatThreadHandler!! }
    private val chatThreadFlow = chatThreadHandler.flow

    /** Tracks messages before they are confirmed as received by backend. */
    private val sentMessagesFlow: MutableStateFlow<Map<UUID, SdkMessage>> = MutableStateFlow(emptyMap())

    private val receivedMessagesFlow: Flow<Map<UUID, SdkMessage>> = chatThreadFlow
        .mapLatest { chatThread -> chatThread.messages }
        .distinctUntilChanged()
        .map { messages -> messages.associateBy(SdkMessage::id) }

    val messages: StateFlow<List<SdkMessage>> = sentMessagesFlow
        .combine(receivedMessagesFlow) { sentMessageMap, chatUpdatePlusMessageMap ->
            sentMessageMap.plus(
                chatUpdatePlusMessageMap
            ).values
        }
        .map { collection -> collection.sortedByDescending { message -> message.createdAt } }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())

    val agentState: StateFlow<Boolean> = chatThreadFlow
        .mapLatest { chatThread -> chatThread.threadAgent }
        .distinctUntilChanged()
        .map { threadAgent -> threadAgent?.isTyping == true }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), false)

    private val chatMetadataMutableState = chatThreadFlow
        .mapLatest { chatThread -> chatThread.asMetadata(isMultiThreadEnabled) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)

    val chatMetadata = chatMetadataMutableState.filterNotNull()

    val canLoadMore: StateFlow<Boolean> = chatThreadFlow
        .map { it.hasMoreMessagesToLoad }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), false)

    private val mutableActionState: MutableStateFlow<OnPopupActionState> = MutableStateFlow(Empty)
    private val actionHandler = chat.actions()
    private val messageHandler: ChatThreadMessageHandler = chatThreadHandler.messages()
    private val eventHandler = chatThreadHandler.events()

    val actionState: StateFlow<OnPopupActionState> = mutableActionState.asStateFlow()

    init {
        val listener = OnPopupActionListener { variables, metadata ->
            mutableActionState.value = ReceivedOnPopupAction(variables, metadata)
        }
        actionHandler.onPopup(listener)
    }

    fun refresh() {
        chatThreadHandler.refresh()
    }

    fun sendMessage(message: OutboundMessage) {
        val appMessage: (UUID) -> SdkMessage = { id ->
            TemporarySentMessage(
                id = id,
                text = message.message,
            )
        }
        val listener = OnMessageSentListener(appMessage, sentMessagesFlow)
        messageHandler.send(message, listener)
    }

    class OnMessageSentListener(
        private val message: (UUID) -> SdkMessage,
        flow: MutableStateFlow<Map<UUID, SdkMessage>>,
    ) : OnMessageTransferListener {

        private val weakRef = WeakReference(flow)
        override fun onProcessed(id: UUID) {
            val map = weakRef.get() ?: return
            val appMessage = message(id)
            map.value = map.value.plus(appMessage.id to appMessage)
        }
    }

    fun sendAttachment(attachment: Uri, message: String? = null) {
        viewModelScope.launch {
            val contentDescriptor = contentDataSource.descriptorForUri(attachment) ?: return@launch
            val appMessage: (UUID) -> SdkMessage = { id ->
                TemporarySentMessage(
                    id = id,
                    attachment = object : Attachment {
                        override val friendlyName: String = contentDescriptor.friendlyName ?: "unnamed"
                        override val mimeType: String = contentDescriptor.mimeType ?: "application/octet-stream"
                        override val url: String = attachment.toString()
                    },
                    text = message.orEmpty(),
                )
            }
            val listener = OnMessageSentListener(appMessage, sentMessagesFlow)
            messageHandler.send(
                OutboundMessage(
                    attachments = listOf(contentDescriptor),
                    message = message.orEmpty()
                ),
                listener = listener,
            )
        }
    }

    fun loadMore() {
        messageHandler.loadMore()
    }

    fun reportThreadRead() {
        eventHandler.trigger(MarkThreadReadEvent)
    }

    fun reportTypingStarted() {
        eventHandler.trigger(TypingStartEvent)
    }

    fun reportTypingEnd() {
        eventHandler.trigger(TypingEndEvent)
    }

    fun reportOnPopupActionDisplayed(action: ReceivedOnPopupAction) {
        chat.events().proactiveActionDisplay(action.metadata)
    }

    fun reportOnPopupActionClicked(action: ReceivedOnPopupAction) {
        chat.events().proactiveActionClick(action.metadata)
    }

    fun reportOnPopupAction(
        reportType: ReportOnPopupAction,
        action: ReceivedOnPopupAction,
    ) {
        val events = chat.events()
        when (reportType) {
            DISPLAYED -> events.proactiveActionDisplay(action.metadata)
            CLICKED -> events.proactiveActionClick(action.metadata)
            SUCCESS -> {
                events.proactiveActionSuccess(action.metadata)
                clearOnPopupAction(action)
            }
            FAILURE -> {
                events.proactiveActionFailure(action.metadata)
                clearOnPopupAction(action)
            }
        }
    }

    private fun clearOnPopupAction(action: ReceivedOnPopupAction) {
        if (mutableActionState.value == action) {
            mutableActionState.value = Empty
        }
    }

    override fun onCleared() {
        actionHandler.close()
        super.onCleared()
    }

    sealed interface OnPopupActionState {
        object Empty : OnPopupActionState
        data class ReceivedOnPopupAction(val variables: Any, val metadata: ActionMetadata) : OnPopupActionState
    }

    enum class ReportOnPopupAction {
        DISPLAYED,
        CLICKED,
        SUCCESS,
        FAILURE
    }

    data class ChatMetadata(
        val threadName: String,
        val agent: Agent?,
    ) {
        companion object {
            fun ChatThread.asMetadata(isMultiThreadEnabled: Boolean) = ChatMetadata(
                threadName = threadName ?: threadAgent?.fullName.takeIf { isMultiThreadEnabled }.orEmpty(),
                agent = threadAgent,
            )
        }
    }
}

private data class TemporarySentMessage(
    override val id: UUID,
    override val threadId: UUID,
    override val createdAt: Date,
    override val direction: MessageDirection,
    override val metadata: MessageMetadata,
    override val author: MessageAuthor,
    override val attachments: Iterable<Attachment>,
    override val fallbackText: String?,
    override val text: String,
) : SdkMessage.Text() {
    constructor(id: UUID, text: String) : this(
        id = id,
        threadId = UUID.randomUUID(),
        createdAt = Date(),
        direction = ToAgent,
        metadata = object : MessageMetadata {
            override val readAt: Date? = null
        },
        author = object : MessageAuthor() {
            override val id: String = ChatThreadFragment.SENDER_ID
            override val firstName: String = ""
            override val lastName: String = ""
            override val imageUrl: String? = null
        },
        attachments = emptyList(),
        fallbackText = null,
        text = text
    )

    constructor(id: UUID, text: String, attachment: Attachment) : this(
        id = id,
        threadId = UUID.randomUUID(),
        createdAt = Date(),
        direction = ToAgent,
        metadata = object : MessageMetadata {
            override val readAt: Date? = null
        },
        author = object : MessageAuthor() {
            override val id: String = ChatThreadFragment.SENDER_ID
            override val firstName: String = ""
            override val lastName: String = ""
            override val imageUrl: String? = null
        },
        attachments = listOf(attachment),
        fallbackText = null,
        text = text
    )
}
