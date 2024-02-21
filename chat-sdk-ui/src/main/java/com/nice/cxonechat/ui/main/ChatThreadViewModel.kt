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
import com.nice.cxonechat.message.MessageStatus
import com.nice.cxonechat.message.MessageStatus.SENDING
import com.nice.cxonechat.message.OutboundMessage
import com.nice.cxonechat.prechat.PreChatSurvey
import com.nice.cxonechat.thread.Agent
import com.nice.cxonechat.thread.ChatThread
import com.nice.cxonechat.thread.CustomField
import com.nice.cxonechat.ui.customvalues.CustomValueItemList
import com.nice.cxonechat.ui.customvalues.extractStringValues
import com.nice.cxonechat.ui.data.ContentDataSourceList
import com.nice.cxonechat.ui.data.flow
import com.nice.cxonechat.ui.domain.SelectedThreadRepository
import com.nice.cxonechat.ui.main.ChatThreadViewModel.ChatMetadata.Companion.asMetadata
import com.nice.cxonechat.ui.main.ChatThreadViewModel.Dialogs.AudioPlayer
import com.nice.cxonechat.ui.main.ChatThreadViewModel.Dialogs.CustomValues
import com.nice.cxonechat.ui.main.ChatThreadViewModel.Dialogs.EditThreadName
import com.nice.cxonechat.ui.main.ChatThreadViewModel.Dialogs.None
import com.nice.cxonechat.ui.main.ChatThreadViewModel.Dialogs.SelectAttachments
import com.nice.cxonechat.ui.main.ChatThreadViewModel.OnPopupActionState.Empty
import com.nice.cxonechat.ui.main.ChatThreadViewModel.OnPopupActionState.ReceivedOnPopupAction
import com.nice.cxonechat.ui.main.ChatThreadViewModel.ReportOnPopupAction.CLICKED
import com.nice.cxonechat.ui.main.ChatThreadViewModel.ReportOnPopupAction.DISPLAYED
import com.nice.cxonechat.ui.main.ChatThreadViewModel.ReportOnPopupAction.FAILURE
import com.nice.cxonechat.ui.main.ChatThreadViewModel.ReportOnPopupAction.SUCCESS
import com.nice.cxonechat.ui.util.isEmpty
import com.nice.cxonechat.utilities.isEmpty
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
import org.koin.android.annotation.KoinViewModel
import java.lang.ref.WeakReference
import java.util.Date
import java.util.UUID
import com.nice.cxonechat.message.Message as SdkMessage

@Suppress("TooManyFunctions")
@OptIn(ExperimentalCoroutinesApi::class)
@KoinViewModel
internal class ChatThreadViewModel(
    private val contentDataSource: ContentDataSourceList,
    private val selectedThreadRepository: SelectedThreadRepository,
    private val chat: Chat,
) : ViewModel() {
    private val threads by lazy { chat.threads() }

    val isMultiThreadEnabled = chat.configuration.hasMultipleThreadsPerEndUser
    val preChatSurvey: PreChatSurvey?
        get() = threads.preChatSurvey
    val hasQuestions: Boolean
        get() = preChatSurvey?.fields?.isEmpty() == false
    private val chatThreadHandler = selectedThreadRepository.chatThreadHandler!!
    private val chatThreadFlow = chatThreadHandler.flow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null) // Share incoming flow values
        .filterNotNull()

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

    val customValues: List<CustomField>
        get() = selectedThreadRepository.chatThreadHandler?.get()?.fields ?: listOf()

    val selectedThreadName: String?
        get() = selectedThreadRepository.chatThreadHandler?.get()?.threadName

    sealed interface Dialogs {
        object None : Dialogs
        object CustomValues : Dialogs
        object EditThreadName : Dialogs
        data class AudioPlayer(
            val url: String,
            val title: String?,
        ) : Dialogs
        data class SelectAttachments(
            val attachments: List<Attachment>,
            val title: String?
        ) : Dialogs

        data class VideoPlayer(
            val uri: String,
            val title: String?,
        ) : Dialogs

        data class ImageViewer(
            val image: Any?,
            val title: String?,
        ) : Dialogs
    }

    private val showDialog = MutableStateFlow<Dialogs>(None)
    val dialogShown = showDialog.asStateFlow()

    // Note this is explicitly *not* part of Dialogs to allow it to be stacked over the select attachments dialog.
    private val preparing = MutableStateFlow(false)
    val preparingToShare = preparing.asStateFlow()

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
        // Ignore messages with no text, attachments, or postback.
        if (message.attachments.isEmpty() && message.message.isBlank() && message.postback?.isBlank() == true) {
            return
        }

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
        viewModelScope.launch(Dispatchers.IO) {
            val contentDescriptor = contentDataSource.descriptorForUri(attachment) ?: return@launch
            val appMessage: (UUID) -> SdkMessage = { id ->
                TemporarySentMessage(
                    id = id,
                    attachment = object : Attachment {
                        override val friendlyName: String = contentDescriptor.friendlyName ?: "unnamed"
                        override val mimeType: String = contentDescriptor.mimeType
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

    internal fun setThreadName(threadName: String) {
        selectedThreadRepository.chatThreadHandler?.setName(threadName)
    }

    private fun showDialog(dialog: Dialogs) {
        showDialog.value = dialog
    }

    internal fun dismissDialog() {
        showDialog.value = Dialogs.None
    }

    internal fun editThreadName() {
        showDialog(EditThreadName)
    }

    internal fun confirmEditThreadName(name: String) {
        dismissDialog()

        setThreadName(name)
    }

    internal fun startEditingCustomValues() {
        showDialog(CustomValues)
    }

    internal fun confirmEditingCustomValues(values: CustomValueItemList) {
        dismissDialog()
        viewModelScope.launch(Dispatchers.Default) {
            chatThreadHandler.customFields().add(values.extractStringValues())
        }
    }

    internal fun cancelEditingCustomValues() {
        dismissDialog()
    }

    internal fun playAudio(url: String, title: String?) {
        showDialog(AudioPlayer(url, title))
    }

    internal fun selectAttachments(attachments: List<Attachment>, title: String?) {
        showDialog(SelectAttachments(attachments, title))
    }

    internal fun beginPrepareAttachments() {
        preparing.value = true
    }

    internal fun finishPrepareAttachments() {
        preparing.value = false
    }

    internal fun showImage(image: Any, title: String?) {
        showDialog(Dialogs.ImageViewer(image, title))
    }

    internal fun showVideo(url: String, title: String?) {
        showDialog(Dialogs.VideoPlayer(url, title))
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
    override val author: MessageAuthor?,
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
            override val seenAt: Date? = null
            override val readAt: Date? = null
            override val status: MessageStatus = SENDING
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
            override val seenAt: Date? = null
            override val readAt: Date? = null
            override val status: MessageStatus = SENDING
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
