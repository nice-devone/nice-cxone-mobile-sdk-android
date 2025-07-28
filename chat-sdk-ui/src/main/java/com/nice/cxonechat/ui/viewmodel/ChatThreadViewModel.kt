/*
 * Copyright (c) 2021-2025. NICE Ltd. All rights reserved.
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

package com.nice.cxonechat.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nice.cxonechat.Chat
import com.nice.cxonechat.ChatActionHandler.OnPopupActionListener
import com.nice.cxonechat.ChatThreadEventHandlerActions.markThreadRead
import com.nice.cxonechat.ChatThreadEventHandlerActions.typingEnd
import com.nice.cxonechat.ChatThreadEventHandlerActions.typingStart
import com.nice.cxonechat.ChatThreadMessageHandler.OnMessageTransferListener
import com.nice.cxonechat.message.Attachment
import com.nice.cxonechat.message.ContentDescriptor
import com.nice.cxonechat.message.MessageAuthor
import com.nice.cxonechat.message.MessageDirection
import com.nice.cxonechat.message.MessageDirection.ToAgent
import com.nice.cxonechat.message.MessageMetadata
import com.nice.cxonechat.message.MessageStatus
import com.nice.cxonechat.message.MessageStatus.Sending
import com.nice.cxonechat.message.OutboundMessage
import com.nice.cxonechat.prechat.PreChatSurvey
import com.nice.cxonechat.thread.ChatThread
import com.nice.cxonechat.thread.ChatThreadState
import com.nice.cxonechat.thread.CustomField
import com.nice.cxonechat.ui.api.CustomFieldProviderType
import com.nice.cxonechat.ui.api.UiCustomFieldsProvider
import com.nice.cxonechat.ui.data.flow
import com.nice.cxonechat.ui.data.model.ChatMetadata.Companion.asMetadata
import com.nice.cxonechat.ui.data.model.PopupActionState
import com.nice.cxonechat.ui.data.model.PopupActionState.Empty
import com.nice.cxonechat.ui.data.model.PopupActionState.ReceivedPopupAction
import com.nice.cxonechat.ui.data.repository.SelectedThreadRepository
import com.nice.cxonechat.ui.data.source.ContentDataSourceList
import com.nice.cxonechat.ui.data.source.ContentDataSourceList.ContentRequestResult
import com.nice.cxonechat.ui.data.source.ContentDataSourceList.ContentRequestResult.Error
import com.nice.cxonechat.ui.domain.model.ChatThreadCopy.Companion.copy
import com.nice.cxonechat.ui.domain.model.CustomValueItemList
import com.nice.cxonechat.ui.domain.model.NoThread
import com.nice.cxonechat.ui.domain.model.Person
import com.nice.cxonechat.ui.domain.model.asPerson
import com.nice.cxonechat.ui.domain.model.extractStringValues
import com.nice.cxonechat.ui.util.isEmpty
import com.nice.cxonechat.ui.util.preview.message.SdkMessage
import com.nice.cxonechat.ui.util.preview.message.SdkText
import com.nice.cxonechat.ui.viewmodel.ChatThreadViewModel.Dialogs.CustomValues
import com.nice.cxonechat.ui.viewmodel.ChatThreadViewModel.Dialogs.EditThreadName
import com.nice.cxonechat.ui.viewmodel.ChatThreadViewModel.Dialogs.None
import com.nice.cxonechat.ui.viewmodel.ChatThreadViewModel.Dialogs.SelectAttachments
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.annotation.KoinViewModel
import org.koin.core.qualifier.named
import org.koin.java.KoinJavaComponent.inject
import java.lang.ref.WeakReference
import java.util.Date
import java.util.UUID
import com.nice.cxonechat.ui.data.source.ContentDataSourceList.ContentRequestResult.Success as RequestSuccess

@Suppress("TooManyFunctions")
@OptIn(ExperimentalCoroutinesApi::class)
@KoinViewModel
internal class ChatThreadViewModel(
    private val contentDataSource: ContentDataSourceList,
    private val selectedThreadRepository: SelectedThreadRepository,
    private val chat: Chat,
) : ViewModel() {
    private val threads by lazy { chat.threads() }

    private val uiCustomFieldsProvider: UiCustomFieldsProvider by inject(
        clazz = UiCustomFieldsProvider::class.java,
        qualifier = named(CustomFieldProviderType.Contact)
    )
    val isMultiThreadEnabled = chat.configuration.hasMultipleThreadsPerEndUser
    val isLiveChat = chat.configuration.isLiveChat
    val preChatSurvey: PreChatSurvey?
        get() = threads.preChatSurvey
    val hasQuestions: Boolean
        get() = preChatSurvey?.fields?.isEmpty() == false
    val chatThreadHandler = selectedThreadRepository.chatThreadHandlerFlow

    private var threadId: UUID? = null

    private val chatThreadFlow = chatThreadHandler
        .onEach {
            if (threadId != it.get().id) {
                // Reset the cached flow if the thread ID has changed and reset pending attachments when the thread changes.
                chatThreadCachedFlow.value = NoThread
                mutablePendingAttachments.value = emptyList()
            }
            threadNameOverride.value = null
        }
        .flatMapLatest { it.flow }
        .onEach { newThread ->
            val sentMessageThreadId = sentMessagesFlow.firstOrNull()?.asIterable()?.firstOrNull()?.value?.threadId
            if (newThread.id != sentMessageThreadId && sentMessagesFlow.value.isNotEmpty()) {
                // If the thread ID of the new thread does not match the thread ID of the sent messages, reset the sent messages cache.
                sentMessagesFlow.value = emptyMap()
            }
            if (threadId != newThread.id) {
                // Update the thread ID (can happen when a pending thread id was re-assigned by backend).
                threadId = newThread.id
            }
            // Update the cached flow with the new thread.
            // If the messages are the same, we can avoid copying the list to save memory.
            if (chatThreadCachedFlow.value.messages !== newThread.messages) {
                chatThreadCachedFlow.value = newThread.copy(messages = newThread.messages.toList())
            } else {
                chatThreadCachedFlow.value = newThread
            }
        }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.Lazily, NoThread)

    /**
     * Cached flow of the current chat thread. This is used to avoid unnecessary recomputations
     * and to provide a consistent view of the thread state, while also allowing for
     * reset of the cache when the thread changes.
     */
    private val chatThreadCachedFlow: MutableStateFlow<ChatThread> = MutableStateFlow(NoThread)

    /** Tracks messages before they are confirmed as received by backend. */
    private val sentMessagesFlow: MutableStateFlow<Map<UUID, SdkMessage>> = MutableStateFlow(emptyMap())

    private val receivedMessagesFlow: Flow<Map<UUID, SdkMessage>> = chatThreadCachedFlow
        .mapLatest { chatThread -> chatThread.messages }
        .distinctUntilChanged()
        .map { messages -> messages.associateBy(SdkMessage::id) }

    val messages: Flow<List<SdkMessage>> = sentMessagesFlow
        .combine(receivedMessagesFlow) { sentMessageMap, chatUpdatePlusMessageMap ->
            sentMessageMap.plus(
                chatUpdatePlusMessageMap
            ).values
        }
        .map { collection -> collection.sortedByDescending { message -> message.createdAt } }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())

    val currentAgent: StateFlow<Person?> = chatThreadCachedFlow
        .mapLatest { chatThread -> chatThread.threadAgent?.asPerson }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)

    val positionInQueue: StateFlow<Int?> = chatThreadCachedFlow
        .mapLatest { chatThread -> chatThread.positionInQueue }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)

    private val chatMetadataInternalFlow = chatThreadCachedFlow
        .map { chatThread ->
            chatThread.asMetadata(isMultiThreadEnabled)
        }
        .shareIn(viewModelScope, SharingStarted.WhileSubscribed(), 0)

    val chatMetadata = chatMetadataInternalFlow.filterNotNull()

    val assignedAgentFlow = chatMetadata.map { it.agent }

    private val threadNameOverride = MutableStateFlow<String?>(null)
    val threadNameFlow = combine(threadNameOverride, chatMetadata) { override, metadata ->
        override ?: metadata.threadName
    }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)

    val canLoadMore: StateFlow<Boolean> = chatThreadCachedFlow
        .map { it.hasMoreMessagesToLoad }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), false)

    val isAgentTyping: StateFlow<Boolean> = chatThreadFlow
        .map { it.threadAgent?.isTyping == true }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), false)

    private val mutableActionState: MutableStateFlow<PopupActionState> = MutableStateFlow(Empty)
    private val actionHandler = chat.actions()
    private val messageHandler = chatThreadHandler.mapLatest { it.messages() }
    private val eventHandler = chatThreadHandler.mapLatest { it.events() }

    val customValues: List<CustomField>
        get() = selectedThreadRepository.chatThreadHandler.get().fields

    val selectedThreadName: String?
        get() = selectedThreadRepository.chatThreadHandler.get().threadName

    val isArchived: StateFlow<Boolean> = chatThreadCachedFlow
        .mapLatest { !it.canAddMoreMessages }
        .distinctUntilChanged()
        .onEach { isArchived ->
            if (isLiveChat) {
                if (isArchived) {
                    showEndContactDialog()
                } else if (Dialogs.EndContact == dialogShown.value) {
                    dismissDialog()
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), false)

    val threadStateFlow: StateFlow<ChatThreadState> = chatThreadCachedFlow
        .mapLatest { it.threadState }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), ChatThreadState.Pending)

    private val mutablePendingAttachments = MutableStateFlow<List<PreparedAttachment>>(emptyList())

    internal val pendingAttachments: StateFlow<List<PreparedAttachment>> = mutablePendingAttachments.asStateFlow()

    sealed interface Dialogs {
        sealed interface FullScreenDialog : Dialogs
        data object None : Dialogs
        data object CustomValues : Dialogs
        data object EditThreadName : Dialogs

        data class SelectAttachments(
            val attachments: List<Attachment>,
        ) : Dialogs

        data class VideoPlayer(
            val uri: String,
            val title: String?,
            val attachment: Attachment,
        ) : FullScreenDialog

        data class ImageViewer(
            val image: Any?,
            val title: String?,
            val attachment: Attachment,
        ) : FullScreenDialog

        data class InvalidAttachments(
            val attachments: Map<Uri, String>,
        ) : Dialogs

        data object EndContact : Dialogs
    }

    private val showDialog = MutableStateFlow<Dialogs>(None)
    val dialogShown = showDialog.asStateFlow()

    // Note this is explicitly *not* part of Dialogs to allow it to be stacked over the select attachments dialog.
    private val preparing = MutableStateFlow(false)
    val preparingToShare = preparing.asStateFlow()

    init {
        val listener = OnPopupActionListener { variables, metadata ->
            mutableActionState.value = ReceivedPopupAction(variables, metadata)
        }
        actionHandler.onPopup(listener)
    }

    fun refresh() {
        viewModelScope.launch {
            chatThreadHandler.first().refresh()
        }
    }

    fun sendMessageWithAttachments(message: OutboundMessage) {
        if (message.attachments.isEmpty() && message.postback.isNullOrBlank()) {
            if (pendingAttachments.value.isNotEmpty()) {
                sendAttachments(pendingAttachments.value, message.message)
                return
            } else if (message.message.isBlank()) {
                // Ignore messages with no text, attachments, or postback.
                return
            }
        }

        val appMessage: (UUID) -> SdkMessage = { id ->
            TemporarySentMessage(
                id = id,
                text = message.message,
            )
        }
        val listener = OnMessageSentListener(appMessage, sentMessagesFlow)
        viewModelScope.launch {
            messageHandler.first().send(message, listener)
        }
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

    fun sendAttachment(attachment: Uri) {
        prepareAttachmentsAndTrigger(listOf(attachment)) { contents -> sendAttachments(contents, null) }
    }

    fun addPendingAttachments(attachments: List<Uri>) {
        prepareAttachmentsAndTrigger(attachments) { contents ->
            // Combine new attachments with existing ones, avoiding duplicates.
            val currentAttachments = mutablePendingAttachments.value
            val newAttachments = contents.filterNot { newAttachment ->
                currentAttachments.any { existingAttachment -> existingAttachment.url == newAttachment.url }
            }
            mutablePendingAttachments.value = currentAttachments + newAttachments
        }
    }

    private fun prepareAttachmentsAndTrigger(
        attachments: List<Uri>,
        trigger: (List<PreparedAttachment>) -> Unit,
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val (requestResults, requestErrors) = attachments.map { uri ->
                contentDataSource.descriptorForUri(uri)
            }.partition { it is RequestSuccess }
            if (requestErrors.isNotEmpty()) {
                val errors = requestErrors.associate { result: ContentRequestResult ->
                    require(result is Error)
                    result.uri to result.cause
                }
                showDialog(Dialogs.InvalidAttachments(errors))
            } else {
                val contents = requestResults.map { result: ContentRequestResult ->
                    require(result is RequestSuccess)
                    PreparedAttachment(
                        contentDescriptor = result.content,
                        url = result.uri.toString(),
                    )
                }
                trigger(contents)
            }
        }
    }

    private fun sendAttachments(attachments: List<PreparedAttachment>, message: String?) {
        val appMessage: (UUID) -> SdkMessage = { id ->
            TemporarySentMessage(
                id = id,
                attachments = attachments,
                text = message.orEmpty(),
            )
        }
        val contentDescriptors = attachments.map(PreparedAttachment::contentDescriptor)
        val listener = OnMessageSentListener(appMessage, sentMessagesFlow)
        viewModelScope.launch {
            messageHandler.first().send(
                OutboundMessage(
                    attachments = contentDescriptors,
                    message = message.orEmpty()
                ),
                listener = listener,
            )
            mutablePendingAttachments.value -= attachments
        }
    }

    fun removePendingAttachment(attachment: Attachment) {
        mutablePendingAttachments.value = pendingAttachments.value.filterNot { it === attachment || it.url == attachment.url }
    }

    fun loadMore() {
        viewModelScope.launch {
            messageHandler.first().loadMore()
        }
    }

    fun reportThreadRead() {
        viewModelScope.launch {
            eventHandler.first().markThreadRead()
        }
    }

    fun reportTypingStarted() {
        viewModelScope.launch {
            eventHandler.first().typingStart()
        }
    }

    fun reportTypingEnd() {
        viewModelScope.launch {
            eventHandler.first().typingEnd()
        }
    }

    override fun onCleared() {
        actionHandler.close()
        super.onCleared()
    }

    internal fun setThreadName(threadName: String) {
        selectedThreadRepository.chatThreadHandler.setName(threadName)
    }

    private fun showDialog(dialog: Dialogs) {
        showDialog.value = dialog
    }

    internal fun dismissDialog() {
        showDialog.value = None
    }

    internal fun editThreadName() {
        showDialog(EditThreadName)
    }

    internal fun confirmEditThreadName(name: String) {
        dismissDialog()
        setThreadName(name)
        threadNameOverride.value = name
    }

    internal fun startEditingCustomValues() {
        showDialog(CustomValues)
    }

    internal fun confirmEditingCustomValues(values: CustomValueItemList) {
        dismissDialog()
        viewModelScope.launch(Dispatchers.Default) {
            chatThreadHandler.firstOrNull()?.customFields()?.add(
                values.extractStringValues() + withContext(Dispatchers.IO) {
                    uiCustomFieldsProvider.customFields()
                }
            )
        }
    }

    internal fun cancelEditingCustomValues() {
        dismissDialog()
    }

    internal fun selectAttachments(attachments: List<Attachment>) {
        showDialog(SelectAttachments(attachments))
    }

    internal fun beginPrepareAttachments() {
        preparing.value = true
    }

    internal fun finishPrepareAttachments() {
        preparing.value = false
    }

    internal fun showImage(image: Any, title: String?, attachment: Attachment) {
        showDialog(Dialogs.ImageViewer(image, title, attachment))
    }

    internal fun showVideo(url: String, title: String?, attachment: Attachment) {
        showDialog(Dialogs.VideoPlayer(url, title, attachment))
    }

    internal fun endContact() {
        viewModelScope.launch {
            chatThreadHandler.firstOrNull()?.endContact()
        }
    }

    internal fun showEndContactDialog() {
        showDialog(Dialogs.EndContact)
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
) : SdkText() {
    constructor(id: UUID, text: String) : this(
        id = id,
        threadId = UUID.randomUUID(),
        createdAt = Date(),
        direction = ToAgent,
        metadata = object : MessageMetadata {
            override val seenAt: Date? = null
            override val readAt: Date? = null
            override val seenByCustomerAt: Date? = null
            override val status: MessageStatus = Sending
        },
        author = object : MessageAuthor() {
            override val id: String = SENDER_ID
            override val firstName: String = ""
            override val lastName: String = ""
            override val imageUrl: String? = null
        },
        attachments = emptyList(),
        fallbackText = null,
        text = text
    )

    constructor(id: UUID, text: String, attachments: List<Attachment>) : this(
        id = id,
        threadId = UUID.randomUUID(), // Will be overwritten by the backend
        createdAt = Date(),
        direction = ToAgent,
        metadata = object : MessageMetadata {
            override val seenAt: Date? = null
            override val readAt: Date? = null
            override val seenByCustomerAt: Date? = null
            override val status: MessageStatus = Sending
        },
        author = object : MessageAuthor() {
            override val id: String = SENDER_ID // Will be overwritten by the backend
            override val firstName: String = ""
            override val lastName: String = ""
            override val imageUrl: String? = null
        },
        attachments = attachments,
        fallbackText = null,
        text = text
    )

    private companion object {
        private const val SENDER_ID = "com.cxone.chat.message.sender.1"
    }
}

internal data class PreparedAttachment(
    val contentDescriptor: ContentDescriptor,
    override val url: String,
) : Attachment {
    override val friendlyName: String = contentDescriptor.friendlyName ?: "unnamed"
    override val mimeType: String = contentDescriptor.mimeType
}
