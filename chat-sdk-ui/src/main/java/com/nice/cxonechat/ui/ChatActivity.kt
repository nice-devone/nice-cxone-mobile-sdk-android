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

package com.nice.cxonechat.ui

import android.content.res.Resources
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.WindowManager.LayoutParams
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import coil.ImageLoader
import coil.request.ImageRequest
import coil.target.Target
import com.google.android.material.snackbar.Snackbar
import com.nice.cxonechat.ChatState
import com.nice.cxonechat.ChatState.CONNECTED
import com.nice.cxonechat.ChatState.CONNECTING
import com.nice.cxonechat.ChatState.CONNECTION_CLOSED
import com.nice.cxonechat.ChatState.CONNECTION_LOST
import com.nice.cxonechat.ChatState.INITIAL
import com.nice.cxonechat.Public
import com.nice.cxonechat.prechat.PreChatSurvey
import com.nice.cxonechat.ui.R.string
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import com.nice.cxonechat.ui.composable.theme.ChatTheme.images
import com.nice.cxonechat.ui.customvalues.mergeWithCustomField
import com.nice.cxonechat.ui.databinding.ActivityMainBinding
import com.nice.cxonechat.ui.main.ChatStateViewModel
import com.nice.cxonechat.ui.main.ChatThreadsViewModel
import com.nice.cxonechat.ui.main.ChatViewModel
import com.nice.cxonechat.ui.main.ChatViewModel.Dialogs.CustomValues
import com.nice.cxonechat.ui.main.ChatViewModel.Dialogs.EditThreadName
import com.nice.cxonechat.ui.main.ChatViewModel.Dialogs.None
import com.nice.cxonechat.ui.main.ChatViewModel.Dialogs.Survey
import com.nice.cxonechat.ui.main.ChatViewModel.NavigationState
import com.nice.cxonechat.ui.main.ChatViewModel.NavigationState.MultiThreadEnabled
import com.nice.cxonechat.ui.main.ChatViewModel.NavigationState.NavigationFinished
import com.nice.cxonechat.ui.main.ChatViewModel.NavigationState.SingleThreadCreated
import com.nice.cxonechat.ui.main.ChatViewModel.State
import com.nice.cxonechat.ui.main.ChatViewModel.State.Initial
import com.nice.cxonechat.ui.model.describe
import com.nice.cxonechat.ui.util.Ignored
import com.nice.cxonechat.ui.util.isEmpty
import com.nice.cxonechat.ui.util.showAlert
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

/**
 * Chat container activity.
 */
