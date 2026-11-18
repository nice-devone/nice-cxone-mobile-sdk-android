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

package com.nice.cxonechat.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nice.cxonechat.ChatInstanceProvider
import com.nice.cxonechat.ChatThreadEventHandlerActions.markThreadRead
import com.nice.cxonechat.ChatThreadEventHandlerActions.selectTimeSlot
import com.nice.cxonechat.ChatThreadEventHandlerActions.sendTranscript
import com.nice.cxonechat.ChatThreadEventHandlerActions.triggerAction
import com.nice.cxonechat.ChatThreadEventHandlerActions.typingEnd
import com.nice.cxonechat.ChatThreadEventHandlerActions.typingStart
import com.nice.cxonechat.ChatThreadHandler
import com.nice.cxonechat.EventResponse
import com.nice.cxonechat.Popup
import com.nice.cxonechat.exceptions.CXoneException
import com.nice.cxonechat.log.Logger
import com.nice.cxonechat.log.LoggerScope
import com.nice.cxonechat.log.debug
import com.nice.cxonechat.log.scope
import com.nice.cxonechat.log.timedScope
import com.nice.cxonechat.log.verbose
import com.nice.cxonechat.log.warning
import com.nice.cxonechat.message.Attachment
import com.nice.cxonechat.message.ContentDescriptor
import com.nice.cxonechat.message.MessageAuthor
import com.nice.cxonechat.message.MessageDirection
import com.nice.cxonechat.message.MessageDirection.ToAgent
import com.nice.cxonechat.message.MessageMetadata
import com.nice.cxonechat.message.MessageStatus
import com.nice.cxonechat.message.MessageStatus.Sending
import com.nice.cxonechat.message.OutboundMessage
import com.nice.cxonechat.message.TimeSlot
import com.nice.cxonechat.prechat.PreChatSurvey
import com.nice.cxonechat.state.Configuration
import com.nice.cxonechat.thread.ChatThread
import com.nice.cxonechat.thread.ChatThreadState
import com.nice.cxonechat.thread.ContactHandler
import com.nice.cxonechat.thread.CustomField
import com.nice.cxonechat.ui.UiModule
import com.nice.cxonechat.ui.api.CustomFieldProviderType
import com.nice.cxonechat.ui.api.UiCustomFieldsProvider
import com.nice.cxonechat.ui.data.model.ChatMetadata.Companion.asMetadata
import com.nice.cxonechat.ui.data.repository.SelectedThreadRepository
import com.nice.cxonechat.ui.data.source.ContentDataSourceList
import com.nice.cxonechat.ui.data.source.ContentDataSourceList.ContentRequestResult
import com.nice.cxonechat.ui.data.source.ContentDataSourceList.ContentRequestResult.Error
import com.nice.cxonechat.ui.data.source.SentAttachmentSeeder
import com.nice.cxonechat.ui.domain.model.ChatThreadCopy.Companion.copy
import com.nice.cxonechat.ui.domain.model.CustomValueItemList
import com.nice.cxonechat.ui.domain.model.NoThread
import com.nice.cxonechat.ui.domain.model.NoThreadHandler
import com.nice.cxonechat.ui.domain.model.Person
import com.nice.cxonechat.ui.domain.model.asPerson
import com.nice.cxonechat.ui.domain.model.extractStringValues
import com.nice.cxonechat.ui.util.isEmpty
import com.nice.cxonechat.ui.util.preview.message.SdkMessage
import com.nice.cxonechat.ui.util.preview.message.SdkReplyButton
import com.nice.cxonechat.ui.util.preview.message.SdkText
import com.nice.cxonechat.ui.viewmodel.ConversationDialog.CustomValues
import com.nice.cxonechat.ui.viewmodel.ConversationDialog.EditThreadName
import com.nice.cxonechat.ui.viewmodel.ConversationDialog.EndContact
import com.nice.cxonechat.ui.viewmodel.ConversationDialog.EndContactConfirmation
import com.nice.cxonechat.ui.viewmodel.ConversationDialog.None
import com.nice.cxonechat.ui.viewmodel.ConversationDialog.SelectAttachments
import com.nice.cxonechat.utilities.isEmpty
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.annotation.KoinViewModel
import org.koin.core.annotation.Named
import org.koin.core.annotation.Provided
import org.koin.core.qualifier.named
import org.koin.java.KoinJavaComponent.inject
import java.util.UUID
import kotlin.time.Clock
import kotlin.time.Instant
import com.nice.cxonechat.ui.data.source.ContentDataSourceList.ContentRequestResult.Success as RequestSuccess

