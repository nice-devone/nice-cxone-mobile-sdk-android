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

package com.nice.cxonechat.sample

import android.content.Context
import android.net.Uri
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.amazon.identity.auth.device.AuthError
import com.amazon.identity.auth.device.api.authorization.AuthCancellation
import com.amazon.identity.auth.device.api.authorization.AuthorizationManager
import com.amazon.identity.auth.device.api.authorization.AuthorizeListener
import com.amazon.identity.auth.device.api.authorization.AuthorizeRequest
import com.amazon.identity.auth.device.api.authorization.AuthorizeResult
import com.amazon.identity.auth.device.api.authorization.ProfileScope
import com.amazon.identity.auth.device.api.workflow.RequestContext
import com.nice.cxonechat.log.Logger
import com.nice.cxonechat.log.LoggerScope
import com.nice.cxonechat.log.debug
import com.nice.cxonechat.log.error
import com.nice.cxonechat.log.info
import com.nice.cxonechat.log.scope
import com.nice.cxonechat.sample.R.string
import com.nice.cxonechat.sample.data.models.ChatAuthorization
import com.nice.cxonechat.sample.ui.CartScreen
import com.nice.cxonechat.sample.ui.ConfirmationScreen
import com.nice.cxonechat.sample.ui.PaymentScreen
import com.nice.cxonechat.sample.ui.ProductListScreen
import com.nice.cxonechat.sample.ui.ProductScreen
import com.nice.cxonechat.sample.ui.Screen
import com.nice.cxonechat.sample.ui.theme.AppTheme
import com.nice.cxonechat.sample.utilities.PKCE
import com.nice.cxonechat.sample.viewModel.StoreViewModel
import com.nice.cxonechat.sample.viewModel.UiState
import com.nice.cxonechat.sample.viewModel.UiState.UiStateContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.get
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.io.File
import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicReference

/**
 * Store activity hosting Compose Navigation-based sample host application and integration.
 */
class StoreActivity : ComponentActivity(), UiStateContext {
    private val storeViewModel: StoreViewModel by viewModel()
    private val pageViewHandler get() = storeViewModel.analyticsHandler

    private val requestContext by lazy {
        RequestContext.create(this as Context)
    }

    private val pickMedia = registerForActivityResult(PickVisualMedia()) { pickedUri ->
        // Callback is invoked after the user selects a media item or closes the
        // photo picker.
        lifecycleScope.launch {
            val uriString = copyUriInputToLocalFile(pickedUri)
            onPickImageCallback.getAndSet(null)?.get()?.invoke(uriString)
        }
    }

    private val onPickImageCallback = AtomicReference<WeakReference<(String?) -> Unit>?>(null)

    private val logger by lazy { LoggerScope(TAG, get()) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (VERSION.SDK_INT >= VERSION_CODES.S) {
            window.setHideOverlayWindows(true)
        }

        setContent {
            AppTheme {
                Screen()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        requestContext.onResume()

        // Let the viewModels know activity has been resumed to properly track page view.
        storeViewModel.onResume()
        pageViewHandler.onResume()
    }

    override fun onPause() {
        // let the viewModels know activity has been paused to properly track page view.
        pageViewHandler.onPause()
        super.onPause()
    }

    @Composable
    private fun Screen() {
        NavScreen()
        PresentDialogs(storeViewModel.uiState.collectAsState().value)
    }

    /**
     * Base screen for sample application, defines.
     */
    @Composable
    private fun NavScreen() {
        val navHostController = rememberNavController().apply {
            setLifecycleOwner(this@StoreActivity)
            enableOnBackPressed(true)
        }

        NavHost(
            navHostController,
            startDestination = ProductListScreen.defaultRoute,
        ) {
            screens.forEach {
                it.navigation(this, navHostController, storeViewModel)
            }
        }
    }

    /**
     * Overlay any required dialogs.
     */
    @Composable
    private fun PresentDialogs(uiState: UiState) = logger.scope("PresentDialogs") {
        debug("OverlayDialogs: uiState=$uiState")

        uiState.Content(this@StoreActivity)
    }

    override fun pickImage(onPickImage: (String?) -> Unit) {
        onPickImageCallback.set(WeakReference(onPickImage))
        // Launch the photo picker and let the user choose only images.
        pickMedia.launch(PickVisualMediaRequest(ImageOnly))
    }

    override fun loginWithAmazon() = logger.scope("loginWithAmazon") {
        val (codeVerifier, codeChallenge) = PKCE.generateCodeVerifier()

        requestContext.registerListener(LoggingAuthorizeListener(codeVerifier, storeViewModel, this))

        AuthorizationManager.authorize(
            AuthorizeRequest.Builder(requestContext)
                .addScopes(ProfileScope.profile(), ProfileScope.postalCode())
                .forGrantType(AuthorizeRequest.GrantType.AUTHORIZATION_CODE)
                .withProofKeyParameters(codeChallenge, "S256")
                .build()
        )
    }

    private inner class LoggingAuthorizeListener(
        private val codeVerifier: String,
        private val storeViewModel: StoreViewModel,
        logger: Logger,
    ) : AuthorizeListener(), LoggerScope by LoggerScope<AuthorizeListener>(logger) {
        override fun onSuccess(result: AuthorizeResult?) = scope("onSuccess") {
            result?.accessToken?.let { accessToken ->
                storeViewModel.chatSettingsHandler.setAuthorization(ChatAuthorization(codeVerifier, accessToken))
            } ?: error("loginWithAmazon success with no result")
        }

        override fun onError(authError: AuthError?) = scope("onError") {
            info("LoginWithAmazon: ${authError?.message ?: getString(string.unknown_error)}")
        }

        override fun onCancel(authCancellation: AuthCancellation?) = scope("onCancel") {
            info("LoginWithAmazon: ${authCancellation?.description}")
        }
    }

    private companion object {
        private const val TAG = "StoreActivity"

        val screens: List<Screen> = listOf(
            ProductListScreen,
            ProductScreen,
            CartScreen,
            PaymentScreen,
            ConfirmationScreen,
        )

        suspend fun Context.copyUriInputToLocalFile(
            pickedUri: Uri?
        ): String? = withContext(Dispatchers.IO) {
            runCatching {
                val uri = checkNotNull(pickedUri)
                val localCopy = File(filesDir, "logo")
                checkNotNull(contentResolver.openInputStream(uri))
                    .use { inputStream -> localCopy.outputStream().use(inputStream::copyTo) }
                localCopy.toUri().toString()
            }.getOrNull()
        }
    }
}
