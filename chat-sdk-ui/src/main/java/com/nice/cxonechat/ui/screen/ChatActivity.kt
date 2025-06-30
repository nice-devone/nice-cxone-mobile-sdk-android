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

package com.nice.cxonechat.ui.screen

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.createGraph
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.nice.cxonechat.ChatMode.LiveChat
import com.nice.cxonechat.ChatMode.MultiThread
import com.nice.cxonechat.ChatState
import com.nice.cxonechat.ChatState.Connected
import com.nice.cxonechat.ChatState.Connecting
import com.nice.cxonechat.ChatState.Initial
import com.nice.cxonechat.ChatState.Prepared
import com.nice.cxonechat.ChatState.Preparing
import com.nice.cxonechat.ChatState.Ready
import com.nice.cxonechat.log.Logger
import com.nice.cxonechat.log.LoggerScope
import com.nice.cxonechat.log.warning
import com.nice.cxonechat.message.Attachment
import com.nice.cxonechat.ui.R.anim
import com.nice.cxonechat.ui.R.string
import com.nice.cxonechat.ui.SelectAttachmentActivityLauncher
import com.nice.cxonechat.ui.UiModule
import com.nice.cxonechat.ui.composable.ChatStateEffect
import com.nice.cxonechat.ui.composable.ThreadContentView
import com.nice.cxonechat.ui.composable.ThreadViewTopBar
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import com.nice.cxonechat.ui.composable.theme.Scaffold
import com.nice.cxonechat.ui.data.repository.AttachmentSharingRepository
import com.nice.cxonechat.ui.data.source.AttachmentDataSource
import com.nice.cxonechat.ui.domain.usecase.NotifyUpdateUseCase
import com.nice.cxonechat.ui.screen.Screen.Offline
import com.nice.cxonechat.ui.screen.Screen.ThreadList
import com.nice.cxonechat.ui.screen.Screen.ThreadScreen
import com.nice.cxonechat.ui.storage.ValueStorage
import com.nice.cxonechat.ui.storage.ValueStorage.Companion.removeFromStringSet
import com.nice.cxonechat.ui.util.applyFixesForKeyboardInput
import com.nice.cxonechat.ui.util.checkNotificationPermissions
import com.nice.cxonechat.ui.util.checkPermissions
import com.nice.cxonechat.ui.util.contentDescription
import com.nice.cxonechat.ui.util.koinActivityViewModel
import com.nice.cxonechat.ui.util.openWithAndroid
import com.nice.cxonechat.ui.util.overrideCloseAnimation
import com.nice.cxonechat.ui.util.overrideOpenAnimation
import com.nice.cxonechat.ui.util.parseThreadDeeplink
import com.nice.cxonechat.ui.util.repeatOnOwnerLifecycle
import com.nice.cxonechat.ui.util.showCancellableSnackbar
import com.nice.cxonechat.ui.util.showToast
import com.nice.cxonechat.ui.viewmodel.AudioRecordingViewModel
import com.nice.cxonechat.ui.viewmodel.ChatStateViewModel
import com.nice.cxonechat.ui.viewmodel.ChatThreadViewModel
import com.nice.cxonechat.ui.viewmodel.ChatThreadsViewModel
import com.nice.cxonechat.ui.viewmodel.ChatViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.get
import org.koin.android.ext.android.inject
import org.koin.android.scope.AndroidScopeComponent
import org.koin.androidx.scope.activityScope
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.compose.LocalKoinScope
import org.koin.compose.koinInject
import org.koin.core.qualifier.named

/**
 * Chat container activity.
 */
@Suppress(
    "TooManyFunctions",
    "LargeClass", // Will be fixed in DE-131056
)
class ChatActivity : ComponentActivity(), AndroidScopeComponent {
    internal val audioViewModel: AudioRecordingViewModel by viewModel()
    internal val valueStorage: ValueStorage by inject()
    private val chatViewModel: ChatViewModel by viewModel()
    private val chatThreadsViewModel: ChatThreadsViewModel by viewModel()
    private val chatThreadViewModel: ChatThreadViewModel by viewModel()
    private val chatStateViewModel: ChatStateViewModel by viewModel()
    private val attachmentSharingRepository: AttachmentSharingRepository by inject()

    private val requestPermissionLauncher: ActivityResultLauncher<String> = getNotificationRequestResult()
    private val audioRequestPermissionLauncher = getAudioRequestResult()

    override val scope by activityScope()

    private val loggerScope: LoggerScope by lazy {
        LoggerScope("ChatActivity", get<Logger>(named(UiModule.loggerName)))
    }