@Suppress(
    "TooManyFunctions",
    "LargeClass"
)
@OptIn(ExperimentalCoroutinesApi::class)
@KoinViewModel
internal class ChatThreadViewModel(
    private val contentDataSource: ContentDataSourceList,
    private val selectedThreadRepository: SelectedThreadRepository,
    private val chatProvider: ChatInstanceProvider,
    @Provided
    @Named(UiModule.LOGGER_NAME)
    internal val logger: Logger,
    private val sentAttachmentSeeder: SentAttachmentSeeder,
) : ViewModel(), LoggerScope by LoggerScope("ChatThreadViewModel", logger) {
    // The Chat instance is session-scoped and rebuilt by the provider on reconnect, while this
    // ViewModel is activity-scoped and outlives sessions, so it is always reached through
    // chatProvider.chat (never captured) and via null-safe accessors below.
    private val uiCustomFieldsProvider: UiCustomFieldsProvider by inject(
        clazz = UiCustomFieldsProvider::class.java,
        qualifier = named(CustomFieldProviderType.Contact)
    )

    // Channel configuration is invariant for a channel (unchanged across session rebuilds), so it is
    // safe to cache. These flags are read both before the first Chat instance has finished building
    // (slow init) and by long-lived flows that outlive a session (e.g. signOut() briefly leaves
    // chatProvider.chat null). A plain requireNotNull(chat) accessor crashes on both ends; a
    // null-safe cached accessor prefers the live session's configuration and falls back to the last
    // known value, defaulting to false only until the first Chat instance is available.
    // NOTE: the structural resolution — an activity-scoped ViewModel no longer reaching into the
    // session-scoped ChatInstanceProvider — is owned by the UI architecture refactor tickets.
    private var cachedConfiguration: Configuration? = null
    private val chatConfiguration: Configuration?
        get() = (chatProvider.chat?.configuration ?: cachedConfiguration)?.also { cachedConfiguration = it }
    val isMultiThreadEnabled: Boolean
        get() = chatConfiguration?.hasMultipleThreadsPerEndUser ?: false
    val isLiveChat: Boolean
        get() = chatConfiguration?.isLiveChat ?: false

    // Same null-safety as the configuration flags: preChatSurvey (and hasQuestions, read eagerly by
    // composables such as the top bar) is channel-invariant, so resolve it through a null-safe cached
    // accessor instead of the requireNotNull(chat) `threads` getter — otherwise reading it while the
    // Chat is still building (slow init) or after signOut() nulls it (teardown) would crash.
    private var cachedPreChatSurvey: PreChatSurvey? = null
    val preChatSurvey: PreChatSurvey?
        get() = (chatProvider.chat?.threads()?.preChatSurvey ?: cachedPreChatSurvey)?.also { cachedPreChatSurvey = it }
    val hasQuestions: Boolean
        get() = preChatSurvey?.fields?.isEmpty() == false
    val chatThreadHandler = selectedThreadRepository.chatThreadHandlerFlow

    private var threadId: UUID? = null

    private val chatThreadFlow = chatThreadHandler
        .onEach {
            if (threadId != it.get().id) {
                // Reset the cached flow if the thread ID has changed and reset pending attachments when the thread changes.
                chatThreadCachedFlow.value = NoThread
                // On Main, so it cannot land between the check and the write in addPendingAttachments.
                withContext(Dispatchers.Main) { mutablePendingAttachments.value = emptyList() }
                // Clear the EndContact dialog flag on genuine thread change (not initial setup).
                // Only relevant for LiveChat mode where the EndContact dialog is used.
                if (threadId != null && isLiveChat) {
                    selectedThreadRepository.withEndContactLock {
                        selectedThreadRepository.bestEffortEndContactUpdate { selectedThreadRepository.setEndContactShownForThread(null) }
                    }
                }
            }
            threadNameOverride.value = null
        }
        .flatMapLatest { it.threadFlow }
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
            // Moves the local copies of sent PDFs before the echo is shown, so its preview needs no download. This also
            // runs when a send registers its copies, so an echo that arrived before that is still adopted.
            sentAttachmentSeeder.adoptEchoes(chatUpdatePlusMessageMap)
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
        .mapLatest { chatThread ->
            // Hidden while an agent is associated, or while a handler other than CXone holds the contact.
            if (chatThread.threadAgent != null || chatThread.isHandledByNonCxone) null else chatThread.positionInQueue
        }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)

    private val ChatThread.isHandledByNonCxone: Boolean
        get() = contactHandler != null && contactHandler != ContactHandler.CXone

    private val chatMetadataInternalFlow = chatThreadCachedFlow
        .map { chatThread ->
            chatThread.asMetadata(isMultiThreadEnabled)
        }
        .shareIn(viewModelScope, SharingStarted.WhileSubscribed(), 0)

    val chatMetadata = chatMetadataInternalFlow.filterNotNull()

    val assignedAgentFlow = chatMetadata.map { it.agent }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /**
     * Resolves the display name for the assigned agent: prefers nickname (if non-blank),
     * falls back to fullName, or null if neither is available or no agent is assigned.
     */
    val agentNameFlow: Flow<String?> = assignedAgentFlow
        .map { agent -> agent?.nickname?.ifBlank { null } ?: agent?.fullName?.ifBlank { null } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)

    /** True when an agent is currently assigned to the thread, regardless of whether the agent's name is visible. */
    val hasAgentFlow: Flow<Boolean> = assignedAgentFlow
        .map { it != null }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), false)

    private val threadNameOverride = MutableStateFlow<String?>(null)
    val threadNameFlow = combine(threadNameOverride, chatMetadata) { override, metadata ->
        override ?: metadata.threadName
    }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)

    val canLoadMore: StateFlow<Boolean> = chatThreadCachedFlow
        .map { it.hasMoreMessagesToLoad }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), false)

    val hasAiMessages: StateFlow<Boolean> = messages
        .map { list -> list.any { isAiAgentMessage(it) } }
        .distinctUntilChanged()
        .catch { e ->
            warning("hasAiMessages flow error", e)
            emit(false)
        }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), false)

    val isAgentTyping: StateFlow<Boolean> = chatThreadFlow
        .map { it.threadAgent?.isTyping == true }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), false)

    private val messageHandler = chatThreadHandler.mapLatest { it.messages() }
    private val eventHandler = chatThreadHandler.mapLatest { it.events() }

    private val _showMessageProcessing: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val showMessageProcessing = _showMessageProcessing.asStateFlow()

    val customValues: List<CustomField>
        get() = selectedThreadRepository.chatThreadHandler.get().fields

    val selectedThreadName: String?
        get() = selectedThreadRepository.chatThreadHandler.get().threadName

    val liveChatAllowTranscript: Boolean
        get() = chatConfiguration?.liveChatAllowTranscript ?: false
    val isArchived: StateFlow<Boolean> = chatThreadCachedFlow
        .mapLatest { !it.canAddMoreMessages }
        .distinctUntilChanged()
        .onEach { isArchived ->
            if (isLiveChat) {
                val currentThreadId = threadId?.toString()
                selectedThreadRepository.withEndContactLock {
                    selectedThreadRepository.bestEffortEndContactUpdate {
                        val alreadyMarked = selectedThreadRepository.getEndContactShownForThread() == currentThreadId
                        if (isArchived && !alreadyMarked && !isSatisfactionSurveyShowing()) {
                            selectedThreadRepository.setEndContactShownForThread(currentThreadId)
                            // The write above suspends; a survey can arrive while it's in flight. Re-check
                            // before overwriting it - undo the mark instead so it shows later.
                            if (isSatisfactionSurveyShowing()) {
                                selectedThreadRepository.setEndContactShownForThread(null)
                            } else {
                                showEndContactDialog()
                            }
                        } else if (!isArchived && EndContact == dialogShown.value) {
                            dismissDialog()
                        }
                    }
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), false)

    val threadStateFlow: StateFlow<ChatThreadState> = chatThreadCachedFlow
        .mapLatest { it.threadState }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), ChatThreadState.Pending)

    private val mutablePendingAttachments = MutableStateFlow<List<PreparedAttachment>>(emptyList())

    internal val pendingAttachments: StateFlow<List<PreparedAttachment>> = mutablePendingAttachments.asStateFlow()

    private val showDialog = MutableStateFlow<ConversationDialog>(None)
    val dialogShown = showDialog.asStateFlow()

    // This view model is activity-scoped and outlives navigating back to ThreadList, where
    // ThreadDialogView (and so this dialog) is not composed - without tracking that, a survey
    // received while away from this screen would mark itself "showing" with nothing on screen to
    // show it, blocking ChatThreadsViewModel.createThread()'s wait indefinitely.
    private val isDialogHostAttached = MutableStateFlow(false)

    // A survey is only ever delivered once, so switching threads and back must restore it rather
    // than let the thread-switch logic below discard the only copy ever shown.
    private val pendingSurveysByThread = mutableMapOf<UUID, Popup.SatisfactionSurvey>()

    private val _errorEvents = MutableSharedFlow<String>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val errorEvents: SharedFlow<String> = _errorEvents.asSharedFlow()

    private val _showTranscriptDialog = MutableStateFlow<SendTranscriptDialog>(SendTranscriptDialog.None)
    val showTranscriptDialog = _showTranscriptDialog.asStateFlow()

    private val _showFullScreenLoading = MutableStateFlow<Boolean>(false)
    val showFullScreenLoading: StateFlow<Boolean> = _showFullScreenLoading.asStateFlow()

    // Note this is explicitly *not* part of Dialogs to allow it to be stacked over the select attachments dialog.
    private val preparing = MutableStateFlow(false)
    val preparingToShare = preparing.asStateFlow()

    init {
        viewModelScope.launch {
            chatThreadHandler.collectLatest { handler ->
                val currentThreadId = handler.get().id
                // A dialog left over from the previous handler belongs to its thread, not this one;
                // restore from pendingSurveysByThread instead of only clearing to None, so a survey
                // already received for this thread is not lost on returning to it.
                val restoredSurvey = pendingSurveysByThread[currentThreadId]
                showDialog.value = restoredSurvey
                    ?.let { ConversationDialog.Popup(popup = it, currentThreadId) }
                    ?: None
                if (restoredSurvey != null) {
                    // isArchived's auto-EndContact can race ahead of this restore and mark the
                    // thread shown before the survey ever appeared - undo it, same as the fresh
                    // delivery path below does, or dismissSatisfactionSurvey() would skip EndContact.
                    selectedThreadRepository.withEndContactLock {
                        selectedThreadRepository.bestEffortEndContactUpdate {
                            if (selectedThreadRepository.getEndContactShownForThread() == currentThreadId.toString()) {
                                selectedThreadRepository.setEndContactShownForThread(null)
                            }
                        }
                    }
                }
                // collectLatest cancels this block (and its popup collection) when a new handler
                // arrives, so switching threads/sessions stops the old popup flow automatically.
                // A survey consumed in the background (ChatThreadsViewModel.reconcileBackgroundSurveys,
                // to post a local notification) is stashed in SelectedThreadRepository, since
                // popupFlow's one-shot delivery means this fresh collection would otherwise never
                // see it again. Recover it here so opening the thread still shows the dialog.
                // NonCancellable: a thread switch between takeSurvey() removing the persisted copy
                // and caching it below would otherwise lose the survey entirely.
                val recoveredSurvey = withContext(NonCancellable) {
                    selectedThreadRepository.takeSurvey(handler.get().id)
                        ?.also { pendingSurveysByThread[currentThreadId] = it }
                }
                val popups: Flow<Popup> = if (recoveredSurvey != null) {
                    merge(flowOf<Popup>(recoveredSurvey), handler.actions().popupFlow)
                } else {
                    handler.actions().popupFlow
                }
                popups.collect { popup ->
                    // Captured before publishing below - a handler/session replacement arriving
                    // while the undo write further down suspends would otherwise cancel this
                    // collectLatest block before the survey is ever shown.
                    val wasEndContactShown = popup is Popup.SatisfactionSurvey && EndContact == dialogShown.value
                    if (popup is Popup.SatisfactionSurvey) {
                        pendingSurveysByThread[currentThreadId] = popup
                        if (isDialogHostAttached.value) {
                            // Set synchronously - the reactive mirror below runs later and a racing
                            // pre-chat survey check could still see this as false in the gap.
                            selectedThreadRepository.setSatisfactionSurveyShowing(this@ChatThreadViewModel, true)
                        }
                    }
                    showDialog(ConversationDialog.Popup(popup = popup, currentThreadId))
                    if (wasEndContactShown) {
                        // EndContact can win a race and get shown just before the survey arrives
                        // and overwrites it; undo the mark so dismissSatisfactionSurvey() can
                        // still show it later. Best-effort now that the survey is already shown.
                        selectedThreadRepository.withEndContactLock {
                            selectedThreadRepository.bestEffortEndContactUpdate {
                                selectedThreadRepository.setEndContactShownForThread(null)
                            }
                        }
                    }
                }
            }
        }
        // Mirrors dialogShown and isDialogHostAttached together, so the shared flag self-corrects
        // on any later transition away from an actually presented survey - a dismissal, a thread
        // switch, or the dialog host leaving composition - not just the one path that happens to
        // clear dialogShown explicitly.
        viewModelScope.launch {
            combine(dialogShown, isDialogHostAttached) { dialog, attached ->
                isSatisfactionSurveyShowing(dialog) && attached
            }.collect { showing ->
                selectedThreadRepository.setSatisfactionSurveyShowing(this@ChatThreadViewModel, showing)
            }
        }
    }

    /** Toggled by [ThreadDialogView][com.nice.cxonechat.ui.composable.conversation.ThreadDialogView] as it enters/leaves composition. */
    internal fun setDialogHostAttached(attached: Boolean) {
        isDialogHostAttached.value = attached
    }

    /**
     * If cleared while the survey is showing, the collector above never sees the dialog change
     * away, leaving the shared flag stuck `true` and blocking every future pre-chat survey wait.
     */
    override fun onCleared() {
        super.onCleared()
        selectedThreadRepository.setSatisfactionSurveyShowing(this, false)
    }

    private fun isSatisfactionSurveyShowing(dialog: ConversationDialog = dialogShown.value): Boolean =
        (dialog as? ConversationDialog.Popup)?.popup is Popup.SatisfactionSurvey

    /**
     * Dismisses the Satisfaction Survey dialog and, if still archived, shows the End Conversation
     * Experience that [isArchived]'s auto-show was holding back.
     */
    internal fun dismissSatisfactionSurvey() = scope("dismissSatisfactionSurvey") {
        (dialogShown.value as? ConversationDialog.Popup)?.let { pendingSurveysByThread.remove(it.threadId) }
        selectedThreadRepository.setSatisfactionSurveyShowing(this@ChatThreadViewModel, false)
        showDialog.value = None
        // Reads the handler directly, not isArchived.value: isArchived only updates while
        // something collects it (e.g. ChatConversation's lifecycle-aware collector), which can
        // race this dismiss on a background resume and leave isArchived.value stale.
        if (isLiveChat && !selectedThreadRepository.chatThreadHandler.get().canAddMoreMessages) {
            viewModelScope.launch { showEndContactAfterDismiss(threadId?.toString()) }
        }
    }

    private suspend fun showEndContactAfterDismiss(currentThreadId: String?) {
        selectedThreadRepository.withEndContactLock {
            selectedThreadRepository.bestEffortEndContactUpdate {
                if (selectedThreadRepository.getEndContactShownForThread() != currentThreadId) {
                    selectedThreadRepository.setEndContactShownForThread(currentThreadId)
                    // The write above suspends; a survey can arrive, or (this job is activity-scoped
                    // and outlives the switch) a different thread can become selected, while it's in
                    // flight. Re-check both - undo the mark instead so it shows later - rather than
                    // publish EndContact over whichever thread is now on screen.
                    val stillSelected = selectedThreadRepository.chatThreadHandler.get().id.toString() == currentThreadId
                    if (isSatisfactionSurveyShowing() || !stillSelected) {
                        selectedThreadRepository.setEndContactShownForThread(null)
                    } else {
                        showDialog.value = EndContact
                    }
                }
            }
        }
    }

    fun refresh() = timedScope("refresh") {
        viewModelScope.launch {
            chatThreadHandler.first().refresh()
        }
    }

    fun sendMessageWithAttachments(message: OutboundMessage): Unit = timedScope("sendMessageWithAttachments") {
        if (message.attachments.isEmpty() && message.postback.isNullOrBlank()) {
            if (pendingAttachments.value.isNotEmpty()) {
                sendAttachments(pendingAttachments.value, message.message)
                return
            } else if (message.message.isBlank()) {
                warning("Dropping completely empty message.")
                // Ignore messages with no text, attachments, or postback.
                return
            }
        }

        val appMessage: (UUID) -> SdkMessage = { id ->
            TemporarySentMessage(
                id = id,
                threadId = threadId ?: UUID.randomUUID(),
                text = message.message,
            )
        }
        viewModelScope.launch {
            try {
                _showMessageProcessing.update { true }
                try {
                    val id = messageHandler.first().send(message)
                    val msg = appMessage(UUID.fromString(id))
                    sentMessagesFlow.value = sentMessagesFlow.value.plus(msg.id to msg)
                    verbose("Message sent with id: $id")
                } finally {
                    _showMessageProcessing.update { false }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (logged: Exception) {
                warning("Failed to send message", logged)
            }
        }
    }

    fun sendAttachment(attachment: Uri) = timedScope("sendAttachment") {
        prepareAttachmentsAndTrigger(listOf(attachment)) { contents -> sendAttachments(contents, null) }
    }

    /**
     * The session and thread an attachment pick is made for now; pass it to [addPendingAttachments].
     * The session is read first, so a sign-out between the reads makes the target stale rather than mismatched.
     */
    internal fun currentAttachmentTarget(): AttachmentTarget {
        val generation = selectedThreadRepository.currentSessionGeneration
        val handler = chatThreadHandler.value.takeUnless { it === NoThreadHandler }
        return AttachmentTarget(generation, handler, handler?.get()?.id)
    }

    /**
     * Whether [target] is still what the composer shows: the session generation is unchanged, and the thread it was
     * picked for (if one was known yet) is still selected — the same handler (its thread may have been re-assigned an
     * id) or the same thread under a recreated handler. A re-assigned id followed by a recreated handler matches
     * neither, so that rare case drops the pick. A failed, cancelled or retried prepare also counts as a new session.
     */
    private fun isCurrent(target: AttachmentTarget): Boolean {
        val selected = chatThreadHandler.value
        return target.sessionGeneration == selectedThreadRepository.currentSessionGeneration &&
            (target.handler == null || selected === target.handler || selected.get().id == target.threadId)
    }

    /**
     * Adds [attachments] to the composer, unless once they are prepared the composer no longer shows the session
     * and thread of [target]. A sign-out landing after that check is still covered: the next session's first
     * thread selection clears the composer.
     */
    fun addPendingAttachments(attachments: List<Uri>, target: AttachmentTarget) = timedScope("addPendingAttachments") {
        prepareAttachmentsAndTrigger(attachments, showErrorsIf = { isCurrent(target) }) { contents ->
            withContext(Dispatchers.Main) { // Assign on main thread to avoid concurrency issues on some devices
                if (!isCurrent(target)) {
                    warning("Dropping attachments picked for a session or thread the composer no longer shows")
                    return@withContext
                }
                // Combine new attachments with existing ones, avoiding duplicates.
                val currentAttachments = mutablePendingAttachments.value
                val newAttachments = contents.filterNot { newAttachment ->
                    currentAttachments.any { existingAttachment -> existingAttachment.url == newAttachment.url }
                }
                verbose("New attachments: $newAttachments, existing attachments: $currentAttachments")
                mutablePendingAttachments.value = currentAttachments + newAttachments
            }
        }
    }

    /**
     * Prepares [attachments], then passes them to [trigger], or shows the preparation errors when [showErrorsIf]
     * still holds, so a stale pick's errors (which list its file URIs) never reach another session.
     */
    private fun prepareAttachmentsAndTrigger(
        attachments: List<Uri>,
        showErrorsIf: () -> Boolean = { true },
        trigger: suspend (List<PreparedAttachment>) -> Unit,
    ) = scope("prepareAttachmentsAndTrigger") {
        viewModelScope.launch(Dispatchers.IO) {
            val (requestResults, requestErrors) = attachments.map { uri ->
                contentDataSource.descriptorForUri(uri)
            }.partition { it is RequestSuccess }
            if (requestErrors.isNotEmpty()) {
                verbose("Some attachments failed to prepare: $requestErrors")
                val errors = requestErrors.associate { result: ContentRequestResult ->
                    require(result is Error)
                    result.uri to result.cause
                }
                // On Main, so the dialog reset on a thread change cannot land between the check and the show.
                withContext(Dispatchers.Main) {
                    if (showErrorsIf()) {
                        showDialog(ConversationDialog.InvalidAttachments(errors))
                    } else {
                        warning("Dropping preparation errors of attachments picked for a session or thread no longer shown")
                    }
                }
            } else {
                verbose("Prepared attachments successfully: $requestResults")
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

    private fun sendAttachments(attachments: List<PreparedAttachment>, message: String?) = scope("sendAttachments") {
        val appMessage: (UUID) -> SdkMessage = { id ->
            TemporarySentMessage(
                id = id,
                threadId = threadId ?: UUID.randomUUID(),
                attachments = attachments,
                text = message.orEmpty(),
            )
        }
        val contentDescriptors = attachments.map(PreparedAttachment::contentDescriptor)
        viewModelScope.launch(Dispatchers.IO) { // For attachment use IO thread to upload it on server
            var seeds = emptyList<SentAttachmentSeeder.Seed>()
            try {
                debug("Sending message with attachments: $attachments")
                _showMessageProcessing.update { true }
                try {
                    seeds = sentAttachmentSeeder.seed(attachments)
                    val id = messageHandler.first().send(
                        OutboundMessage(
                            attachments = contentDescriptors,
                            message = message.orEmpty()
                        ),
                    )
                    sentAttachmentSeeder.register(UUID.fromString(id), seeds)
                    seeds = emptyList() // The echo owns them now.
                    val msg = appMessage(UUID.fromString(id))
                    sentMessagesFlow.value = sentMessagesFlow.value.plus(msg.id to msg)
                    verbose("Message sent with id: $id")
                } finally {
                    _showMessageProcessing.update { false }
                }
                mutablePendingAttachments.value -= attachments
                verbose("Attachments sent and removed from pending list.")
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (logged: Exception) {
                warning("Failed to send message with attachments", logged)
            } finally {
                withContext(NonCancellable) { sentAttachmentSeeder.discard(seeds) }
            }
        }
    }

    fun removePendingAttachment(attachment: Attachment) = scope("removePendingAttachment") {
        verbose("Removing pending attachment: $attachment")
        mutablePendingAttachments.value = pendingAttachments.value.filterNot { it === attachment || it.url == attachment.url }
    }

    fun loadMore() {
        viewModelScope.launch {
            messageHandler.first().loadMore()
        }
    }

    fun reportThreadRead() {
        viewModelScope.launch {
            try {
                eventHandler.first().markThreadRead()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (logged: Exception) {
                warning("Failed to report thread read", logged)
            }
        }
    }

    fun reportTypingStarted() {
        viewModelScope.launch {
            try {
                eventHandler.first().typingStart()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (logged: Exception) {
                warning("Failed to report typing started", logged)
            }
        }
    }

    fun reportTypingEnd() {
        viewModelScope.launch {
            try {
                eventHandler.first().typingEnd()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (logged: Exception) {
                warning("Failed to report typing end", logged)
            }
        }
    }

    internal fun setThreadName(threadName: String) {
        selectedThreadRepository.chatThreadHandler.setName(threadName)
    }

    private fun showDialog(dialog: ConversationDialog) = scope("showDialog") {
        verbose("Show dialog set to: $dialog")
        showDialog.value = dialog
    }

    internal fun dismissDialog() = scope("dismissDialog") {
        verbose("Show dialog dismissed")
        showDialog.value = None
    }

    internal fun editThreadName() = scope("editThreadName") {
        showDialog(EditThreadName)
    }

    internal fun confirmEditThreadName(name: String) = scope("confirmEditThreadName") {
        dismissDialog()
        setThreadName(name)
        threadNameOverride.value = name
    }

    internal fun startEditingCustomValues() = scope("startEditingCustomValues") {
        showDialog(CustomValues)
    }

    internal fun confirmEditingCustomValues(values: CustomValueItemList) = scope("confirmEditingCustomValues") {
        dismissDialog()
        viewModelScope.launch(Dispatchers.Default) {
            try {
                chatThreadHandler.firstOrNull()?.customFields()?.add(
                    values.extractStringValues() + withContext(Dispatchers.IO) {
                        uiCustomFieldsProvider.customFields()
                    }
                )
            } catch (expected: CXoneException) {
                warning("Failed to update custom fields", expected)
                val message = expected.localizedMessage?.takeIf { it.isNotBlank() }
                    ?: expected.message?.takeIf { it.isNotBlank() }
                    ?: "An unexpected error occurred while updating custom fields."
                _errorEvents.tryEmit(message)
            }
        }
    }

    internal fun cancelEditingCustomValues() = scope("cancelEditingCustomValues") {
        dismissDialog()
    }

    internal fun selectAttachments(attachments: List<Attachment>) = scope("selectAttachments") {
        showDialog(SelectAttachments(attachments))
    }

    internal fun beginPrepareAttachments() {
        preparing.value = true
    }

    internal fun finishPrepareAttachments() {
        preparing.value = false
    }

    internal fun showImage(image: Any, title: String?, attachment: Attachment) = scope("showImage") {
        showDialog(ConversationDialog.ImageViewer(image, title, attachment))
    }

    internal fun showVideo(url: String, title: String?, attachment: Attachment) = scope("showVideo") {
        showDialog(ConversationDialog.VideoPlayer(url, title, attachment))
    }

    internal fun showConfirmEndContactDialog() = scope("showConfirmEndContactDialog") {
        showDialog(EndContactConfirmation)
    }

    internal fun endContact() = scope("endContact") {
        dismissDialog()
        viewModelScope.launch {
            chatThreadHandler.firstOrNull()?.endContact()
        }
    }

    internal fun showEndContactDialog() = scope("showEndContactDialog") {
        showDialog(EndContact)
    }

    internal fun showSendTranscriptDialog(dialog: SendTranscriptDialog) {
        _showTranscriptDialog.update { dialog }
    }

    internal fun dismissSendTranscriptDialog() {
        _showFullScreenLoading.update { false }
        _showTranscriptDialog.update { SendTranscriptDialog.None }
    }

    internal fun sendTranscript(email: String) {
        viewModelScope.launch {
            _showFullScreenLoading.update { true }
            try {
                val response = eventHandler.first().sendTranscript(email = email)
                _showFullScreenLoading.update { false }
                val success = response is EventResponse.Success
                showSendTranscriptDialog(SendTranscriptDialog.SendTranscriptUpdateDialog(success))
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _showFullScreenLoading.update { false }
                showSendTranscriptDialog(SendTranscriptDialog.SendTranscriptUpdateDialog(false))
            }
        }
    }

    internal fun reportReplyButtonClicked(replyButton: SdkReplyButton) = scope("reportReplyButtonClicked") {
        viewModelScope.launch {
            // Captured before the suspending call below - a survey can arrive while triggerAction()
            // is in flight and must not be dismissed as if this click had caused it.
            val dialogBeforeAction = dialogShown.value
            try {
                eventHandler.first().triggerAction(replyButton)
                if (dialogBeforeAction is ConversationDialog.Popup && dialogShown.value === dialogBeforeAction) {
                    dismissDialog()
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (logged: Exception) {
                warning("Failed to trigger reply button action", logged)
            }
        }
    }

    internal fun reportTimeslotSelected(
        timeSlotLocalizedText: String,
        timeslot: TimeSlot,
    ) = scope("reportTimeslotClicked") {
        viewModelScope.launch {
            try {
                eventHandler.first().selectTimeSlot(
                    timeSlotLocalizedText = timeSlotLocalizedText,
                    timeSlot = timeslot,
                )
                // If the timeslot was clicked from a popup, dismiss the popup dialog.
                if (dialogShown.value is ConversationDialog.Popup) dismissDialog()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (logged: Exception) {
                warning("Failed to report selected timeslot", logged)
            }
        }
    }
}

private data class TemporarySentMessage(
    override val id: UUID,
    override val threadId: UUID,
    override val createdAt: Instant,
    override val direction: MessageDirection,
    override val metadata: MessageMetadata,
    override val author: MessageAuthor?,
    override val attachments: Iterable<Attachment>,
    override val fallbackText: String?,
    override val text: String,
) : SdkText() {
    constructor(id: UUID, threadId: UUID, text: String) : this(
        id = id,
        threadId = threadId,
        createdAt = Clock.System.now(),
        direction = ToAgent,
        metadata = object : MessageMetadata {
            override val seenAt: Instant? = null
            override val readAt: Instant? = null
            override val seenByCustomerAt: Instant? = null
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

    constructor(id: UUID, text: String, threadId: UUID, attachments: List<Attachment>) : this(
        id = id,
        threadId = threadId, // Will be overwritten by the backend
        createdAt = Clock.System.now(),
        direction = ToAgent,
        metadata = object : MessageMetadata {
            override val seenAt: Instant? = null
            override val readAt: Instant? = null
            override val seenByCustomerAt: Instant? = null
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

internal fun isAiAgentMessage(message: SdkMessage): Boolean =
    message.direction == MessageDirection.ToClient && message.isAiGenerated

/**
 * The session ([SelectedThreadRepository.currentSessionGeneration]) and thread an attachment pick was made for.
 * [handler] and [threadId] are null when the pick came before any thread was known, e.g. a first open while still
 * connecting.
 */
internal data class AttachmentTarget(
    val sessionGeneration: Int,
    val handler: ChatThreadHandler?,
    val threadId: UUID?,
)

internal data class PreparedAttachment(
    val contentDescriptor: ContentDescriptor,
    override val url: String,
) : Attachment {
    override val friendlyName: String = contentDescriptor.friendlyName ?: "unnamed"
    override val mimeType: String = contentDescriptor.mimeType
}
