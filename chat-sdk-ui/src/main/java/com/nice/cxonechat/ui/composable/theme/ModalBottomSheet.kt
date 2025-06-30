/*
 * Copyright (c) 2021-2025. NICE Ltd. All rights reserved.
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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement.SpaceBetween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue.Expanded
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nice.cxonechat.ui.R.string
import com.nice.cxonechat.ui.composable.theme.ChatTheme.space
import kotlinx.coroutines.launch
import androidx.compose.material3.ModalBottomSheet as Material3ModalBottomSheet

/**
 * A modal bottom sheet with a title, content, and two buttons at the bottom, one for cancel and one for submit.
 * Both buttons will hide the sheet when clicked, since the sheet is modal it should be removed from the hierarchy
 * if it is no longer needed, otherwise it will block any other non-modal UI from being interacted with.
 *
 * @param onDismiss callback when the sheet is dismissed.
 * @param onSubmit callback when the submit button is clicked.
 * @param canSubmit whether the submit button should be in a enabled state.
 * @param modifier modifier for the sheet.
 * @param title title of the sheet.
 * @param sheetGestureEnabled whether the sheet should be dismissible by dragging.
 * @param sheetState state of the sheet, which can be used to reset the sheet to the expanded state.
 * @param dragHandle drag handle for the sheet.
 * @param content content of the sheet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ChatTheme.ModalBottomSheet(
    onDismiss: () -> Unit,
    onSubmit: () -> Unit,
    canSubmit: Boolean,
    modifier: Modifier = Modifier,
    title: String? = null,
    sheetGestureEnabled: Boolean = false,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    dragHandle: @Composable () -> Unit = { BottomSheetDefaults.DragHandle(color = colorScheme.surfaceDim) },
    content: @Composable () -> Unit,
) {
    val scope = rememberCoroutineScope()
    fun hideSheet(onComplete: () -> Unit) {
        scope.launch { sheetState.hide() }.invokeOnCompletion { onComplete() }
    }
    Material3ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetGesturesEnabled = sheetGestureEnabled,
        containerColor = colorScheme.background,
        contentColor = colorScheme.onBackground,
        dragHandle = dragHandle,
        sheetState = sheetState,
        modifier = modifier,
    ) {
        Box(
            contentAlignment = Alignment.BottomCenter,
        ) {
            // By using the dynamic height of the footer, we can adjust the padding of the content to get the sticky footer.
            var footerHeight: Int by remember { mutableIntStateOf(0) }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = space.xl,
                        end = space.xl,
                        bottom = with(LocalDensity.current) { footerHeight.toDp() }
                    ),
            ) {
                title?.let {
                    Text(
                        it,
                        modifier = Modifier.padding(bottom = 41.dp),
                        style = chatTypography.surveyTitle
                    )
                }
                content()
            }

            val bottomRowBackground = colorScheme.onBackground.copy(alpha = 0.05f)
            NavigationBarOverlay(bottomRowBackground)
            Row(
                horizontalArrangement = SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(bottomRowBackground)
                    .onGloballyPositioned { layoutCoordinates ->
                        footerHeight = layoutCoordinates.size.height
                    }
            ) {
                TextButton(
                    modifier = Modifier.testTag("cancel_button"),
                    onClick = { hideSheet(onDismiss) }
                ) {
                    Text(stringResource(string.cancel))
                }
                TextButton(onClick = { hideSheet(onSubmit) }, enabled = canSubmit) {
                    Text(stringResource(string.done))
                }
            }
        }
    }
}

/**
 *  This spacer is drawn over the navigation bar to paint it with the same color as the bottom row.
 *  This fugly hack is needed since bottomsheet supports only one color for the content and it will
 *  work only with semitransparent color.
 *  @param bottomRowBackground color of the space.
 */
@Composable
private fun ChatTheme.NavigationBarOverlay(bottomRowBackground: Color) {
    Spacer(
        modifier = Modifier
            .fillMaxWidth()
            .height(space.xl)
            .offset(y = space.xl)
            .background(bottomRowBackground)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showSystemUi = true, showBackground = true)
@Composable
private fun ModalBottomSheetPreview() {
    val previewSheetState = remember {
        SheetState(
            skipPartiallyExpanded = true,
            initialValue = Expanded,
            positionalThreshold = { 0f },
            velocityThreshold = { 0f },
        )
    }
    var canSubmit by remember { mutableStateOf(false) }
    val previewContent: @Composable () -> Unit = {
        LazyColumn(
            contentPadding = PaddingValues(vertical = space.medium),
        ) {
            stickyHeader {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = SpaceBetween
                ) {
                    Text("Enable submit button")
                    Switch(canSubmit, onCheckedChange = { canSubmit = it })
                }
            }
            items(25) {
                ChatTheme.TextField(
                    label = "Label $it",
                    value = rememberTextFieldState("$it"),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
    ChatTheme {
        val scope = rememberCoroutineScope()
        Column {
            Button(onClick = {
                scope.launch {
                    previewSheetState.show()
                }
            }) {
                Text("Show Modal Bottom Sheet")
            }
            Text(previewSheetState.currentValue.toString())
            if (previewSheetState.isVisible) {
                ChatTheme.ModalBottomSheet(
                    onDismiss = {},
                    onSubmit = {},
                    title = "Title",
                    sheetState = previewSheetState,
                    content = previewContent,
                    canSubmit = canSubmit
                )
            }
        }
    }
}
