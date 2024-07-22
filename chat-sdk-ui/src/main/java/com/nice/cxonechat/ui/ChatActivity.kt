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

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.os.Bundle
import android.view.Menu
import android.view.WindowManager.LayoutParams
import androidx.annotation.AnimRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Lifecycle.State.DESTROYED
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import com.google.android.material.snackbar.Snackbar
import com.nice.cxonechat.ChatMode.LiveChat
import com.nice.cxonechat.ChatMode.MultiThread
import com.nice.cxonechat.ChatState
import com.nice.cxonechat.ChatState.Connected
import com.nice.cxonechat.ChatState.Connecting
import com.nice.cxonechat.ChatState.ConnectionLost
import com.nice.cxonechat.ChatState.Initial
import com.nice.cxonechat.ChatState.Offline
import com.nice.cxonechat.ChatState.Prepared
import com.nice.cxonechat.ChatState.Preparing
import com.nice.cxonechat.ChatState.Ready
import com.nice.cxonechat.Public
import com.nice.cxonechat.exceptions.RuntimeChatException.AuthorizationError
import com.nice.cxonechat.prechat.PreChatSurvey
import com.nice.cxonechat.ui.R.anim
import com.nice.cxonechat.ui.R.string
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import com.nice.cxonechat.ui.databinding.ActivityMainBinding
import com.nice.cxonechat.ui.main.ChatStateViewModel
import com.nice.cxonechat.ui.main.ChatThreadsViewModel
import com.nice.cxonechat.ui.main.ChatViewModel
import com.nice.cxonechat.ui.main.ChatViewModel.Dialogs.None
import com.nice.cxonechat.ui.main.ChatViewModel.Dialogs.Survey
import com.nice.cxonechat.ui.main.ChatViewModel.NavigationState
import com.nice.cxonechat.ui.main.ChatViewModel.NavigationState.MultiThreadEnabled
import com.nice.cxonechat.ui.main.ChatViewModel.NavigationState.NavigationFinished
import com.nice.cxonechat.ui.main.ChatViewModel.NavigationState.SingleThreadCreated
import com.nice.cxonechat.ui.main.ChatViewModel.State
import com.nice.cxonechat.ui.main.ChatViewModel.State.CreateSingleThread
import com.nice.cxonechat.ui.main.ChatViewModel.State.SingleThreadCreationFailed
import com.nice.cxonechat.ui.main.ChatViewModel.State.SingleThreadPreChatSurveyRequired
import com.nice.cxonechat.ui.model.describe
import com.nice.cxonechat.ui.util.Ignored
import com.nice.cxonechat.ui.util.isEmpty
import com.nice.cxonechat.ui.util.showAlert
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.util.UUID
import java.util.concurrent.CancellationException
import com.nice.cxonechat.ui.main.ChatViewModel.NavigationState.Offline as NavigationOffline

/**
 * Chat container activity.
 */
@Public
@Suppress("TooManyFunctions")
class ChatActivity : AppCompatActivity() {
    private val chatViewModel: ChatViewModel by viewModel()
    private val chatThreadsViewModel: ChatThreadsViewModel by viewModel()
    private val chatStateViewModel: ChatStateViewModel by viewModel()
    private val closing
        get() = lifecycle.currentState == DESTROYED

    @Suppress("LateinitUsage")
    private lateinit var binding: ActivityMainBinding

    private var chatStateSnackbar: Snackbar? = null
        set(value) {
            field?.dismiss()
            field = value
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        applyFixesForKeyboardInput()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupComposableUi()

        registerHandler(::handleChatState)
        registerChatModelStateHandler()
        registerHandler(::handleErrorStates)
    }

