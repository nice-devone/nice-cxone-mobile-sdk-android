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

package com.nice.cxonechat.ui.composable.generic

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.tooling.preview.Preview
import com.nice.cxonechat.ui.R.string
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import com.nice.cxonechat.ui.composable.theme.ChatTheme.chatShapes
import com.nice.cxonechat.ui.composable.theme.ChatTheme.space

/**
 * Creates a centered loading spinner that can be overlaid on previews.
 *
 * @param modifier Modifier for the spinner container.
 * @param isGroupAttachment Whether the attachment belongs to a group or not, used for styling decisions.
 * @param showLoadingBorder Whether to show a border around the spinner, typically used for image attachments.
 * @param fillMaxSize Whether the spinner should fill the maximum available size.
 */
@Composable
internal fun LoadingSpinner(
    modifier: Modifier = Modifier,
    isGroupAttachment: Boolean = true,
    showLoadingBorder: Boolean = true,
    fillMaxSize: Boolean = true,
) {
    val loading = stringResource(string.loading)
    val shape = if (isGroupAttachment) chatShapes.selectionFrame else chatShapes.bubbleSoloShape
    Box(
        modifier = modifier
            .then(if (fillMaxSize) Modifier.fillMaxSize() else Modifier)
            .background(
                color = ChatTheme.chatColors.token.background.default,
                shape = shape
            )
            .let { baseModifier ->
                if (showLoadingBorder) {
                    baseModifier.border(
                        width = space.framePreviewWidth,
                        color = ChatTheme.chatColors.token.border.default,
                        shape = shape
                    )
                } else {
                    baseModifier
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(
            modifier = Modifier
                .semantics {
                    testTag = "loading"
                    this.contentDescription = loading
                }
        )
    }
}

@Preview
@Composable
private fun LoadingSpinnerPreview() {
    ChatTheme {
        LoadingSpinner()
    }
}
