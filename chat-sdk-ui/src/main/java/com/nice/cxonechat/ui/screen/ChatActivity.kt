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

import android.Manifest.permission
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
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
import androidx.lifecycle.Lifecycle.State.DESTROYED
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.createGraph
import com.nice.cxonechat.ChatMode.LiveChat
import com.nice.cxonechat.ChatMode.MultiThread
import com.nice.cxonechat.ChatState
import com.nice.cxonechat.ChatState.Connected
import com.nice.cxonechat.ChatState.Connecting
import com.nice.cxonechat.ChatState.Initial
import com.nice.cxonechat.ChatState.Prepared
import com.nice.cxonechat.ChatState.Preparing
import com.nice.cxonechat.ChatState.Ready
import com.nice.cxonechat.Public
import com.nice.cxonechat.log.Logger
import com.nice.cxonechat.log.LoggerScope
import com.nice.cxonechat.log.duration
import com.nice.cxonechat.log.error
import com.nice.cxonechat.log.scope
import com.nice.cxonechat.ui.AttachmentType
import com.nice.cxonechat.ui.R.anim
import com.nice.cxonechat.ui.R.string
import com.nice.cxonechat.ui.SelectAttachmentActivityLauncher
import com.nice.cxonechat.ui.UiModule
import com.nice.cxonechat.ui.composable.ChatStateEffect
import com.nice.cxonechat.ui.composable.ThreadContentView
import com.nice.cxonechat.ui.composable.ThreadViewTopBar
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import com.nice.cxonechat.ui.composable.theme.Scaffold
import com.nice.cxonechat.ui.data.AudioRecordingManager
import com.nice.cxonechat.ui.data.ChatAttachmentHandler
import com.nice.cxonechat.ui.data.ChatDeeplinkHandler
import com.nice.cxonechat.ui.data.source.AttachmentDataSource
import com.nice.cxonechat.ui.domain.usecase.NotifyUpdateUseCase
import com.nice.cxonechat.ui.screen.Screen.Offline
import com.nice.cxonechat.ui.screen.Screen.ThreadList
import com.nice.cxonechat.ui.screen.Screen.ThreadScreen
import com.nice.cxonechat.ui.storage.ValueStorage
import com.nice.cxonechat.ui.storage.ValueStorage.Companion.getStringSet
import com.nice.cxonechat.ui.storage.ValueStorage.Companion.removeFromStringSet
import com.nice.cxonechat.ui.storage.ValueStorage.Companion.setStringSet
import com.nice.cxonechat.ui.storage.ValueStorage.StringKey.RequestedPermissionsKey
import com.nice.cxonechat.ui.util.ErrorGroup
import com.nice.cxonechat.ui.util.ErrorGroup.LOW
import com.nice.cxonechat.ui.util.PermissionState.DENIED
import com.nice.cxonechat.ui.util.PermissionState.GRANTED
import com.nice.cxonechat.ui.util.PermissionState.NOT_DECLARED
import com.nice.cxonechat.ui.util.applyFixesForKeyboardInput
import com.nice.cxonechat.ui.util.checkNotificationPermissions
import com.nice.cxonechat.ui.util.getPermissionState
import com.nice.cxonechat.ui.util.koinActivityViewModel
import com.nice.cxonechat.ui.util.overrideCloseAnimation
import com.nice.cxonechat.ui.util.overrideOpenAnimation
import com.nice.cxonechat.ui.util.repeatOnOwnerLifecycle
import com.nice.cxonechat.ui.util.showRationale
import com.nice.cxonechat.ui.viewmodel.AudioRecordingViewModel
import com.nice.cxonechat.ui.viewmodel.ChatStateViewModel
import com.nice.cxonechat.ui.viewmodel.ChatThreadViewModel
import com.nice.cxonechat.ui.viewmodel.ChatThreadsViewModel
import com.nice.cxonechat.ui.viewmodel.ChatViewModel
import com.nice.cxonechat.ui.viewmodel.ConversationDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import org.koin.android.ext.android.get
import org.koin.android.ext.android.inject
import org.koin.android.scope.AndroidScopeComponent
import org.koin.androidx.scope.activityScope
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf
import org.koin.core.qualifier.named

