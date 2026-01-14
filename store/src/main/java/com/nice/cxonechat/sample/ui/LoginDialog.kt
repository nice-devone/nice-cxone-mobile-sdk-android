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

package com.nice.cxonechat.sample.ui

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nice.cxonechat.UserName
import com.nice.cxonechat.log.Logger
import com.nice.cxonechat.log.LoggerScope
import com.nice.cxonechat.log.debug
import com.nice.cxonechat.log.scope
import com.nice.cxonechat.sample.R.string
import com.nice.cxonechat.sample.data.models.ChatUserName
import com.nice.cxonechat.sample.data.models.LoginData
import com.nice.cxonechat.sample.ui.theme.AppTheme
import com.nice.cxonechat.sample.ui.theme.AppTheme.space
import com.nice.cxonechat.sample.ui.theme.TextField
import com.nice.cxonechat.sample.utilities.Requirements.required
import kotlinx.coroutines.launch
import org.koin.java.KoinJavaComponent.inject

/**
 * Display the Login dialog to collect the users first and last name.
 *
 * @param userName Current [UserName] if any.
 * @param customerId Current customerId if any.
 * @param onAccept Invoked when the user accepts to dismiss the dialog and continue.
 * @param analytics Additional [Composable] content which should be included in the dialog for purpose of tracking
 * analytics data.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginDialog(
    userName: UserName?,
    customerId: String?,
    onAccept: (LoginData) -> Unit,
    @SuppressLint("ComposableLambdaParameterNaming") // This is not intended for actual content.
    analytics: (@Composable () -> Unit)? = null,
) {
    val sheetState = rememberModalBottomSheetState()
    val coroutineScope = rememberCoroutineScope()
    var firstName by rememberSaveable { mutableStateOf(userName?.firstName ?: "") }
    var lastName by rememberSaveable { mutableStateOf(userName?.lastName ?: "") }
    var suppliedCustomerId by rememberSaveable { mutableStateOf(customerId) }
    val login = {
        val newUserName = ChatUserName(lastName = lastName, firstName = firstName)
        if (newUserName.valid) {
            onAccept(LoginData(newUserName, suppliedCustomerId))
        }
    }
    ModalBottomSheet(
        onDismissRequest = {
            coroutineScope.launch { sheetState.hide() }
        },
        sheetState = sheetState
    ) {
        analytics?.invoke()
        LoginDialogContent(
            firstName = firstName,
            onFirstNameChange = { firstName = it },
            lastName = lastName,
            onLastNameChange = { lastName = it },
            suppliedCustomerId = suppliedCustomerId,
            onCustomerIdChange = { suppliedCustomerId = it.ifBlank { null } },
            login = login
        )
    }
}

@Composable
private fun LoginDialogContent(
    firstName: String,
    onFirstNameChange: (String) -> Unit,
    lastName: String,
    onLastNameChange: (String) -> Unit,
    suppliedCustomerId: String?,
    onCustomerIdChange: (String) -> Unit,
    login: () -> Unit
) {
    val focusManager = LocalFocusManager.current
    val loginTextFieldModifier = Modifier
        .fillMaxWidth()
        .padding(top = space.small)
    Column(
        modifier = Modifier
            .wrapContentHeight()
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = space.medium)
            .testTag("login_bottomsheet")
    ) {
        Text(
            text = stringResource(string.login),
            style = AppTheme.typography.titleLarge.copy(
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium
            )
        )
        AppTheme.TextField(
            modifier = loginTextFieldModifier.padding(top = space.medium),
            label = stringResource(string.first_name),
            value = firstName,
            requirement = required,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            onValueChanged = onFirstNameChange
        )
        AppTheme.TextField(
            modifier = loginTextFieldModifier,
            label = stringResource(string.last_name),
            value = lastName,
            requirement = required,
            onValueChanged = onLastNameChange
        )
        AppTheme.TextField(
            modifier = loginTextFieldModifier,
            label = stringResource(string.customer_id),
            value = suppliedCustomerId.orEmpty(),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(
                onDone = {
                    focusManager.clearFocus()
                    login()
                }
            ),
            onValueChanged = onCustomerIdChange
        )
        LoginConfirmButton(
            login = login,
            enableButton = firstName.isNotBlank() && lastName.isNotBlank()
        )
    }
}

@Composable
private fun ColumnScope.LoginConfirmButton(
    login: () -> Unit,
    enableButton: Boolean,
) {
    TextButton(
        onClick = login,
        modifier = Modifier
            .align(Alignment.End)
            .padding(top = space.small)
            .testTag("login_bottomsheet_ok_button"),
        enabled = enableButton
    ) {
        Text(text = stringResource(string.ok))
    }
}

@Preview
@Composable
private fun LoginDialogPreview() {
    val logger: Logger by inject(Logger::class.java)
    val loggerScope = LoggerScope("LoginDialogPreview", logger)

    LoginDialog(
        userName = null,
        customerId = null,
        onAccept = { userName ->
            loggerScope.scope("onAccept") {
                debug("finish: $userName")
            }
        }
    )
}
