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

package com.nice.cxonechat.ui.composable

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBoxValue.EndToStart
import androidx.compose.material3.SwipeToDismissBoxValue.StartToEnd
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment.Companion.Center
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.nice.cxonechat.message.Message
import com.nice.cxonechat.prechat.PreChatSurvey
import com.nice.cxonechat.state.FieldDefinition
import com.nice.cxonechat.state.FieldDefinitionList
import com.nice.cxonechat.thread.Agent
import com.nice.cxonechat.thread.ChatThread
import com.nice.cxonechat.thread.ChatThreadState
import com.nice.cxonechat.thread.ChatThreadState.Ready
import com.nice.cxonechat.thread.CustomField
import com.nice.cxonechat.ui.R.array
import com.nice.cxonechat.ui.R.string
import com.nice.cxonechat.ui.composable.conversation.model.PreviewMessageProvider
import com.nice.cxonechat.ui.composable.generic.describe
import com.nice.cxonechat.ui.composable.theme.Alert
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import com.nice.cxonechat.ui.composable.theme.ChatTheme.chatColors
import com.nice.cxonechat.ui.composable.theme.ChatTheme.chatTypography
import com.nice.cxonechat.ui.composable.theme.ChatTheme.space
import com.nice.cxonechat.ui.composable.theme.SingleChoiceSegmentedButton
import com.nice.cxonechat.ui.composable.theme.SwipeBackground
import com.nice.cxonechat.ui.composable.theme.SwipeToDismiss
import com.nice.cxonechat.ui.domain.model.ChatThreadCopy.Companion.copy
import com.nice.cxonechat.ui.domain.model.CreateThreadResult.Failure
import com.nice.cxonechat.ui.domain.model.Thread
import com.nice.cxonechat.ui.domain.model.prechat.PreChatResponse
import com.nice.cxonechat.ui.screen.PreChatSurveyScreen
import com.nice.cxonechat.ui.util.Ignored
import com.nice.cxonechat.ui.viewmodel.ChatThreadsViewModel.State
import com.nice.cxonechat.ui.viewmodel.ChatThreadsViewModel.State.Initial
import com.nice.cxonechat.ui.viewmodel.ChatThreadsViewModel.State.ThreadPreChatSurveyRequired
import com.nice.cxonechat.ui.viewmodel.ChatThreadsViewModel.State.ThreadSelected
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.random.Random

@Composable
internal fun MultiThreadContent(
    threads: List<Thread>,
    onThreadSelected: (Thread) -> Unit,
    onArchiveThread: (Thread) -> Unit,
    state: State,
    threadFailure: Failure?,
    resetState: () -> Unit,
    respondToSurvey: (Sequence<PreChatResponse>) -> Unit,
    resetCreateThreadState: () -> Unit,
    editThreadName: (Thread) -> Unit,
) {
    Column(modifier = Modifier.testTag("multi_thread_content")) {
        var showArchivedThreads by rememberSaveable { mutableStateOf(false) }

        ActiveThreadToggle(showArchivedThreads = showArchivedThreads) {
            showArchivedThreads = !showArchivedThreads
        }
        AnimatedContent(showArchivedThreads) { showArchived ->
            if (showArchived) {
                val archivedThreads = remember(threads) { threads.filter { !it.chatThread.canAddMoreMessages } }
                ArchivedThreadListView(
                    threads = archivedThreads,
                    onThreadSelected = onThreadSelected
                )
            } else {
                val activeThreads = remember(threads) { threads.filter { it.chatThread.canAddMoreMessages } }
                ChatThreadListView(
                    threads = activeThreads,
                    onThreadSelected = onThreadSelected,
                    onArchiveThread = onArchiveThread,
                    editThreadName = editThreadName,
                )
            }
        }
    }

    ChatThreadsStateAlert(
        state = state,
        threadFailure = threadFailure,
        resetState = resetState,
        respondToSurvey = respondToSurvey,
        resetCreateThreadState = resetCreateThreadState
    )
}

@Composable
@Preview
private fun ActiveThreadToggle(
    modifier: Modifier = Modifier,
    showArchivedThreads: Boolean = false,
    onValueChanged: (Boolean) -> Unit = {},
) {
    val context = LocalContext.current
    val states = remember { context.resources.getStringArray(array.thread_state_names).toList() }

    Row(
        modifier = Modifier
            .testTag("active_thread_toggle_view")
            .then(modifier)
            .fillMaxWidth()
            .padding(space.defaultPadding),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = CenterVertically,
    ) {
        ChatTheme.SingleChoiceSegmentedButton(
            currentSelection = states[if (showArchivedThreads) 1 else 0],
            toggleStates = states,
            modifier = Modifier
                .testTag("active_thread_toggle_button_row"),
        ) {
            onValueChanged(it != states[0])
        }
    }
}

