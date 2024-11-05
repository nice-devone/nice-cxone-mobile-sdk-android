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

package com.nice.cxonechat.ui.composable.conversation

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.nice.cxonechat.ui.R
import com.nice.cxonechat.ui.composable.theme.Alert
import com.nice.cxonechat.ui.composable.theme.ChatTheme

@Composable
internal fun ErrorDialog(
    title: String,
    message: String,
    onDismiss: () -> Unit
) {
    ChatTheme.Alert(
        title = title,
        message = message,
        dismissLabel = stringResource(id = R.string.ok),
        onDismiss = onDismiss
    )
}
