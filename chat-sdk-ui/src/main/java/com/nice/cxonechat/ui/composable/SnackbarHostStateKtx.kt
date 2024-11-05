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

package com.nice.cxonechat.ui.composable

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarDuration.Indefinite
import androidx.compose.material3.SnackbarHostState
import com.nice.cxonechat.ui.composable.generic.showActionSnackbar

/**
 * Shows a snackbar with a message and an action that can be cancelled.
 *
 * @param message the message to show
 * @param actionLabel the label of the action button
 * @param onAction the action to perform when the action button is clicked
 * @param duration the duration of the snackbar
 */
internal suspend fun SnackbarHostState.showCancellableSnackbar(
    message: String,
    actionLabel: String,
    onAction: () -> Unit,
    duration: SnackbarDuration = Indefinite,
) {
    showActionSnackbar(message, actionLabel, duration, onAction)
}