/**
 * Chat container activity.
 */
@Suppress("TooManyFunctions")
class ChatActivity : ComponentActivity(), AndroidScopeComponent {
    internal val audioViewModel: AudioRecordingViewModel by viewModel()
    internal val valueStorage: ValueStorage by inject()
    private val chatViewModel: ChatViewModel by viewModel()
    private val chatThreadsViewModel: ChatThreadsViewModel by viewModel()
    private val chatThreadViewModel: Lazy<ChatThreadViewModel> = viewModel()
    internal val chatStateViewModel: ChatStateViewModel by viewModel()
    private val requestPermissionLauncher: ActivityResultLauncher<String> = getNotificationRequestResult()
    private val audioRequestPermissionLauncher = getAudioRequestResult()
    private val cameraRequestPermissionLauncher = getCameraRequestResult()
    internal var pendingAttachmentType: AttachmentType? = null

    override val scope by activityScope()

    private val audioRecordingManager: AudioRecordingManager by inject(parameters = { parametersOf(audioViewModel, chatStateViewModel) })

    internal val loggerScope: LoggerScope by lazy {
        LoggerScope("ChatActivity", get<Logger>(named(UiModule.LOGGER_NAME)))
    }

    private val chatAttachmentHandler by inject<ChatAttachmentHandler>(
        parameters = {
            parametersOf(chatThreadViewModel, chatStateViewModel)
        }
    )

    private val chatDeeplinkHandler by inject<ChatDeeplinkHandler>(
        parameters = {
            parametersOf(loggerScope)
        }
    )

    @Suppress(
        "LateinitUsage" // Explicitly initialized in onCreate to avoid hidden side effects
    )
    internal lateinit var activityLauncher: SelectAttachmentActivityLauncher

    override fun onCreate(savedInstanceState: Bundle?) = loggerScope.scope("onCreate") {
        super.onCreate(savedInstanceState)
        duration {
            applyFixesForKeyboardInput()
            // Explicit initialization and registration of activityLauncher
            activityLauncher = SelectAttachmentActivityLauncher(
                context = application,
                temporaryFileStorage = get(),
                sendAttachments = { attachment ->
                    lifecycleScope.launch {
                        runCatching {
                            chatAttachmentHandler.addAttachment(attachment)
                        }.onFailure { error ->
                            loggerScope.error("Failed to add attachment", error)
                            // Optionally, show a user-facing error here
                        }
                    }
                },
                registry = activityResultRegistry,
                logger = get<Logger>(named(UiModule.LOGGER_NAME))
            ).also(lifecycle::addObserver)
            setupComposableUi()
            repeatOnOwnerLifecycle { chatDeeplinkHandler.handleDeeplink(intent) }
        }
    }

    override fun onNewIntent(intent: Intent) = loggerScope.scope("onNewIntent") {
        super.onNewIntent(intent)
        duration {
            repeatOnOwnerLifecycle { chatDeeplinkHandler.handleDeeplink(intent) }
        }
    }

    override fun onPause() = loggerScope.scope("onPause") {
        super.onPause()
        duration {
            chatViewModel.close()
        }
    }

