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

package com.nice.cxonechat.ui

import android.Manifest.permission
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.SnackbarDuration.Short
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.createGraph
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.nice.cxonechat.ChatMode.LiveChat
import com.nice.cxonechat.ChatMode.MultiThread
import com.nice.cxonechat.ChatState
import com.nice.cxonechat.ChatState.Connected
import com.nice.cxonechat.ChatState.Initial
import com.nice.cxonechat.ChatState.Preparing
import com.nice.cxonechat.ChatState.Ready
import com.nice.cxonechat.message.Attachment
import com.nice.cxonechat.ui.R.anim
import com.nice.cxonechat.ui.R.string
import com.nice.cxonechat.ui.Screen.Offline
import com.nice.cxonechat.ui.Screen.ThreadList
import com.nice.cxonechat.ui.Screen.ThreadScreen
import com.nice.cxonechat.ui.composable.HandleChatErrorState
import com.nice.cxonechat.ui.composable.HandleChatState
import com.nice.cxonechat.ui.composable.HandleChatViewDialog
import com.nice.cxonechat.ui.composable.OfflineContentView
import com.nice.cxonechat.ui.composable.ThreadContentView
import com.nice.cxonechat.ui.composable.ThreadListContentView
import com.nice.cxonechat.ui.composable.conversation.ChatThreadTopBar
import com.nice.cxonechat.ui.composable.conversation.model.ConversationTopBarState
import com.nice.cxonechat.ui.composable.showCancellableSnackbar
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import com.nice.cxonechat.ui.composable.theme.Fab
import com.nice.cxonechat.ui.composable.theme.Scaffold
import com.nice.cxonechat.ui.composable.theme.TopBar
import com.nice.cxonechat.ui.domain.AttachmentSharingRepository
import com.nice.cxonechat.ui.main.AudioRecordingViewModel
import com.nice.cxonechat.ui.main.ChatStateViewModel
import com.nice.cxonechat.ui.main.ChatThreadViewModel
import com.nice.cxonechat.ui.main.ChatThreadsViewModel
import com.nice.cxonechat.ui.main.ChatViewModel
import com.nice.cxonechat.ui.storage.ValueStorage
import com.nice.cxonechat.ui.util.applyFixesForKeyboardInput
import com.nice.cxonechat.ui.util.checkNotificationPermissions
import com.nice.cxonechat.ui.util.checkPermissions
import com.nice.cxonechat.ui.util.contentDescription
import com.nice.cxonechat.ui.util.openWithAndroid
import com.nice.cxonechat.ui.util.overrideCloseAnimation
import com.nice.cxonechat.ui.util.overrideOpenAnimation
import com.nice.cxonechat.ui.util.parseThreadDeeplink
import com.nice.cxonechat.ui.util.repeatOnOwnerLifecycle
import com.nice.cxonechat.ui.util.showToast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * Chat container activity.
 */
@Suppress("TooManyFunctions")
class ChatActivity : ComponentActivity() {
    private val chatViewModel: ChatViewModel by viewModel()
    private val chatThreadsViewModel: ChatThreadsViewModel by viewModel()
    private val chatThreadViewModel: ChatThreadViewModel by viewModel()
    private val chatStateViewModel: ChatStateViewModel by viewModel()
    private val audioViewModel: AudioRecordingViewModel by viewModel()
    private val attachmentSharingRepository: AttachmentSharingRepository by inject()
    private val valueStorage: ValueStorage by inject()

    private val requestPermissionLauncher: ActivityResultLauncher<String> = getNotificationRequestResult()
    private val audioRequestPermissionLauncher = getAudioRequestResult()
    private val activityLauncher by lazy {
        SelectAttachmentActivityLauncher(::sendAttachment, activityResultRegistry).also(lifecycle::addObserver)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyFixesForKeyboardInput()
        activityLauncher // activity launcher has to self-register before onStart
        setupComposableUi()
        repeatOnOwnerLifecycle { intent.handleDeeplink() }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        repeatOnOwnerLifecycle { intent.handleDeeplink() }
    }

    override fun onPause() {
        super.onPause()
        chatViewModel.close()
    }

