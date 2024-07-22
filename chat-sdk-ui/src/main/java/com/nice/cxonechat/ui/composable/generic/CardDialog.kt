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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Card
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.LoremIpsum
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.nice.cxonechat.ui.composable.theme.ChatTheme

/**
 * A [Dialog] with [Card] as base and predefined optional [Text] field for a title
 * of the card dialog.
 *
 * @param title Optional text which will be displayed as title of the card.
 * @param modifier [Modifier] passed to the [Card] in the [Dialog].
 * @param properties [DialogProperties] for the [Dialog]. Default is that the dialog is dismissible by clicking outside
 * and by the back-press.
 * @param onDismiss Action which will be passed to [Dialog] as `onDismissRequest` parameter.
 * @param content Content of the [Card] in the [Dialog].
 */
@Composable
internal fun CardDialog(
    title: String?,
    modifier: Modifier = Modifier,
    properties: DialogProperties = DialogProperties(
        dismissOnBackPress = true,
        dismissOnClickOutside = true,
    ),
    onDismiss: () -> Unit,
    content: @Composable () -> Unit,
) {
    Dialog(
        properties = properties,
        onDismissRequest = onDismiss
    ) {
        Card(modifier = modifier) {
            Column {
                if (title != null) {
                    CardTitle(title, modifier = Modifier.align(Alignment.CenterHorizontally))
                }
                content()
            }
        }
    }
}

@Preview
@Composable
private fun Preview(
    @PreviewParameter(LoremIpsum::class) content: String,
) {
    ChatTheme {
        Surface(modifier = Modifier.fillMaxSize(0.5f)) {
            CardDialog(title = "Card with title", onDismiss = {}) {
                Text(text = content, maxLines = 5)
            }
        }
    }
}

@Preview
@Composable
private fun PreviewWithoutTitle(
    @PreviewParameter(LoremIpsum::class) content: String,
) {
    ChatTheme {
        Surface(modifier = Modifier.fillMaxSize(0.5f)) {
            CardDialog(title = null, onDismiss = {}) {
                Text(text = content, maxLines = 5)
            }
        }
    }
}
