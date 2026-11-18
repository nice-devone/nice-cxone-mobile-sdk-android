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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Dp
import com.nice.cxonechat.ui.composable.theme.ChatTheme.space

/**
 * The modal bottom sheet the SDK's sheets are built from: Material 3's, laid out for the window it is
 * drawn into and themed with the chat's own colours.
 *
 * Every sheet in the module goes through here, so the wide-screen width rule is decided once. A sheet
 * that reached for [ModalBottomSheet] directly would silently keep Material 3's phone-sized cap.
 *
 * The width rule is [wideScreenSheetMaxWidth], which a sheet whose design draws it as a centred card in
 * landscape overrides through [sheetMaxWidth]. Content is not inset here, because a sheet decides that
 * per element rather than as a block: a divider or a scrollbar strip spans the sheet while the controls
 * beside it sit in the [wideScreenHorizontalPadding] column.
 *
 * Parameters mirror [ModalBottomSheet]'s and default to it, except for the width, for the colours,
 * which default to the chat theme, and for [sheetState], which drops Material 3's half-height stop.
 * Only the ones some sheet needs are exposed — add rather than pass through the rest for its own sake.
 *
 * @param onDismissRequest Called when the sheet is dismissed by gesture, back press or scrim tap.
 * @param modifier Modifier for the sheet.
 * @param sheetState State of the sheet. Defaults to a hidden sheet with no half-height stop.
 * @param sheetMaxWidth Width the sheet may grow to. Defaults to the module's wide-screen width rule.
 * @param sheetGesturesEnabled Whether the sheet can be dragged.
 * @param properties Platform-specific properties, including whether a tap outside or a back press
 * dismisses the sheet.
 * @param containerColor Background of the sheet.
 * @param contentColor Preferred colour for content inside the sheet.
 * @param dragHandle Marker at the top of the sheet, or `null` for a sheet whose design has none.
 * @param contentWindowInsets Window insets applied to the content.
 * @param content Content of the sheet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ChatTheme.WideScreenModalBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    // No half height stop by default: a sheet that asks for a choice has nothing to scroll to.
    sheetState: SheetState = rememberBottomSheetState(SheetValue.Hidden, SheetAnchors.FullHeightOnly),
    sheetMaxWidth: Dp = wideScreenSheetMaxWidth(),
    sheetGesturesEnabled: Boolean = true,
    properties: ModalBottomSheetProperties = ModalBottomSheetProperties(),
    containerColor: Color = chatColors.token.background.surface.subtle,
    contentColor: Color = chatColors.token.content.primary,
    dragHandle: @Composable (() -> Unit)? = { BottomSheetDefaults.DragHandle() },
    contentWindowInsets: @Composable () -> WindowInsets = { BottomSheetDefaults.modalWindowInsets },
    content: @Composable ColumnScope.() -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        sheetState = sheetState,
        sheetMaxWidth = sheetMaxWidth,
        sheetGesturesEnabled = sheetGesturesEnabled,
        properties = properties,
        containerColor = containerColor,
        contentColor = contentColor,
        dragHandle = dragHandle,
        contentWindowInsets = contentWindowInsets,
        content = content,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@PreviewLightDark
@Composable
private fun PreviewWideScreenSheet() {
    ChatTheme {
        ChatTheme.WideScreenModalBottomSheet(onDismissRequest = {}) {
            Text(
                text = "A sheet as wide as the window it is drawn into.",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = wideScreenHorizontalPadding())
                    .padding(space.large),
            )
        }
    }
}