    override fun onDestroy() = loggerScope.scope("onDestroy") {
        super.onDestroy()
        duration {
            val atd = inject<AttachmentDataSource>()
            repeatOnOwnerLifecycle(state = DESTROYED) {
                if (atd.isInitialized()) {
                    atd.value.clearCache()
                }
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

    override fun finish() = loggerScope.scope("finish") {
        super.finish()
        duration {
            overrideCloseAnimation(anim.dismiss_host, anim.dismiss_chat)
        }
    }

    private fun setupComposableUi() = loggerScope.scope("setupComposableUi") {
        duration {
            enableEdgeToEdge()
            setContent {
                ChatTheme {
                    val snackbarHostState = remember { SnackbarHostState() }
                    HandleEarlyChatState { chatState ->
                        ChatUi(chatState, snackbarHostState)
                    }
                }
            }
        }
    }

    @Composable
    private fun HandleEarlyChatState(onChatReady: @Composable (ChatState) -> Unit) {
        val state by chatStateViewModel.state.collectAsState()
        val context = LocalContext.current
        when (state) {
            // if the chat isn't prepared yet, prepare it.  Hopefully it's been
            // configured by the provider.
            Initial, Preparing -> LaunchedEffect(state) {
                chatViewModel.prepare(context)
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

        val threadNotFound by chatThreadsViewModel.threadNotFound.collectAsState()
        LaunchedEffect(threadNotFound) {
            if (threadNotFound && chatViewModel.chatMode === MultiThread) {
                navController.navigate(ThreadList) {
                    popUpTo(navController.graph.findStartDestination().id) {
                        inclusive = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
                chatThreadsViewModel.resetThreadNotFound()
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
                chatViewModel.onReady()
            },
            onOfflineAction = { navController.navigate(Offline) }
        )
        ChatErrorScreen(onTerminalError = ::finish)
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
                if (dialogState !is ConversationDialog.FullScreenDialog) {
                    ThreadViewTopBar(isMultiThread, isLiveChat, scrollBehavior)
                }
            },
        ) { padding ->
            Box(modifier = Modifier.padding(padding)) {
                ThreadView(snackbarHostState = snackbarHostState)
                if (isMultiThread) BackgroundThreadUpdates()
            }
        }
    }

    private fun getChatInitialScreen(isLiveChat: Boolean, chatState: ChatState, isMultiThread: Boolean) = when {
        isLiveChat && chatState === ChatState.Offline -> Offline
        isMultiThread -> ThreadList
        else -> ThreadScreen
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
                permission.POST_NOTIFICATIONS,
                string.notifications_rationale,
                requestPermissionLauncher::launch
            )
        }
    }

    private fun ChatState.isAtLeastPrepared() = this === Connected || this === Ready || this === Prepared || this === Connecting

    @Suppress(
        "UndocumentedPublicClass", // Companion objects don't require documentation.
    )
    companion object {

        @Composable
        private fun ChatActivity.ThreadView(snackbarHostState: SnackbarHostState) {
            val chatThreadViewModel: ChatThreadViewModel = koinActivityViewModel()
            val onAttachmentSelection: (AttachmentType) -> Unit = { attachmentType ->
                // Check if camera permission is needed for this attachment type
                if (attachmentType is AttachmentType.CaptureMedia) {
                    withCameraPermission(
                        onGranted = {
                            activityLauncher.getAttachment(attachmentType)
                        },
                        onRequest = {
                            pendingAttachmentType = attachmentType
                            cameraRequestPermissionLauncher.launch(permission.CAMERA)
                        }
                    )
                } else {
                    // Non-camera attachment types don't need camera permission
                    activityLauncher.getAttachment(attachmentType)
                }
            }
            ThreadContentView(
                onAttachmentClicked = remember { { chatAttachmentHandler.onAttachmentClicked(this, it) } },
                onShare = remember {
                    {
                        lifecycleScope.launch {
                            chatAttachmentHandler.onShare(this@ThreadView, it)
                        }
                    }
                },
                closeChat = remember { { finish() } },
                onDismissRecording = remember {
                    {
                        lifecycleScope.launch {
                            audioRecordingManager.dismissRecording(this@ThreadView, audioRequestPermissionLauncher)
                        }
                    }
                },
                onError = { message ->
                    chatStateViewModel.showError(LOW, message)
                },
                onTriggerRecording = remember { { audioRecordingManager.triggerRecording(this, audioRequestPermissionLauncher) } },
                chatThreadViewModel = chatThreadViewModel,
                chatViewModel = chatViewModel,
                audioViewModel = audioViewModel,
                onAttachmentTypeSelection = remember { { onAttachmentSelection(it) } },
                snackBarHostState = snackbarHostState
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
        @Public
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
 * Checks camera permission status and executes the appropriate callback.
 *
 * This function handles the camera permission flow by checking the current permission state
 * and either executing the granted callback, showing a rationale dialog, or requesting the permission.
 * It also handles the edge case where the CAMERA permission is not declared in the manifest.
 *
 * @param onGranted Callback to execute when the camera permission is already granted.
 * @param onRequest Callback to execute when the permission needs to be requested from the user.
 */
private fun ChatActivity.withCameraPermission(onGranted: () -> Unit, onRequest: () -> Unit) =
    lifecycleScope.launch {
        loggerScope.scope("checkCameraPermission") {
            when (getPermissionState(permission.CAMERA)) {
                GRANTED -> onGranted()
                DENIED -> if (shouldShowRequestPermissionRationale(permission.CAMERA)) {
                    showRationale(string.camera_permission_rationale, onRequest)
                } else {
                    if (requestCameraPermission()) {
                        onRequest()
                    } else {
                        // Permission was requested and the user has repeatedly denied the request, show error
                        chatStateViewModel.showError(
                            errorGroup = ErrorGroup.LOW_SPECIFIC,
                            message = getString(string.camera_permission_denied_body),
                            title = getString(string.camera_permission_denied_title)
                        )
                    }
                }

                NOT_DECLARED -> runCatching {
                    // Permission not declared - try anyway with defensive error handling
                    onGranted()
                }.onFailure { throwable ->
                    error("Failed to launch intent requiring CAMERA permission without it being declared", throwable)
                    chatStateViewModel.showError(
                        errorGroup = ErrorGroup.LOW_SPECIFIC,
                        message = getString(string.camera_permission_denied_body),
                        title = getString(string.camera_permission_denied_title)
                    )
                }
            }
        }
    }

private suspend fun ChatActivity.requestCameraPermission(): Boolean {
    val requestedPermissions = valueStorage.getStringSet(RequestedPermissionsKey)
    val isFirstRequest = !requestedPermissions.contains(permission.CAMERA)
    if (isFirstRequest) {
        valueStorage.setStringSet(RequestedPermissionsKey, requestedPermissions + permission.CAMERA)
    }
    return isFirstRequest
}

/**
 * Register for notification request result.
 *
 * This function sets up an activity result launcher that will handle the result of the notification permission request.
 * If the permission is not granted, it shows a dialog informing the user about the denied permission.
 */
private fun ChatActivity.getNotificationRequestResult() =
    registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (!isGranted) {
            chatStateViewModel.showError(
                errorGroup = ErrorGroup.LOW_SPECIFIC,
                message = getString(string.no_notifications_message),
                title = getString(string.no_notifications_title)
            )
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
                valueStorage.removeFromStringSet(RequestedPermissionsKey, results.keys)
            }
        } else {
            audioViewModel.setRecordingPermissionGranted(false)
            chatStateViewModel.showError(
                errorGroup = ErrorGroup.LOW_SPECIFIC,
                message = getString(string.recording_audio_permission_denied_body),
                title = getString(string.recording_audio_permission_denied_title)
            )
        }
    }

/**
 * Register for camera permission request result.
 *
 * This function sets up an activity result launcher that will handle the result of the camera permission request.
 * If the permission is granted, it launches the camera activity immediately. If not granted, it shows a dialog
 * informing the user about the denied permission.
 */
private fun ChatActivity.getCameraRequestResult() =
    registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            lifecycleScope.launch(Dispatchers.Default) {
                valueStorage.removeFromStringSet(RequestedPermissionsKey, setOf(permission.CAMERA))
            }
            // Launch the camera activity immediately if pending
            pendingAttachmentType?.let { activityLauncher.getAttachment(it) }
            pendingAttachmentType = null
        } else {
            pendingAttachmentType = null
            chatStateViewModel.showError(
                errorGroup = ErrorGroup.LOW_SPECIFIC,
                message = getString(string.camera_permission_denied_body),
                title = getString(string.camera_permission_denied_title)
            )
        }
    }
