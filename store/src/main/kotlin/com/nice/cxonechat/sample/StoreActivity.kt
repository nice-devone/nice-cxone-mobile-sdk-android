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

package com.nice.cxonechat.sample

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.nice.cxonechat.log.LoggerScope
import com.nice.cxonechat.log.debug
import com.nice.cxonechat.log.error
import com.nice.cxonechat.log.scope
import com.nice.cxonechat.sample.data.models.ChatAuthorization
import com.nice.cxonechat.sample.data.models.StoreLaunchRequest
import com.nice.cxonechat.sample.data.models.StoreLaunchRequestResult.Malformed
import com.nice.cxonechat.sample.data.models.StoreLaunchRequestResult.None
import com.nice.cxonechat.sample.data.models.StoreLaunchRequestResult.Supplied
import com.nice.cxonechat.sample.ui.CartScreen
import com.nice.cxonechat.sample.ui.ConfirmationScreen
import com.nice.cxonechat.sample.ui.PaymentScreen
import com.nice.cxonechat.sample.ui.ProductListScreen
import com.nice.cxonechat.sample.ui.ProductScreen
import com.nice.cxonechat.sample.ui.Screen
import com.nice.cxonechat.sample.ui.theme.AppTheme
import com.nice.cxonechat.sample.utilities.AuthConfiguration
import com.nice.cxonechat.sample.utilities.toStoreLaunchRequestResult
import com.nice.cxonechat.sample.viewModel.PendingLaunchConfiguration
import com.nice.cxonechat.sample.viewModel.StoreViewModel
import com.nice.cxonechat.sample.viewModel.UiState
import com.nice.cxonechat.sample.viewModel.UiState.UiStateContext
import com.nice.cxonechat.ui.screen.ChatActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.openid.appauth.AuthState
import net.openid.appauth.AuthorizationException
import net.openid.appauth.AuthorizationRequest
import net.openid.appauth.AuthorizationResponse
import net.openid.appauth.AuthorizationService
import net.openid.appauth.ResponseTypeValues
import org.koin.android.ext.android.get
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.io.File
import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicReference

/**
 * Store activity hosting Compose Navigation-based sample host application and integration.
 */
@Suppress("TooManyFunctions")
class StoreActivity : ComponentActivity(), UiStateContext {
    private val storeViewModel: StoreViewModel by viewModel()
    private val pageViewHandler get() = storeViewModel.analyticsHandler

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

    private val authConfig: AuthConfiguration by lazy { AuthConfiguration.from(this) }
    private val authService: AuthorizationService by lazy { AuthorizationService(this) }

    private val implicitAuthLauncher = registerForActivityResult(StartActivityForResult()) { result ->
        handleImplicitAuthResult(result)
    }