    private fun registerChatModelStateHandler() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                var job: Job? = null
                chatStateViewModel.state.collect {
                    job = if (listOf(Ready, Offline).contains(it)) {
                        handleChatModelState()
                    } else {
                        job?.cancel(CancellationException("State: $it"))
                        null
                    }
                }
            }
        }
    }

    private fun CoroutineScope.handleChatModelState() = launch {
        chatViewModel.state.collect { state ->
            when (state) {
                State.Initial -> Ignored
                is NavigationState -> {
                    if (state is MultiThreadEnabled || state is NavigationFinished) {
                        observeBackgroundThreadUpdates()
                    }
                    startFragmentNavigation(state)
                    if (state is MultiThreadEnabled) {
                        intent?.handleDeeplink()
                    }
                }

                CreateSingleThread -> chatViewModel.createThread()
                is SingleThreadPreChatSurveyRequired -> chatViewModel.showPreChatSurvey(state.survey)
                is SingleThreadCreationFailed -> showOnThreadCreationFailure(state)
            }
        }
    }

    private fun registerHandler(
        handler: suspend () -> Unit,
        repeatOnLifecycleState: Lifecycle.State = Lifecycle.State.RESUMED,
    ) {
        lifecycleScope.launch {
            repeatOnLifecycle(repeatOnLifecycleState) {
                handler()
            }
        }
    }

    private suspend fun handleErrorStates() {
        chatStateViewModel.chatErrorState.collect {
            if (it is AuthorizationError) {
                AlertDialog.Builder(this)
                    .setMessage(string.chat_state_error_default_message)
                    .setCancelable(false)
                    .setNeutralButton(string.chat_state_error_action_close) { _, _ -> finish() }
                    .setOnDismissListener { finish() }
                    .create()
                    .show()
            } else {
                chatStateSnackbar = Snackbar.make(
                    binding.root,
                    it.message ?: getText(string.chat_state_error_default_message),
                    Snackbar.LENGTH_SHORT
                ).also(Snackbar::show)
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                intent?.handleDeeplink()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        chatViewModel.close()
    }

    /**
     * This is workaround for issue when keyboard is shown window content pans under the toolbar and keyboard overlaps
     * window contents.
     * There should be a better solution.
     */
    @Suppress("DEPRECATION")
    private fun applyFixesForKeyboardInput() {
        if (VERSION.SDK_INT >= VERSION_CODES.R) window.setDecorFitsSystemWindows(true)
        window.setSoftInputMode(LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
    }

    private fun CoroutineScope.observeBackgroundThreadUpdates() = launch {
        chatThreadsViewModel.backgroundThreadsFlow.filterNotNull().collect {
            Snackbar.make(
                binding.root,
                getString(string.background_thread_updated, it.chatThread.threadName.orEmpty()),
                Snackbar.LENGTH_SHORT
            ).show()
        }
    }

    override fun finish() {
        super.finish()

        overrideCloseAnimation(anim.dismiss_host, anim.dismiss_chat)
    }

    private fun setupComposableUi() {
        binding.composeView.setContent {
            ChatTheme {
                when (val dialog = chatViewModel.dialogShown.collectAsState().value) {
                    None -> Ignored
                    is Survey -> BuildPreChatSurveyDialog(survey = dialog.survey)
                }
            }
        }
    }

    @Composable
    private fun BuildPreChatSurveyDialog(survey: PreChatSurvey) {
        PreChatSurveyDialog(
            survey = survey,
            onCancel = ::finish,
            onValidSurveySubmission = chatViewModel::respondToSurvey,
        )
    }

    private fun startFragmentNavigation(state: NavigationState) {
        val navigationStart = when (state) {
            NavigationOffline -> R.navigation.offline
            MultiThreadEnabled -> R.navigation.threads
            SingleThreadCreated -> R.navigation.chat
            NavigationFinished -> return
        }

        val navHostFragment = NavHostFragment.create(navigationStart)
        supportFragmentManager.beginTransaction()
            .replace(R.id.nav_host_fragment, navHostFragment)
            .setPrimaryNavigationFragment(navHostFragment)
            .commitNow()

        navHostFragment.navController.addOnDestinationChangedListener { _, _, _ ->
            invalidateOptionsMenu()
        }

        chatViewModel.setNavigationFinishedState()

        invalidateOptionsMenu()
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            chatStateViewModel.state.filter { it == Connected }.firstOrNull()?.also {
                chatViewModel.reportOnResume()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.default_menu, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        if (menu != null && chatStateViewModel.state.value == Connected) {
            val navController = findNavController(R.id.nav_host_fragment)
            val isInChat = navController.currentDestination?.id == R.id.offlineFragment
            val isMultiThread = chatViewModel.chatMode === MultiThread
            val isLiveChat = chatViewModel.chatMode === LiveChat
            val hasQuestions = chatViewModel.preChatSurvey?.fields?.isEmpty() == false

            with(menu) {
                findItem(R.id.action_thread_name)?.isVisible = isInChat && isMultiThread
                findItem(R.id.action_custom_values)?.isVisible = isInChat && hasQuestions
                findItem(R.id.action_end_contact)?.isVisible = isInChat && isLiveChat
            }
        }

        return super.onPrepareOptionsMenu(menu)
    }

    private suspend fun handleChatState() {
        chatStateViewModel.state.collect { state: ChatState ->
            when (state) {
                // if the chat isn't prepared yet, prepare it.  Hopefully it's been
                // configured by the provider.
                Initial -> chatViewModel.prepare(applicationContext)

                Preparing -> chatStateSnackbar = Snackbar.make(
                    binding.root,
                    getString(string.preparing_sdk),
                    Snackbar.LENGTH_INDEFINITE
                ).setAction(string.chat_state_connecting_action_cancel) {
                    finish()
                }.apply(Snackbar::show)

                // if the chat is (or becomes) prepared, then start a connect attempt
                Prepared -> if (!closing) {
                    chatViewModel.connect()
                }

                Connecting -> chatStateSnackbar = Snackbar.make(
                    binding.root,
                    getString(string.chat_state_connecting),
                    Snackbar.LENGTH_INDEFINITE
                ).setAction(string.chat_state_connecting_action_cancel) {
                    finish()
                }.apply(Snackbar::show)

                Connected -> chatStateSnackbar = Snackbar.make(
                    binding.root,
                    string.chat_state_connected,
                    Snackbar.LENGTH_SHORT
                ).apply(Snackbar::show)

                Ready -> chatStateSnackbar = Snackbar.make(
                    binding.root,
                    "SDK ready",
                    Snackbar.LENGTH_SHORT
                ).apply(Snackbar::show)

                Offline -> chatStateSnackbar = Snackbar.make(
                    binding.root,
                    "SDK OFFLINE",
                    Snackbar.LENGTH_SHORT
                )

                ConnectionLost -> chatStateSnackbar = Snackbar.make(
                    binding.root,
                    string.chat_state_connection_lost,
                    Snackbar.LENGTH_INDEFINITE
                ).setAction(string.chat_state_connection_lost_action_reconnect) {
                    chatViewModel.connect()
                }.apply(Snackbar::show)
            }
        }
    }

    private fun showOnThreadCreationFailure(state: SingleThreadCreationFailed) {
        showAlert(describe(state.failure), onClick = chatViewModel::refreshThreadState)
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
        private fun Activity.overrideOpenAnimation(
            @AnimRes enterAnim: Int,
            @AnimRes exitAnim: Int,
        ) {
            if (VERSION.SDK_INT < VERSION_CODES.UPSIDE_DOWN_CAKE) {
                @Suppress("DEPRECATION")
                overridePendingTransition(enterAnim, exitAnim)
            } else {
                overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, enterAnim, exitAnim)
            }
        }

        /*
         * This could be defined as a normal method on ChatActivity, but this seems to keep it paired with
         * overrideCloseAnimation better.
         */
        private fun Activity.overrideCloseAnimation(
            @AnimRes enterAnim: Int,
            @AnimRes exitAnim: Int,
        ) {
            if (VERSION.SDK_INT < VERSION_CODES.UPSIDE_DOWN_CAKE) {
                @Suppress("DEPRECATION")
                overridePendingTransition(enterAnim, exitAnim)
            } else {
                overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE, enterAnim, exitAnim)
            }
        }

        /**
         * Start the [ChatActivity] from a given source activity.
         *
         * @param from Activity to use as a base for the new [ChatActivity].
         */
        fun startChat(from: Activity) {
            from.startActivity(Intent(from, ChatActivity::class.java))
            from.overrideOpenAnimation(anim.present_chat, anim.present_host)
        }
    }
}

private fun Uri.parseThreadDeeplink(): Result<UUID> = runCatching {
    val threadIdString = getQueryParameter("idOnExternalPlatform")
    require(!threadIdString.isNullOrEmpty()) { "Invalid threadId in $this" }
    UUID.fromString(threadIdString)
}