@Composable
private fun ChatThreadListView(
    threads: List<Thread>,
    modifier: Modifier = Modifier,
    onThreadSelected: (Thread) -> Unit,
    onArchiveThread: (Thread) -> Unit,
    editThreadName: (Thread) -> Unit,
) {
    AnimatedContent(targetState = threads.isEmpty(), modifier = Modifier.testTag("chat_thread_list")) { noThreads ->
        if (noThreads) {
            Box(
                Modifier
                    .fillMaxSize()
                    .testTag("no_threads_view")
            ) {
                Text(
                    stringResource(string.label_no_threads),
                    modifier = Modifier
                        .align(Center)
                        .alpha(0.5f),
                    style = chatTypography.noThreads
                )
            }
        } else {
            val lazyListState = rememberLazyListState()

            LazyColumn(
                state = lazyListState,
                modifier = modifier,
            ) {
                items(
                    threads,
                    key = { item: Thread -> item.id }
                ) { thread ->
                    Column(Modifier.animateItem()) {
                        ChatThreadWrapper(
                            thread = thread,
                            onArchiveThread = onArchiveThread,
                            editThreadName = editThreadName,
                        ) {
                            ChatThreadView(thread = thread, onThreadSelected = onThreadSelected)
                        }
                        HorizontalDivider(
                            color = chatColors.token.border.default,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ArchivedThreadListView(
    threads: List<Thread>,
    modifier: Modifier = Modifier,
    onThreadSelected: (Thread) -> Unit,
) {
    val lazyListState = rememberLazyListState()

    LazyColumn(
        state = lazyListState,
        modifier = Modifier
            .testTag("archived_thread_list")
            .then(modifier),
    ) {
        items(
            threads,
            key = { item: Thread -> item.id }
        ) { thread ->
            ChatThreadView(thread = thread, onThreadSelected = onThreadSelected)
            HorizontalDivider(
                color = chatColors.token.border.default,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatThreadsStateAlert(
    state: State,
    threadFailure: Failure?,
    resetState: () -> Unit,
    respondToSurvey: (Sequence<PreChatResponse>) -> Unit,
    resetCreateThreadState: () -> Unit,
) {
    when (state) {
        /* nothing to do */
        Initial -> Ignored

        is ThreadPreChatSurveyRequired -> {
            val coroutineScope = rememberCoroutineScope()
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            PreChatSurveyScreen(
                survey = state.survey,
                onCancel = resetState,
                onValidSurveySubmission = respondToSurvey,
                sheetState = sheetState,
            )

            threadFailure?.let { failure ->
                ChatTheme.Alert(
                    message = describe(failure),
                    onDismiss = {
                        coroutineScope.launch {
                            resetCreateThreadState()
                            sheetState.show()
                        }
                    },
                    dismissLabel = stringResource(string.cancel),
                    modifier = Modifier.testTag("thread_creation_failed_dialog"),
                )
            }
        }

        /* handled by state monitor */
        ThreadSelected -> Ignored
    }
}

@Composable
private fun ChatThreadWrapper(
    thread: Thread,
    onArchiveThread: (Thread) -> Unit,
    editThreadName: (Thread) -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val currentThread by rememberUpdatedState(newValue = thread)

    val dismissBoxState = rememberSwipeToDismissBoxState()
    var visible by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()
    AnimatedVisibility(visible) {
        ChatTheme.SwipeToDismiss(
            dismissState = dismissBoxState,
            directions = mapOf(
                EndToStart to SwipeBackground(
                    color = chatColors.token.status.warning,
                    icon = Icons.Default.Archive,
                    iconTint = chatColors.token.status.onWarning,
                    contentDescription = stringResource(string.archive_thread_content_description)
                ),
                StartToEnd to SwipeBackground(
                    color = chatColors.token.status.success,
                    icon = Icons.Default.Settings,
                    iconTint = chatColors.token.status.onSuccess,
                    contentDescription = stringResource(string.change_thread_name)
                )
            ),
            modifier = Modifier.fillMaxWidth(),
            onDismiss = { value ->
                if (value == EndToStart) {
                    onArchiveThread(currentThread)
                    visible = false // Hide the thread after archiving
                }
                if (value == StartToEnd) {
                    scope.launch {
                        editThreadName(currentThread)
                        dismissBoxState.reset() // Reset the box state after editing
                    }
                }
            },
            content = content
        )
    }
}

// Preview
@Immutable
internal data class PreviewAgent(
    override val id: Int,
    override val firstName: String,
    override val lastName: String,
    override val nickname: String? = null,
    override val isBotUser: Boolean = false,
    override val isSurveyUser: Boolean = false,
    override val imageUrl: String,
    override val isTyping: Boolean = false,
) : Agent() {
    companion object {
        private var nextId = 1
            get() = field.also { field += 1 }

        fun nextAgent(): Agent {
            val agentId = nextId
            val firstName = "Number$agentId"
            val lastName = "Agent"

            return PreviewAgent(
                id = agentId,
                firstName = firstName,
                lastName = lastName,
                imageUrl = "https://brand-embassy-avatars-qa.s3.eu-west-1.amazonaws.com/324bcba7-f317-4a99-b833-06a1952c41c6.jpg"
            )
        }
    }
}

@Immutable
internal data class PreviewThread(
    override val id: UUID = UUID.randomUUID(),
    override val threadName: String,
    override val messages: List<Message> = PreviewMessageProvider().messages.toList(),
    override val threadAgent: Agent? = PreviewAgent.nextAgent(),
    override val canAddMoreMessages: Boolean = true,
    override val scrollToken: String = "",
    override val fields: List<CustomField> = listOf(),
    override val threadState: ChatThreadState = Ready,
    override val positionInQueue: Int? = null,
    override val hasOnlineAgent: Boolean = true,
) : ChatThread() {
    companion object {
        var nextThreadIndex: Int = 1
            get() = field.also { field += 1 }

        fun nextThread(): Thread {
            val threadIndex = nextThreadIndex
            val archived = Random.nextBoolean()
            val thread = PreviewThread(threadName = "Thread $threadIndex", canAddMoreMessages = archived)

            return Thread(thread, thread.threadName)
        }
    }
}

@Suppress("UNUSED_PARAMETER")
private class PreviewModel(
    private val threadsFlow: MutableStateFlow<List<Thread>>,
    private val stateFlow: MutableStateFlow<State>,
    private val createThreadFailureFlow: MutableStateFlow<Failure?>,
) {
    val threads: StateFlow<List<Thread>>
        get() = threadsFlow.asStateFlow()

    val state: StateFlow<State>
        get() = stateFlow.asStateFlow()

    val createThreadFailure: StateFlow<Failure?>
        get() = createThreadFailureFlow.asStateFlow()

    constructor(
        threads: List<Thread>,
        state: State = Initial,
        failure: Failure? = null,
    ) : this(
        MutableStateFlow(threads),
        MutableStateFlow(state),
        MutableStateFlow(failure)
    )

    @Suppress("UnusedPrivateMember")
    fun onThreadSelected(thread: Thread) = Unit

    fun onArchiveThread(thread: Thread) {
        threadsFlow.value = threadsFlow.value.map {
            if (thread.id == it.id) {
                it.copy(chatThread = it.chatThread.copy(canAddMoreMessages = false))
            } else {
                it
            }
        }
    }

    fun resetState() {
        stateFlow.value = Initial
    }

    fun respondToSurvey(responses: Sequence<PreChatResponse>) {
        if (responses.count() != 2) {
            createThreadFailureFlow.value = Failure.REASON_PRECHAT_SURVEY_REQUIRED
        } else {
            createThread()
        }
    }

    fun resetCreateThreadState() {
        createThreadFailureFlow.value = null
    }

    private fun createThread() {
        threadsFlow.value += PreviewThread.nextThread()
        resetState()
        resetCreateThreadState()
    }

    fun editThreadName(ignored: Thread) {
        // No-op
    }

    companion object {
        fun nextModel(
            threadCount: Int,
            state: State = Initial,
            failure: Failure? = null,
        ) = PreviewModel(
            threads = (0 until threadCount).map { PreviewThread.nextThread() },
            state = state,
            failure = failure
        )
    }
}

@PreviewLightDark
@Composable
private fun PreviewThreadList(
    @PreviewParameter(PreviewModelProvider::class) viewModel: PreviewModel = PreviewModelProvider().values.first(),
) {
    ChatTheme {
        Surface {
            MultiThreadContent(
                state = viewModel.state.collectAsState().value,
                threads = viewModel.threads.collectAsState().value,
                threadFailure = viewModel.createThreadFailure.collectAsState().value,
                onThreadSelected = viewModel::onThreadSelected,
                onArchiveThread = viewModel::onArchiveThread,
                resetState = viewModel::resetState,
                respondToSurvey = viewModel::respondToSurvey,
                resetCreateThreadState = viewModel::resetCreateThreadState,
                editThreadName = viewModel::editThreadName
            )
        }
    }
}

@Suppress("unused")
private class PreviewModelProvider : PreviewParameterProvider<PreviewModel> {
    private object Survey : PreChatSurvey {
        override val name = "PreChat Survey"
        override val fields: FieldDefinitionList = sequenceOf(
            object : FieldDefinition.Text {
                override val fieldId = UUID.randomUUID().toString()
                override val label = "Name"
                override val isRequired = false
                override val isEMail = false

                override fun validate(value: String) = Unit
            },
            object : FieldDefinition.Text {
                override val fieldId = UUID.randomUUID().toString()
                override val label = "EMail"
                override val isRequired = true
                override val isEMail = true

                override fun validate(value: String) = Unit
            }
        )
    }

    override val values = sequenceOf(
        PreviewModel.nextModel(4),
    )
}