    private val explicitAuthLauncher = registerForActivityResult(StartActivityForResult()) { result ->
        handleExplicitAuthResult(result)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Deposited before the first access to storeViewModel, which is what creates it -- the
        // ViewModel picks the request up exactly where it decides between a stored configuration and
        // the configuration dialog. Skipped on a restore, so a recreated activity doesn't reapply a
        // configuration the user has since changed.
        if (savedInstanceState == null) {
            readLaunchRequest(intent)?.let(get<PendingLaunchConfiguration>()::offer)
        }

        setContent {
            enableEdgeToEdge()
            AppTheme {
                Screen()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)

        // The ViewModel already exists on this path, so the request goes straight to it. Validation
        // is deliberately the same as in onCreate -- a warm start must not be the weaker door.
        readLaunchRequest(intent)?.let(storeViewModel::applyLaunchRequest)
    }

    /**
     * Read the automated launch instructions carried by [intent], if any.
     *
     * A malformed request is reported and dropped rather than partially applied, so that a mistyped
     * `adb shell am start` fails visibly instead of quietly connecting somewhere unexpected.
     *
     * @param intent Intent which started or restarted this activity.
     * @return What the intent asked for, or null if it asked for nothing usable.
     */
    private fun readLaunchRequest(intent: Intent): StoreLaunchRequest? = logger.scope("readLaunchRequest") {
        when (val result = intent.toStoreLaunchRequestResult()) {
            None -> null

            is Malformed -> {
                error("Ignoring launch intent: ${result.reason}")
                null
            }

            is Supplied -> result.request
        }
    }

    override fun onResume() {
        super.onResume()

        // Let the viewModels know activity has been resumed to properly track page view.
        storeViewModel.onResume()
        pageViewHandler.onResume()
    }

    override fun onPause() {
        // let the viewModels know activity has been paused to properly track page view.
        pageViewHandler.onPause()
        super.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        authService.dispose()
    }

    @Composable
    private fun Screen() {
        // The storefront is skipped while a launch intent is driving the app to the chat: building
        // it would load the product list and its images for a screen nobody is going to look at. It
        // appears when the chat is left again, or when the setup ends without opening the chat.
        if (storeViewModel.launchPresentation.isStorefrontVisible.collectAsState().value) {
            NavScreen()
        }
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

    override fun openChat() = ChatActivity.startChat(this)

    override fun requestImplicitToken() = logger.scope("requestImplicitToken") {
        debug("requestImplicitToken — launching AppAuth authorization")
        try {
            val request = AuthorizationRequest.Builder(
                authConfig.serviceConfiguration,
                authConfig.androidClientId,
                ResponseTypeValues.CODE,
                authConfig.redirectUri,
            )
                .setScope(authConfig.scope)
                .setAdditionalParameters(mapOf("access_type" to "offline"))
                .build()
            implicitAuthLauncher.launch(authService.getAuthorizationRequestIntent(request))
        } catch (e: ActivityNotFoundException) {
            error("No browser available for implicit auth", e)
            storeViewModel.deliverImplicitTokenFailure(e)
        } catch (e: IllegalStateException) {
            error("Failed to build implicit auth request", e)
            storeViewModel.deliverImplicitTokenFailure(e)
        }
    }

    private fun handleImplicitAuthResult(result: ActivityResult) = logger.scope("handleImplicitAuthResult") {
        val data = result.data
        if (data == null) {
            storeViewModel.deliverImplicitTokenFailure(Exception("Authorization cancelled"))
            return@scope
        }

        val response = AuthorizationResponse.fromIntent(data)
        val exception = AuthorizationException.fromIntent(data)

        if (exception != null || response == null) {
            storeViewModel.deliverImplicitTokenFailure(
                exception ?: Exception("No authorization response received")
            )
            return@scope
        }

        val authState = AuthState(authConfig.serviceConfiguration)
        authState.update(response, null)

        authService.performTokenRequest(response.createTokenExchangeRequest()) { tokenResponse, tokenException ->
            authState.update(tokenResponse, tokenException)

            val accessToken = tokenResponse?.accessToken
            // accessTokenExpirationTime is in millis (Date.time); null if the provider omitted it.
            val expiryMillis = tokenResponse?.accessTokenExpirationTime
            when {
                tokenException != null -> storeViewModel.deliverImplicitTokenFailure(tokenException)
                accessToken != null -> {
                    storeViewModel.chatSettingsHandler.saveAuthStateJson(authState.jsonSerializeString())
                    storeViewModel.deliverImplicitToken(accessToken, expiryMillis)
                }
                else -> storeViewModel.deliverImplicitTokenFailure(Exception("No access token in token response"))
            }
        }
    }

    override fun requestExplicitToken() = logger.scope("requestExplicitToken") {
        debug("requestExplicitToken — launching AppAuth PKCE authorization")
        try {
            val request = AuthorizationRequest.Builder(
                authConfig.serviceConfiguration,
                authConfig.androidClientId,
                ResponseTypeValues.CODE,
                authConfig.redirectUri,
            )
                .setScope(authConfig.scope)
                .setAdditionalParameters(mapOf("access_type" to "offline"))
                .build()
            explicitAuthLauncher.launch(authService.getAuthorizationRequestIntent(request))
        } catch (e: ActivityNotFoundException) {
            error("No browser available for explicit auth", e)
        } catch (e: IllegalStateException) {
            error("Failed to build explicit auth request", e)
        }
    }

    private fun handleExplicitAuthResult(result: ActivityResult) = logger.scope("handleExplicitAuthResult") {
        val data = result.data
        if (data == null) {
            debug("Explicit authorization cancelled by user")
            return@scope
        }

        val response = AuthorizationResponse.fromIntent(data)
        val exception = AuthorizationException.fromIntent(data)

        if (exception != null || response == null) {
            error("Explicit auth failed", exception ?: Exception("No authorization response"))
            return@scope
        }

        val code = response.authorizationCode
        val verifier = response.request.codeVerifier

        if (code == null || verifier == null) {
            error("Missing authorization code or code verifier in response")
            return@scope
        }

        storeViewModel.setAuthorization(ChatAuthorization(code = code, verifier = verifier))
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
            pickedUri: Uri?,
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