    @Composable
    private fun BackgroundThreadUpdates(snackbarHostState: SnackbarHostState) {
        chatThreadsViewModel.backgroundThreadsFlow.collectAsState(null).value?.let { thread ->
            LaunchedEffect(thread) {
                snackbarHostState.showSnackbar(
                    message = getString(string.background_thread_updated, thread.chatThread.threadName.orEmpty()),
                    duration = Short,
                )
            }
        }
    }

    private fun sendAttachment(it: Uri) {
        chatThreadViewModel.sendAttachment(it)
    }

    override fun finish() {
        super.finish()
        overrideCloseAnimation(anim.dismiss_host, anim.dismiss_chat)
    }

    private fun setupComposableUi() {
        setContent {
            ChatTheme {
                val snackbarHostState = remember { SnackbarHostState() }
                HandleEarlyChatState(snackbarHostState) { chatState ->
                    ChatUi(chatState, snackbarHostState)
                }
            }
        }
    }

    @Composable
    private fun HandleEarlyChatState(snackbarHostState: SnackbarHostState, onChatReady: @Composable (ChatState) -> Unit) {
        val state by chatStateViewModel.state.collectAsState()
        val context = LocalContext.current
        when (state) {
            // if the chat isn't prepared yet, prepare it.  Hopefully it's been
            // configured by the provider.
            Initial, Preparing -> LaunchedEffect(state) {
                snackbarHostState.currentSnackbarData?.dismiss()
                if (state == Initial) {
                    chatViewModel.prepare(context)
                }
                snackbarHostState.showCancellableSnackbar(
                    message = context.getString(string.preparing_sdk),
                    actionLabel = context.getString(string.cancel),
                    onAction = ::finish,
                )
            }

            else -> onChatReady(state)
        }
    }

