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

package com.nice.cxonechat.sample.ui

import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.absolutePadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import com.nice.cxonechat.UserName
import com.nice.cxonechat.sample.R.string
import com.nice.cxonechat.sample.data.models.ChatUserName
import com.nice.cxonechat.sample.ui.theme.AppTheme
import com.nice.cxonechat.sample.ui.theme.AppTheme.space
import com.nice.cxonechat.sample.ui.theme.Dialog
import com.nice.cxonechat.sample.ui.theme.OutlinedButton
import com.nice.cxonechat.sample.ui.theme.TextField
import com.nice.cxonechat.sample.utilities.Requirements.required

/**
 * Display the Login dialog to collect the users first and last name.
 *
 * @param userName Current [UserName] if any.
 * @param onAccept Invoked when the user accepts to dismiss the dialog and continue.
 */
@Composable
fun LoginDialog(userName: UserName?, onAccept: (UserName) -> Unit) {
    var firstName by remember { mutableStateOf(userName?.firstName ?: "") }
    var lastName by remember { mutableStateOf(userName?.lastName ?: "") }

    AppTheme.Dialog(
        modifier = Modifier.wrapContentHeight(),
        title = stringResource(string.login),
        onDismiss = { },
        confirmButton = {
            AppTheme.OutlinedButton(
                stringResource(string.ok),
                enabled = firstName.isNotBlank() && lastName.isNotBlank(),
                onClick = { onAccept(ChatUserName(lastName = lastName, firstName = firstName)) },
            )
        }
    ) {
        Column(modifier = Modifier.absolutePadding(top = space.medium)) {
            val focusManager = LocalFocusManager.current

            AppTheme.TextField(
                label = stringResource(string.first_name),
                value = firstName,
                requirement = required,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            ) {
                firstName = it
            }
            AppTheme.TextField(
                label = stringResource(string.last_name),
                value = lastName,
                requirement = required,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = {
                        focusManager.clearFocus()
                        val newUserName = ChatUserName(lastName = lastName, firstName = firstName)
                        if(newUserName.valid) {
                            onAccept(newUserName)
                        }
                    }
                )
            ) {
                lastName = it
            }
        }
    }
}

@Preview
@Composable
private fun LoginDialogPreview() {
    LoginDialog(userName = null) { userName ->
        Log.d("LoginDialog", "finish: $userName")
    }
}
