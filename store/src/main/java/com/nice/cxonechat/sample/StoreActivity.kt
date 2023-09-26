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

package com.nice.cxonechat.sample

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import androidx.core.net.toUri
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
import com.nice.cxonechat.UserName
import com.nice.cxonechat.sample.R.string
import com.nice.cxonechat.sample.UiState.CONFIGURATION
import com.nice.cxonechat.sample.UiState.CONNECTING
import com.nice.cxonechat.sample.UiState.INITIAL
import com.nice.cxonechat.sample.UiState.LOGIN
import com.nice.cxonechat.sample.UiState.OAUTH
import com.nice.cxonechat.sample.UiState.UI_SETTINGS
import com.nice.cxonechat.sample.data.models.ChatAuthorization
import com.nice.cxonechat.sample.data.models.SdkConfiguration
import com.nice.cxonechat.sample.data.models.SdkConfigurations
import com.nice.cxonechat.sample.data.repository.UISettings
import com.nice.cxonechat.sample.extensions.Ignored
import com.nice.cxonechat.sample.ui.BusySpinner
import com.nice.cxonechat.sample.ui.CartScreen
import com.nice.cxonechat.sample.ui.ConfirmationScreen
import com.nice.cxonechat.sample.ui.LoginDialog
import com.nice.cxonechat.sample.ui.PaymentScreen
import com.nice.cxonechat.sample.ui.ProductListScreen
import com.nice.cxonechat.sample.ui.ProductScreen
import com.nice.cxonechat.sample.ui.Screen
import com.nice.cxonechat.sample.ui.SdkConfigurationDialog
import com.nice.cxonechat.sample.ui.theme.AppTheme
import com.nice.cxonechat.sample.ui.uisettings.UISettingsDialog
import com.nice.cxonechat.sample.utilities.PKCE
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicReference

/**
 * Store activity hosting Compose Navigation-based sample host application and integration.
 */
@AndroidEntryPoint
class StoreActivity : ComponentActivity() {
    private val viewModel by viewModels<StoreViewModel>()
    private val requestContext by lazy {
        RequestContext.create(this as Context)
    }

    private val pickMedia = registerForActivityResult(PickVisualMedia()) { pickedUri ->
        // Callback is invoked after the user selects a media item or closes the
        // photo picker.
        val uriString = runCatching {
            val uri = checkNotNull(pickedUri)
            val localCopy = File(filesDir, "logo")
            checkNotNull(contentResolver.openInputStream(uri))
                .use { inputStream -> localCopy.outputStream().use(inputStream::copyTo) }
            localCopy.toUri().toString()
        }.getOrNull()
        onPickImageCallback.getAndSet(null)?.get()?.invoke(uriString)
    }

    private val onPickImageCallback = AtomicReference<WeakReference<(String?) -> Unit>?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            AppTheme {
                Screen()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        requestContext.onResume()
        viewModel.startChat()
    }

    @Composable
    private fun Screen() {
        val uiState = viewModel.uiState.collectAsState()
        val settings = viewModel.chatSettingsRepository.settings.collectAsState()
        val configuration = remember { derivedStateOf { settings.value?.sdkConfiguration } }
        val userName = remember { derivedStateOf { settings.value?.userName } }
        val configurations = viewModel.sdkConfigurationListRepository.configurationList.collectAsState()

        Log.d(TAG, "Screen: uiState=${uiState.value}")

        NavScreen()
        PresentDialogs(
            uiState.value,
            configuration.value,
            configurations.value,
            userName.value,
            viewModel::cancelConnect
        )
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
                it.navigation(this, navHostController, viewModel)
            }
        }
    }

    /**
     * Overlay any required dialogs.
     */
    @Composable
    private fun PresentDialogs(
        uiState: UiState,
        configuration: SdkConfiguration?,
        configurations: SdkConfigurations,
        userName: UserName?,
        cancelConnect: () -> Unit
    ) {
        Log.d(TAG, "OverlayDialogs: uiState=$uiState")

        when (uiState) {
            INITIAL -> BusySpinner(message = stringResource(string.loading))

            CONFIGURATION -> SdkConfigurationDialog(
                configuration,
                configurations,
                { viewModel.cancelConfigurationDialog() },
                viewModel::setConfiguration
            )

            CONNECTING -> BusySpinner(
                message = stringResource(string.connecting),
                onCancel = cancelConnect
            )

            LOGIN -> LoginDialog(userName, viewModel::setUserName)

            OAUTH -> loginWithAmazon()

            UI_SETTINGS -> UISettingsDialog(
                value = UISettings.collectAsState().value,
                onDismiss = viewModel::cancelUiSettings,
                pickImage = ::pickImage,
            ) {
                viewModel.uiSettingsRepository.save(it)
            }

            else -> Ignored
        }
    }

    private fun pickImage(onPickImage: (String?) -> Unit) {
        onPickImageCallback.set(WeakReference(onPickImage))
        // Launch the photo picker and let the user choose only images.
        pickMedia.launch(PickVisualMediaRequest(ImageOnly))
    }

    private fun loginWithAmazon() {
        val (codeVerifier, codeChallenge) = PKCE.generateCodeVerifier()

        requestContext.registerListener(object : AuthorizeListener() {
            override fun onSuccess(result: AuthorizeResult?) {
                result?.accessToken?.let { accessToken ->
                    viewModel.setAuthorization(ChatAuthorization(codeVerifier, accessToken))
                } ?: Log.e(TAG, "loginWithAmazon success with no result")
            }

            override fun onError(p0: AuthError?) {
                Log.i(TAG, "LoginWithAmazon: ${p0?.message ?: getString(string.unknown_error)}")
            }

            override fun onCancel(p0: AuthCancellation?) {
                Log.i(TAG, "LoginWithAmazon: ${p0?.description}")
            }
        })

        AuthorizationManager.authorize(
            AuthorizeRequest.Builder(requestContext)
                .addScopes(ProfileScope.profile(), ProfileScope.postalCode())
                .forGrantType(AuthorizeRequest.GrantType.AUTHORIZATION_CODE)
                .withProofKeyParameters(codeChallenge, "S256")
                .build()
        )
    }

    companion object {
        private const val TAG = "StoreActivity"

        private val screens: List<Screen> = listOf(
            ProductListScreen,
            ProductScreen,
            CartScreen,
            PaymentScreen,
            ConfirmationScreen,
        )
    }
}