    @Composable
    private fun ChatUi(chatState: ChatState, snackbarHostState: SnackbarHostState) {
        val isMultiThread = remember { chatViewModel.chatMode === MultiThread }
        val isLiveChat = remember { chatViewModel.chatMode === LiveChat }

        val navController = rememberNavController()
        val initialScreen = remember { getChatInitialScreen(isLiveChat, chatState, isMultiThread) }
        val screenGraph = navController.createGraph(startDestination = initialScreen) {
            composable<Offline> {
                OfflineView(snackbarHostState)
            }
            composable<ThreadList> {
                ThreadListView(snackbarHostState, navController)
            }
            composable<ThreadScreen> {
                ThreadView(snackbarHostState, isMultiThread, isLiveChat)
            }
        }
        HandleChatViewDialog(
            dialogShownFlow = chatViewModel.dialogShown,
            cancelAction = ::finish,
            submitAction = chatViewModel::respondToSurvey,
            retryAction = chatViewModel::refreshThreadState
        )
        HandleChatState(
            snackbarHostState = snackbarHostState,
            chatStateFlow = chatStateViewModel.state,
            onConnectChatAction = chatViewModel::connect,
            onReadyAction = {
                navController.navigate(if (chatViewModel.chatMode === MultiThread) ThreadList else ThreadScreen) {
                    popUpTo(navController.graph.findStartDestination().id) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            },
            onFinishAction = ::finish,
            onOfflineAction = {
                navController.navigate(Offline)
            },
        )
        HandleChatErrorState(snackbarHostState, chatStateViewModel.chatErrorState, ::finish)
        NavHost(navController, screenGraph)
    }

    @Composable
    private fun ThreadView(snackbarHostState: SnackbarHostState, isMultiThread: Boolean, isLiveChat: Boolean) {
        ChatTheme.Scaffold(
            snackbarHostState = snackbarHostState,
            topBar = { ThreadViewTopBar(isMultiThread, isLiveChat) }
        ) { padding ->
            Box(modifier = Modifier.padding(padding)) {
                ThreadContentView(snackbarHostState = snackbarHostState)
                if (isMultiThread) BackgroundThreadUpdates(snackbarHostState)
            }
        }
    }

    @Composable
    private fun ThreadListView(snackbarHostState: SnackbarHostState, navController: NavHostController) {
        ChatTheme.Scaffold(
            snackbarHostState = snackbarHostState,
            topBar = { ChatTheme.TopBar(title = stringResource(id = string.thread_list_title)) },
            floatingActionButton = { ChatFab(chatThreadsViewModel::createThread) }
        ) {
            Box(modifier = Modifier.padding(it)) {
                BackgroundThreadUpdates(snackbarHostState)
                ThreadListContentView(chatThreadsViewModel) {
                    navController.navigate(ThreadScreen)
                }
            }
        }
    }

    @Composable
    private fun OfflineView(snackbarHostState: SnackbarHostState) {
        BackHandler {
            finish()
        }
        ChatTheme.Scaffold(
            snackbarHostState = snackbarHostState,
            topBar = { ChatTheme.TopBar(title = stringResource(id = string.offline)) }
        ) {
            Box(modifier = Modifier.padding(it)) {
                OfflineContentView()
            }
        }
    }

    private fun getChatInitialScreen(isLiveChat: Boolean, chatState: ChatState, isMultiThread: Boolean) = when {
        isLiveChat && chatState === ChatState.Offline -> Offline
        isMultiThread -> ThreadList
        else -> ThreadScreen
    }

    @Composable
    private fun ThreadViewTopBar(isMultiThread: Boolean, isLiveChat: Boolean) {
        val threadNameFlow = remember { chatThreadViewModel.chatMetadata.map { it.threadName } }
        val hasQuestions = remember { chatThreadViewModel.hasQuestions }
        ChatThreadTopBar(
            conversationState = ConversationTopBarState(
                threadName = threadNameFlow,
                isMultiThreaded = isMultiThread,
                isLiveChat = isLiveChat,
                hasQuestions = hasQuestions,
                isArchived = chatThreadViewModel.isArchived,
                threadState = chatThreadViewModel.threadStateFlow,
            ),
            onEditThreadName = { chatThreadViewModel.editThreadName() },
            onEditThreadValues = chatThreadViewModel::startEditingCustomValues,
            onEndContact = chatThreadViewModel::endContact,
            displayEndConversation = chatThreadViewModel::showEndContactDialog,
        )
    }

    @SuppressLint(
        "MissingPermission" // permission state is checked by `checkPermissions()` method
    )
    private suspend fun onTriggerRecording(): Boolean {
        if (!checkPermissions(
                valueStorage = valueStorage,
                permissions = requiredRecordAudioPermissions,
                rationale = string.recording_audio_permission_rationale,
                onAcceptPermissionRequest = audioRequestPermissionLauncher::launch
            )
        ) {
            return false
            // Permissions will need to be sorted out first, user will have to click the button again after that
        }
        return if (audioViewModel.recordingFlow.value) {
            audioViewModel.stopRecording(this@ChatActivity)
        } else {
            audioViewModel.startRecording(this@ChatActivity).isSuccess
        }
    }

    @SuppressLint(
        "MissingPermission" // permission state is checked by `checkPermissions()` method
    )
    private fun onDismissRecording() {
        lifecycleScope.launch {
            if (!checkPermissions(
                    valueStorage = valueStorage,
                    permissions = requiredRecordAudioPermissions,
                    rationale = string.recording_audio_permission_rationale,
                    onAcceptPermissionRequest = audioRequestPermissionLauncher::launch
                )
            ) {
                return@launch
            }
            audioViewModel.deleteLastRecording(this@ChatActivity) {
                showToast(string.record_audio_failed_cleanup, Toast.LENGTH_LONG)
            }
        }
    }

    private fun onShare(attachments: Collection<Attachment>) {
        chatThreadViewModel.beginPrepareAttachments()
        lifecycleScope.launch(Dispatchers.IO) {
            val intent = attachmentSharingRepository.createSharingIntent(attachments, this@ChatActivity)
            chatThreadViewModel.finishPrepareAttachments()
            lifecycleScope.launch(Dispatchers.Main) {
                if (intent == null) {
                    showToast(string.prepare_attachments_failure)
                } else {
                    startActivity(Intent.createChooser(intent, null))
                }
            }
        }
    }

    private fun onAttachmentClicked(attachment: Attachment) {
        val url = attachment.url
        val mimeType = attachment.mimeType.orEmpty()
        val title by lazy { attachment.contentDescription }
        when {
            mimeType.startsWith("image/") -> chatThreadViewModel.showImage(url, title ?: getString(string.image_preview_title))
            mimeType.startsWith("video/") -> chatThreadViewModel.showVideo(url, title ?: getString(string.video_preview_title))
            mimeType.startsWith("audio/") -> chatThreadViewModel.playAudio(url, title)
            else -> openWithAndroid(attachment)
        }
    }

    private fun openWithAndroid(attachment: Attachment) {
        if (!openWithAndroid(attachment.url, attachment.mimeType)) showInvalidAttachmentDialog(attachment)
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            chatStateViewModel.state.filter { it == Connected }.firstOrNull()?.also {
                chatViewModel.reportOnResume()
            }
        }
        lifecycleScope.launch {
            chatStateViewModel.state.filter { it === Ready }.first()
            chatViewModel.refreshThreadState()
        }
        if (VERSION.SDK_INT >= VERSION_CODES.TIRAMISU) {
            checkNotificationPermissions(
                permission.POST_NOTIFICATIONS,
                string.notifications_rationale,
                requestPermissionLauncher::launch
            )
        }
    }

