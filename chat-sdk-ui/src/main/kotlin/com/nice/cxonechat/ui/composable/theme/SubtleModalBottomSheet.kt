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

package com.nice.cxonechat.ui.composable.theme

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import com.nice.cxonechat.ui.composable.theme.ChatTheme.space

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ChatTheme.SubtleModalBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    dragHandler: @Composable (() -> Unit)? = null,
    // No half height stop: these sheets ask for a choice, not for scrolling.
    sheetState: SheetState = rememberBottomSheetState(SheetValue.Hidden, SheetAnchors.FullHeightOnly),
    // Default keeps a centred card in landscape; a caller (e.g. the Satisfaction Survey) can opt
    // back into the wide-screen rule by passing its own sheetMaxWidth.
    sheetMaxWidth: Dp = BottomSheetDefaults.SheetMaxWidth,
    containerColor: Color = chatColors.token.background.surface.subtle,
    sheetGesturesEnabled: Boolean = true,
    bottomSheetTag: String = "inactivity_popup_bottom_sheet",
    content: @Composable ColumnScope.() -> Unit,
) {
    WideScreenModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        sheetMaxWidth = sheetMaxWidth,
        dragHandle = dragHandler,
        containerColor = containerColor,
        contentColor = chatColors.token.content.primary,
        sheetGesturesEnabled = sheetGesturesEnabled,
        modifier = modifier
            .testTag(bottomSheetTag),
        content = content,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun PreviewSubtleModalBottomSheet() {
    ChatTheme {
        ChatTheme.SubtleModalBottomSheet(
            onDismissRequest = {},
        ) {
            Text(
                "This is a preview of the SubtleModalBottomSheet",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(space.large)
            )
        }
    }
}
