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

package com.nice.cxonechat.ui.composable.conversation.plugin

import android.content.ActivityNotFoundException
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import com.nice.cxonechat.ui.composable.conversation.PreviewMessageItemBase
import com.nice.cxonechat.ui.composable.conversation.model.PluginElement.Button
import com.nice.cxonechat.ui.composable.generic.SimpleAlertDialog
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import com.nice.cxonechat.ui.composable.theme.OutlinedButton

@Composable
internal fun ButtonElementView(
    button: Button,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var error: String? by remember { mutableStateOf(null) }

    Row {
        ChatTheme.OutlinedButton(
            text = button.text,
            modifier = modifier,
        ) {
            try {
                button.action?.invoke(context)
            } catch(expect: ActivityNotFoundException) {
                error = expect.message
            }
        }

        error?.let { message ->
            SimpleAlertDialog(message = message) {
                error = null
            }
        }
    }
}

@Composable
@Preview
private fun PreviewButton() {
    PreviewMessageItemBase(
        message = MessagePreviewProvider().button.asMessage("button"),
        showSender = true,
    )
}