    private suspend fun Intent.handleDeeplink() {
        val data = data ?: return
        withContext(Dispatchers.Default) {
            data
                .parseThreadDeeplink()
                .mapCatching { chatThreadsViewModel.selectThreadById(it) }
        }
    }

    companion object {
        private val requiredRecordAudioPermissions = if (VERSION.SDK_INT > VERSION_CODES.Q) {
            setOf(permission.RECORD_AUDIO)
        } else {
            setOf(permission.RECORD_AUDIO, permission.WRITE_EXTERNAL_STORAGE)
        }

        @Composable
        private fun ChatActivity.ThreadContentView(snackbarHostState: SnackbarHostState) {
            ThreadContentView(
                onAttachmentClicked = ::onAttachmentClicked,
                onShare = ::onShare,
                closeChat = ::finish,
                onDismissRecording = ::onDismissRecording,
                onTriggerRecording = ::onTriggerRecording,
                chatThreadViewModel = chatThreadViewModel,
                chatViewModel = chatViewModel,
                audioViewModel = audioViewModel,
                snackbarHostState = snackbarHostState,
                activityLauncher = activityLauncher,
            )
            val chatThreadsState by chatThreadsViewModel.state.collectAsState()
            if (chatThreadsState === ChatThreadsViewModel.State.ThreadSelected) {
                chatThreadsViewModel.resetState()
            }
        }

        /**
         * Start the [ChatActivity] from a given source activity.
         *
         * @param from Activity to use as a base for the new [ChatActivity].
         */
        @JvmStatic
        fun startChat(from: Activity) {
            from.startActivity(Intent(from, ChatActivity::class.java))
            from.overrideOpenAnimation(anim.present_chat, anim.present_host)
        }
    }
}

private fun ComponentActivity.getNotificationRequestResult() =
    registerForActivityResult(RequestPermission()) { isGranted ->
        if (!isGranted) {
            MaterialAlertDialogBuilder(this)
                .setTitle(string.no_notifications_title)
                .setMessage(string.no_notifications_message)
                .setNeutralButton(string.ok, null)
                .show()
        }
    }

private fun ComponentActivity.getAudioRequestResult() =
    registerForActivityResult(RequestMultiplePermissions()) { requestResults: Map<String, Boolean>? ->
        if (requestResults.orEmpty().any { !it.value }) {
            MaterialAlertDialogBuilder(this)
                .setTitle(string.recording_audio_permission_denied_title)
                .setMessage(string.recording_audio_permission_denied_body)
                .setNeutralButton(string.ok) { dialog, _ ->
                    dialog.dismiss()
                }
        }
    }

private fun Context.showInvalidAttachmentDialog(attachment: Attachment) {
    MaterialAlertDialogBuilder(this)
        .setTitle(string.unsupported_type_title)
        .setMessage(getString(string.unsupported_type_message, attachment.mimeType))
        .setNegativeButton(string.cancel, null)
        .show()
}

@Composable
private fun ChatFab(onClick: () -> Unit = {}) {
    ChatTheme.Fab(
        onClick = onClick,
        icon = rememberVectorPainter(image = Icons.Default.Add),
        contentDescription = null,
    )
}

@Serializable
private sealed class Screen {

    @Serializable
    data object Offline : Screen()

    @Serializable
    data object ThreadList : Screen()

    @Serializable
    data object ThreadScreen : Screen()
}