    private val activityLauncher by lazy {
        SelectAttachmentActivityLauncher(
            context = application,
            temporaryFileStorage = get(),
            sendAttachments = ::addAttachment,
            registry = activityResultRegistry
        ).also(lifecycle::addObserver)
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

    override fun onDestroy() {
        super.onDestroy()
        val atd = inject<AttachmentDataSource>()
        repeatOnOwnerLifecycle(state = Lifecycle.State.DESTROYED) {
            if (atd.isInitialized()) {
                atd.value.clearCache()
            }
        }
    }

    @Composable
    private fun BackgroundThreadUpdates() {
        val notifyUpdateUseCase: NotifyUpdateUseCase = koinInject()
        chatThreadsViewModel.backgroundThreadsFlow.collectAsState(null).value?.let { thread ->
            LaunchedEffect(thread.id, thread.messages.toList().size) {
                notifyUpdateUseCase.notifyUpdate(thread)
            }
        }
    }

    private fun addAttachment(attachments: List<Uri>) {
        chatThreadViewModel.addPendingAttachments(attachments)
    }

    override fun finish() {
        super.finish()
        overrideCloseAnimation(anim.dismiss_host, anim.dismiss_chat)
    }

    private fun setupComposableUi() {
        enableEdgeToEdge()
        setContent {
            CompositionLocalProvider(
                LocalKoinScope provides scope // Scope all injections to this activity by default
            ) {
                ChatTheme {
                    val snackbarHostState = remember { SnackbarHostState() }
                    HandleEarlyChatState(snackbarHostState) { chatState ->
                        ChatUi(chatState, snackbarHostState)
                    }
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
        val testMod = Modifier.semantics {
            testTagsAsResourceId = true // Enabled for UI test automation
        }
        val screenGraph = navController.createGraph(startDestination = initialScreen) {
            composable<Offline> {
                OfflineScreen(::finish, snackbarHostState, testMod)
            }
            composable<ThreadList> {
                ThreadListScreen(snackbarHostState, navController, testMod)
            }
            composable<ThreadScreen> {
                ThreadScreen(snackbarHostState, isMultiThread, isLiveChat, testMod)
            }
        }
        ChatDialogScreen(
            dialogShownFlow = chatViewModel.dialogShown,
            modifier = testMod,
            cancelAction = remember { { finish() } },
            submitAction = remember { { chatViewModel.respondToSurvey(it) } },
            retryAction = remember { { chatViewModel.refreshThreadState() } }
        )
        ChatStateEffect(
            chatStateFlow = chatStateViewModel.state,
            onConnectChatAction = remember { { chatViewModel.connect() } },
            onReadyAction = {
                navController.navigate(if (chatViewModel.chatMode === MultiThread) ThreadList else ThreadScreen) {
                    popUpTo(navController.graph.findStartDestination().id) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            },
            onOfflineAction = { navController.navigate(Offline) }
        )
        ChatErrorScreen(snackbarHostState, chatStateViewModel.chatErrorState, onTerminalError = ::finish)
        NavHost(navController, screenGraph, modifier = testMod)
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun ThreadScreen(snackbarHostState: SnackbarHostState, isMultiThread: Boolean, isLiveChat: Boolean, modifier: Modifier) {
        val chatThreadViewModel: ChatThreadViewModel = koinActivityViewModel<ChatThreadViewModel>()
        val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
        ChatTheme.Scaffold(
            modifier = Modifier
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .imePadding()
                .testTag("thread_view")
                .then(modifier),
            snackbarHostState = snackbarHostState,
            topBar = {
                val dialogState by chatThreadViewModel.dialogShown.collectAsState()
                if (dialogState !is ChatThreadViewModel.Dialogs.FullScreenDialog) {
                    ThreadViewTopBar(isMultiThread, isLiveChat, scrollBehavior)
                }
            },
        ) { padding ->
            Box(modifier = Modifier.padding(padding)) {
                ThreadView()
                if (isMultiThread) BackgroundThreadUpdates()
            }
        }
    }

    private fun getChatInitialScreen(isLiveChat: Boolean, chatState: ChatState, isMultiThread: Boolean) = when {
        isLiveChat && chatState === ChatState.Offline -> Offline
        isMultiThread -> ThreadList
        else -> ThreadScreen
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
            ).also(audioViewModel::setRecordingPermissionGranted)
        ) {
            return false
            // Permissions will need to be sorted out first, user will have to click the button again after that
        }
        return if (audioViewModel.recordingFlow.value) {
            audioViewModel.stopRecording()
        } else {
            audioViewModel.startRecording().isSuccess
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
                ).also(audioViewModel::setRecordingPermissionGranted)
            ) {
                return@launch
            }
            audioViewModel.deleteLastRecording {
                showToast(string.record_audio_failed_cleanup, Toast.LENGTH_LONG)
            }
        }
    }

    private fun onShare(attachments: Collection<Attachment>) {
        chatThreadViewModel.beginPrepareAttachments()
        lifecycleScope.launch(Dispatchers.IO) {
            val sharingIntent = attachmentSharingRepository.createSharingIntent(attachments, this@ChatActivity)
            chatThreadViewModel.finishPrepareAttachments()
            lifecycleScope.launch(Dispatchers.Main) {
                if (sharingIntent == null) {
                    showToast(string.prepare_attachments_failure)
                } else {
                    startActivity(Intent.createChooser(sharingIntent, null))
                }
            }
        }
    }

    private fun onAttachmentClicked(attachment: Attachment) {
        val url = attachment.url
        val mimeType = attachment.mimeType.orEmpty()
        val title by lazy { attachment.contentDescription }
        when {
            mimeType.startsWith("image/") -> chatThreadViewModel.showImage(
                image = url,
                title = title.takeUnless { it.isNullOrEmpty() } ?: getString(string.image_preview_title),
                attachment = attachment
            )

            mimeType.startsWith("video/") -> chatThreadViewModel.showVideo(
                url = url,
                title = title ?: getString(string.video_preview_title),
                attachment = attachment
            )

            else -> openWithAndroid(attachment)
        }
    }

    private fun openWithAndroid(attachment: Attachment) {
        if (!openWithAndroid(attachment.url, attachment.mimeType)) showInvalidAttachmentDialog(attachment)
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            chatStateViewModel.state.filter { it.isAtLeastPrepared() }.firstOrNull()?.also {
                chatViewModel.reportOnResume()
            }
        }
        lifecycleScope.launch {
            chatStateViewModel.state.filter { it === Ready }.first()
            chatViewModel.refreshThreadState()
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            checkNotificationPermissions(
                Manifest.permission.POST_NOTIFICATIONS,
                string.notifications_rationale,
                requestPermissionLauncher::launch
            )
        }
    }

    private fun ChatState.isAtLeastPrepared() = this === Connected || this === Ready || this === Prepared || this === Connecting

    private suspend fun Intent.handleDeeplink() {
        val data = data ?: return
        withContext(Dispatchers.Default) {
            data
                .parseThreadDeeplink()
                .mapCatching { chatThreadsViewModel.selectThreadById(it) }
                .onFailure {
                    loggerScope.warning("Failed to parse deeplink: $data", it)
                }
        }
    }

    @Suppress(
        "UndocumentedPublicClass", // Companion objects don't require documentation.
    )
    companion object {
        private val requiredRecordAudioPermissions = if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) {
            setOf(Manifest.permission.RECORD_AUDIO)
        } else {
            setOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }

        @Composable
        private fun ChatActivity.ThreadView() {
            val chatThreadViewModel: ChatThreadViewModel = koinActivityViewModel()
            ThreadContentView(
                onAttachmentClicked = remember { { onAttachmentClicked(it) } },
                onShare = remember { { onShare(it) } },
                closeChat = remember { { finish() } },
                onDismissRecording = remember { { onDismissRecording() } },
                onTriggerRecording = remember { { onTriggerRecording() } },
                chatThreadViewModel = chatThreadViewModel,
                chatViewModel = chatViewModel,
                audioViewModel = audioViewModel,
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

        /**
         * Prepare an intent which will open Chat with specified thread, if possible.
         *
         * @param context see [Intent]
         * @param uri Uri to use as a base for the new [ChatActivity].
         */
        internal fun buildOpenThreadIntent(
            context: Context,
            uri: Uri,
        ) = Intent(Intent.ACTION_VIEW, uri, context, ChatActivity::class.java)
    }
}

/**
 * Register for notification request result.
 *
 * This function sets up an activity result launcher that will handle the result of the notification permission request.
 * If the permission is not granted, it shows a dialog informing the user about the denied permission.
 */
private fun ComponentActivity.getNotificationRequestResult() =
    registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (!isGranted) {
            MaterialAlertDialogBuilder(this)
                .setTitle(string.no_notifications_title)
                .setMessage(string.no_notifications_message)
                .setNeutralButton(string.ok, null)
                .show()
        }
    }

/**
 * Register for audio recording permission request result.
 *
 * This function sets up an activity result launcher that will handle the result of the audio recording permission request.
 * If the permissions are granted, it updates the `audioViewModel` and removes the permissions from the stored set.
 * If not granted, it shows a dialog informing the user about the denied permission.
 */
private fun ChatActivity.getAudioRequestResult() =
    registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { requestResults: Map<String, Boolean>? ->
        val results = requestResults.orEmpty()
        if (results.values.all { it }) {
            lifecycleScope.launch(Dispatchers.Default) {
                audioViewModel.setRecordingPermissionGranted(true)
                // Remove permissions from the stored set since they are granted now, but it may be temporary.
                valueStorage.removeFromStringSet(ValueStorage.StringKey.RequestedPermissionsKey, results.keys)
            }
        } else {
            audioViewModel.setRecordingPermissionGranted(false)
            MaterialAlertDialogBuilder(this)
                .setTitle(string.recording_audio_permission_denied_title)
                .setMessage(string.recording_audio_permission_denied_body)
                .setNeutralButton(string.ok) { dialog, _ ->
                    dialog.dismiss()
                }
        }
    }

/**
 * Show a dialog indicating that the attachment type is not supported.
 *
 * @param attachment The attachment that is not supported.
 */
private fun Context.showInvalidAttachmentDialog(attachment: Attachment) {
    MaterialAlertDialogBuilder(this)
        .setTitle(string.unsupported_type_title)
        .setMessage(getString(string.unsupported_type_message, attachment.mimeType))
        .setNegativeButton(string.cancel, null)
        .show()
}
