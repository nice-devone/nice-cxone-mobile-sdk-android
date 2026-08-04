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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.nice.cxonechat.sample.R.string
import com.nice.cxonechat.sample.ui.theme.AppTheme
import com.nice.cxonechat.sample.ui.theme.AppTheme.space
import com.nice.cxonechat.sample.ui.theme.MultiToggleButton
import com.nice.cxonechat.sample.ui.theme.OutlinedButton

/**
 * Bottom sheet that lets the user choose between Implicit and Explicit Google Sign-In flows,
 * then confirms with a Login button.
 *
 * @param onDismiss Called when the sheet is dismissed without a selection.
 * @param onSelectMethod Called with the chosen [OAuthSignInMethod] when the Login button is tapped.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun GoogleSignInBottomSheet(
    onDismiss: () -> Unit,
    onSelectMethod: (OAuthSignInMethod) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { it != SheetValue.Hidden },
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        properties = ModalBottomSheetProperties(shouldDismissOnBackPress = false),
    ) {
        GoogleSignInContent(onSelectMethod = onSelectMethod)
    }
}

/**
 * Content of the Google Sign-In bottom sheet: title, subtitle, flow selector, and login button.
 *
 * @param onSelectMethod Called with the chosen [OAuthSignInMethod] when the Login button is tapped.
 */
@Composable
internal fun GoogleSignInContent(
    onSelectMethod: (OAuthSignInMethod) -> Unit,
) {
    var selectedMethod by rememberSaveable { mutableStateOf(OAuthSignInMethod.Implicit) }

    val implicitLabel = stringResource(string.oauth_method_implicit)
    val explicitLabel = stringResource(string.oauth_method_explicit)
    val methodLabel = remember(implicitLabel, explicitLabel) {
        mapOf(
            OAuthSignInMethod.Implicit to implicitLabel,
            OAuthSignInMethod.Explicit to explicitLabel,
        )
    }

    Column(
        modifier = Modifier
            .wrapContentHeight()
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = space.screenHorizontal, vertical = space.medium)
            .testTag("google_sign_in_bottom_sheet"),
    ) {
        Text(
            text = stringResource(string.sign_in_with_google),
            style = AppTheme.typography.titleLarge,
        )
        Text(
            text = stringResource(string.sign_in_with_google_subtitle),
            style = AppTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = space.small),
        )
        AppTheme.MultiToggleButton(
            currentSelection = selectedMethod,
            toggleStates = OAuthSignInMethod.entries,
            labelFor = { methodLabel.getValue(it) },
            modifier = Modifier
                .wrapContentWidth()
                .align(Alignment.CenterHorizontally)
                .padding(top = space.large)
                .testTag("google_sign_in_toggle_button"),
            onToggleChange = { selectedMethod = it },
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = space.large),
            horizontalArrangement = Arrangement.Center,
        ) {
            AppTheme.OutlinedButton(
                text = stringResource(string.login),
                modifier = Modifier.testTag("google_sign_in_login_button"),
                isDefault = true,
                onClick = { onSelectMethod(selectedMethod) },
            )
        }
    }
}

@Preview
@Composable
private fun GoogleSignInContentPreview() {
    AppTheme {
        GoogleSignInContent(onSelectMethod = {})
    }
}
