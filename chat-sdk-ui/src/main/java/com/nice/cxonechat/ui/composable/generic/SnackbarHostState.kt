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

package com.nice.cxonechat.ui.composable.generic

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarDuration.Indefinite
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SnackbarResult.ActionPerformed
import androidx.compose.material3.SnackbarResult.Dismissed

internal suspend fun SnackbarHostState.showActionSnackbar(
    message: String,
    actionLabel: String,
    duration: SnackbarDuration = Indefinite,
    onAction: () -> Unit,
    onDismiss: () -> Unit = {},
) {
    showSnackbar(
        message = message,
        actionLabel = actionLabel,
        duration = duration,
    ).also { result: SnackbarResult ->
        when (result) {
            ActionPerformed -> onAction()
            Dismissed -> onDismiss()
        }
    }
}
