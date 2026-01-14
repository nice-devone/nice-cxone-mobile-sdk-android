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

package com.nice.cxonechat.sample.viewModel

import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.nice.cxonechat.sample.R.string
import com.nice.cxonechat.sample.data.repository.UISettings
import com.nice.cxonechat.sample.ui.ErrorDialog
import com.nice.cxonechat.sample.ui.LoginDialog
import com.nice.cxonechat.sample.ui.SdkConfigurationDialog
import com.nice.cxonechat.sample.ui.theme.AppTheme
import com.nice.cxonechat.sample.ui.theme.OutlinedButton
import com.nice.cxonechat.sample.ui.uisettings.UISettingsDialog
import com.nice.cxonechat.ui.composable.theme.BusySpinner
import org.koin.androidx.compose.koinViewModel

/**
 * Current state of the UI.
 *
 * @property isInDialog true iff this state results in displaying a dialog.
 */
sealed class UiState(val isInDialog: Boolean) {
    /**
     * Execution context provided by the host activity.
     */
    interface UiStateContext {
        /** Invoke to login with the amazon OAuth provider. */
        fun loginWithAmazon()

        /** Invoke to pick an image to be used as a logo in the chat windows. */
        fun pickImage(onPickImage: (String?) -> Unit)
    }

    /**
     * Present any composable content, i.e., dialog, associated with this state.
     *
     * @param context Context with appropriate hooks back to host activity.
     */
    @Composable
    open fun Content(context: UiStateContext) {
    }

    /** Nothing has been done yet. */
    data object Initial : UiState(isInDialog = false) {
        @Composable
        override fun Content(context: UiStateContext) {
            BusySpinner(message = stringResource(string.loading))
        }
    }

    /** Requesting Configuration details from the user. */
    data object Configuration : UiState(isInDialog = true) {
        @Composable
        override fun Content(context: UiStateContext) {
            val viewModel: StoreViewModel = koinViewModel<StoreViewModel>()
            val settings = viewModel.chatSettingsRepository.settings.collectAsState()
            val configuration = remember { derivedStateOf { settings.value?.sdkConfiguration } }
            val configurations = viewModel.sdkConfigurationListRepository.configurationList.collectAsState()

            SdkConfigurationDialog(
                configuration = configuration.value,
                configurationDefinitions = configurations.value,
                onDismiss = { viewModel.setUiState(Prepared) },
                onConfigurationSelected = viewModel.chatSettingsHandler::setConfiguration
            )
        }
    }

    /** Preparing the chat object. */
    data object Preparing : UiState(isInDialog = true) {
        @Composable
        override fun Content(context: UiStateContext) {
            val viewModel: StoreViewModel = koinViewModel<StoreViewModel>()
            BusySpinner(
                message = stringResource(string.connecting),
                onCancel = viewModel.chatProvider::cancel
            )
        }
    }

    /** Performing OAuth authentication with the user. */
    data object OAuth : UiState(isInDialog = true) {
        @Composable
        override fun Content(context: UiStateContext) {
            context.loginWithAmazon()
        }
    }

    /** Performing simple authentication with the user. */
    data object Login : UiState(isInDialog = true) {
        @Composable
        override fun Content(
            context: UiStateContext,
        ) {
            val viewModel: StoreViewModel = koinViewModel<StoreViewModel>()
            val settings = viewModel.chatSettingsRepository.settings.collectAsState()
            val userName = remember { derivedStateOf { settings.value?.userName } }
            val customerId = remember { derivedStateOf { settings.value?.customerId } }

            LoginDialog(
                userName = userName.value,
                customerId = customerId.value,
                onAccept = viewModel::setLoginData,
            ) {
                viewModel.analyticsHandler.SendPageView("login", "/login")
            }
        }
    }

    /** Displaying the UI Settings dialog. */
    data object UiSettings : UiState(isInDialog = true) {
        @Composable
        override fun Content(context: UiStateContext) {
            val viewModel: StoreViewModel = koinViewModel<StoreViewModel>()
            UISettingsDialog(
                value = UISettings.collectAsState().value,
                onDismiss = { viewModel.setUiState(Prepared) },
                pickImage = context::pickImage,
                onReset = viewModel.uiSettingsRepository::clear,
                onConfirm = viewModel.uiSettingsRepository::save
            )
        }
    }

    /** Displaying an SdkVersionNotSupported error dialog. */
    data object SdkNotSupported : UiState(isInDialog = true) {
        @Composable
        override fun Content(context: UiStateContext) {
            val viewModel: StoreViewModel = koinViewModel<StoreViewModel>()
            val activity = LocalActivity.current
            ErrorDialog(
                title = stringResource(string.sdk_not_supported_title),
                message = stringResource(string.sdk_not_supported_message),
                onDismiss = { viewModel.setUiState(Prepared) },
                confirmButton = {
                    AppTheme.OutlinedButton(
                        text = stringResource(string.ok),
                        modifier = Modifier.testTag("sdk_not_supported_dialog_ok_button"),
                        onClick = { activity?.finishAffinity() },
                    )
                }
            )
        }
    }

    /** Connection is prepared and ready for analytics. */
    data object Prepared : UiState(isInDialog = false)
}