@AndroidEntryPoint
@Public
@Suppress("TooManyFunctions")
class ChatActivity :
    Toolbar.OnMenuItemClickListener,
    AppCompatActivity() {
    private var toolbar: Toolbar? = null
    private val chatViewModel: ChatViewModel by viewModels()
    private val chatThreadsViewModel: ChatThreadsViewModel by viewModels()
    private val chatStateViewModel: ChatStateViewModel by viewModels()

    @Suppress("LateinitUsage")
    private lateinit var binding: ActivityMainBinding

    private var chatStateSnackbar: Snackbar? = null
        set(value) {
            field?.dismiss()
            field = value
        }
    private val Int.toPx
        get() = (this * Resources.getSystem().displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyFixesForKeyboardInput()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val toolbarById = findViewById<Toolbar>(R.id.my_toolbar)
        toolbar = toolbarById
        toolbarById.title = ""
        setSupportActionBar(this.toolbar)

        setupComposableUi()

        toolbarById.inflateMenu(R.menu.default_menu)
        toolbarById.setOnMenuItemClickListener(this)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                chatViewModel.state.collect { state ->
                    when (state) {
                        Initial -> Ignored

                        is NavigationState -> {
                            if (state is MultiThreadEnabled) {
                                observeBackgroundThreadUpdates()
                            }
                            startFragmentNavigation(state)
                        }

                        is State.SingleThreadPreChatSurveyRequired -> chatViewModel.showPreChatSurvey(state.survey)
                        State.SingleThreadCreationReady -> chatViewModel.createThread()
                        is State.SingleThreadCreationFailed -> showOnThreadCreationFailure(state)
                    }
                }
            }
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                registerChatStateUiHandler()
            }
        }
    }

    /**
     * This is workaround for issue when keyboard is shown window content pans under the toolbar and keyboard overlaps
     * window contents.
     * There should be a better solution.
     */
    @Suppress("DEPRECATION")
    private fun applyFixesForKeyboardInput() {
        if (VERSION.SDK_INT >= VERSION_CODES.R) window.setDecorFitsSystemWindows(true)
        @Suppress("DEPRECATION")
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

    override fun setTitle(title: CharSequence?) {
        super.setTitle(title)

        supportActionBar?.title = title
    }

    private fun setLogo(model: Any?) {
        val context = this
        val imageLoader = ImageLoader(context)
        val target = object : Target {
            override fun onError(error: Drawable?) {
                supportActionBar?.setLogo(null)
            }

            override fun onSuccess(result: Drawable) {
                supportActionBar?.setDisplayShowHomeEnabled(true)
                supportActionBar?.setDisplayUseLogoEnabled(true)
                supportActionBar?.setLogo(result)
            }
        }
        val imageRequest = ImageRequest.Builder(context)
            .size(25.toPx)
            .data(model)
            .target(target)
            .build()
        imageLoader.enqueue(imageRequest)
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.dismiss_host, R.anim.dismiss_chat)
    }

    private fun setupComposableUi() {
        binding.composeView.setContent {
            ChatTheme {
                when (val dialog = chatViewModel.dialogShown.collectAsState().value) {
                    None -> Ignored
                    CustomValues -> BuildEditCustomValues()
                    EditThreadName -> EditThreadName()
                    is Survey -> BuildPreChatSurveyDialog(survey = dialog.survey)
                }
                setLogo(images.logo)
                supportActionBar?.setBackgroundDrawable(ColorDrawable(ChatTheme.colors.primary.toArgb()))
            }
        }
    }

    @Composable
    private fun BuildEditCustomValues() {
        EditCustomValuesDialog(
            title = stringResource(string.edit_custom_field_title),
            fields = chatViewModel.preChatSurvey?.fields
                .orEmpty()
                .mergeWithCustomField(
                    chatViewModel.customValues
                ),
            onCancel = chatViewModel::cancelEditingCustomValues,
            onConfirm = chatViewModel::confirmEditingCustomValues
        )
    }

    @Composable
    private fun EditThreadName() {
        EditThreadNameDialog(
            threadName = chatViewModel.selectedThreadName ?: "",
            onCancel = chatViewModel::dismissDialog,
            onAccept = chatViewModel::confirmEditThreadName
        )
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
        chatViewModel.reportOnResume()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.default_menu, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        if (menu != null) {
            val navController = findNavController(R.id.nav_host_fragment)
            val isInChat = navController.currentDestination?.id == R.id.chatThreadFragment
            val isMultiThread = chatViewModel.isMultiThreadEnabled
            val hasQuestions = chatViewModel.preChatSurvey?.fields?.isEmpty() == false

            with(menu) {
                findItem(R.id.action_thread_name)?.isVisible = isInChat && isMultiThread
                findItem(R.id.action_custom_values)?.isVisible = isInChat && hasQuestions
            }
        }

        return super.onPrepareOptionsMenu(menu)
    }

    override fun onMenuItemClick(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.action_custom_values -> editCustomValues()
            R.id.action_thread_name -> showEditThreadNameDialog()
        }
        return true
    }

    private suspend fun registerChatStateUiHandler() {
        chatStateViewModel.state.collect { state: ChatState ->
            when (state) {
                INITIAL -> Ignored

                CONNECTING -> chatStateSnackbar = Snackbar.make(
                    binding.root,
                    string.chat_state_connecting,
                    Snackbar.LENGTH_INDEFINITE
                ).setAction(string.chat_state_connecting_action_cancel) {
                    finish() // Dirty hack - refactor together with DI
                }.apply(Snackbar::show)

                CONNECTED -> chatStateSnackbar = Snackbar.make(
                    binding.root,
                    string.chat_state_connected,
                    Snackbar.LENGTH_SHORT
                ).apply(Snackbar::show)

                CONNECTION_LOST -> chatStateSnackbar = Snackbar.make(
                    binding.root,
                    string.chat_state_connection_lost,
                    Snackbar.LENGTH_INDEFINITE
                ).setAction(string.chat_state_connection_lost_action_reconnect) {
                    chatViewModel.reconnect()
                }.apply(Snackbar::show)

                CONNECTION_CLOSED -> chatStateSnackbar = Snackbar.make(
                    binding.root,
                    string.chat_state_connection_closed,
                    Snackbar.LENGTH_INDEFINITE
                ).setAction(string.chat_state_connection_closed_action_restart) {
                    finish()
                }.apply(Snackbar::show)
            }
        }
    }

    private fun showEditThreadNameDialog() {
        chatViewModel.editThreadName()
    }

    private fun editCustomValues() {
        chatViewModel.startEditingCustomValues()
    }

    private fun showOnThreadCreationFailure(state: State.SingleThreadCreationFailed) {
        showAlert(describe(state.failure), onClick = chatViewModel::dismissThreadCreationFailure)
    }
}
