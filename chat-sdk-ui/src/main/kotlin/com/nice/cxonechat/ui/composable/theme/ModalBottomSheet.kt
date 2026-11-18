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

import android.view.ViewTreeObserver
import android.view.Window
import android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
import android.view.WindowManager.LayoutParams.SOFT_INPUT_MASK_ADJUST
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement.SpaceBetween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults.buttonColors
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.SheetValue.Expanded
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nice.cxonechat.ui.R.string
import com.nice.cxonechat.ui.composable.theme.ChatTheme.space
import kotlinx.coroutines.launch

/**
 * A modal bottom sheet with a title, content, and two buttons at the bottom, one for cancel and one for submit.
 * Both buttons will hide the sheet when clicked, since the sheet is modal it should be removed from the hierarchy
 * if it is no longer needed, otherwise it will block any other non-modal UI from being interacted with.
 *
 * The title and drag handle sit in their own non-scrolling header, separated from the body by a divider.
 *
 * @param onDismiss callback when the sheet is dismissed.
 * @param onSubmit callback when the submit button is clicked.
 * @param canSubmit whether the submit button should be in a enabled state.
 * @param modifier modifier for the sheet.
 * @param title Optional title of the sheet.
 * @param sheetGestureEnabled whether the sheet should be dismissible by dragging.
 * @param dismissOnClickOutside whether tapping the scrim outside the sheet dismisses it. Defaults to
 * `true`; a sheet whose own design allows closing only through [onSubmit]/[onDismiss] — not a drag, a
 * scrim tap, or, on a wide screen, the visible strip either side of a width-capped sheet — passes
 * `false`. A back press still dismisses the sheet either way.
 * @param sheetState state of the sheet, which can be used to reset the sheet to the expanded state.
 * @param content content of the sheet, given the side padding it applies as its scrolling list's
 * `contentPadding`; see [sheetBodyContentPadding]. On a compact screen it is the body's only side margin,
 * so content that ignores it runs to the sheet's edges.
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
    dismissOnClickOutside: Boolean = true,
    sheetState: SheetState = rememberBottomSheetState(SheetValue.Hidden, SheetAnchors.FullHeightOnly),
    content: @Composable (contentPadding: PaddingValues) -> Unit,
) {
    val scope = rememberCoroutineScope()
    fun hideSheet(onComplete: () -> Unit) {
        scope.launch { sheetState.hide() }.invokeOnCompletion { onComplete() }
    }
    WideScreenModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetGesturesEnabled = sheetGestureEnabled,
        properties = ModalBottomSheetProperties(shouldDismissOnClickOutside = dismissOnClickOutside),
        // Only the grab affordance goes here: Material 3 makes this whole slot one clickable region.
        dragHandle = { ModalBottomSheetDragHandle() },
        sheetState = sheetState,
        // Drops the default top inset, which would double up with the manual one below. Vertical-only
        // so the horizontal nav-bar inset is owned solely by the modifier below and isn't applied twice.
        // The keyboard inset keeps the footer and the focused field above the keyboard. Union, not add: both insets
        // are measured from the window bottom, so the keyboard's already covers the nav bar.
        contentWindowInsets = {
            WindowInsets.navigationBars.union(WindowInsets.ime).only(WindowInsetsSides.Vertical)
        },
        // Nothing applies system bar insets to the sheet's own window, so status bar and side nav bar insets
        // (e.g. rotated 3-button nav in landscape) are applied explicitly; zero where none exist.
        // On a wide screen the sheet spans the full window height instead of reserving space for the
        // status bar - HideDialogStatusBarWideScreen below hides it on this window - while a
        // compact screen keeps it visible and padded. Deliberately isWideScreen() here, not the
        // isLandscapeWindow() that wideScreenSheetMaxWidth() gates the sheet's width on: the 640dp
        // cap should survive on a portrait tablet, but the status bar is hidden - and its inset
        // dropped - on every wide screen.
        modifier = modifier
            .padding(
                top = if (isWideScreen()) 0.dp else WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
            )
            .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Horizontal)),
    ) {
        // The sheet opens its own window (Material 3's ModalBottomSheetDialogWrapper), which doesn't
        // inherit the host activity's hidden status bar; without this it stays visible over the sheet.
        HideDialogStatusBarWideScreen()
        ResizeSheetWindowForKeyboard()
        Box(
            contentAlignment = Alignment.BottomCenter,
        ) {
            // By using the dynamic height of the footer, we can adjust the padding of the content to get the sticky footer.
            var footerHeight: Int by remember { mutableIntStateOf(0) }
            // Same technique for the title height, reserved as the body's top padding.
            var titleHeight: Int by remember { mutableIntStateOf(0) }

            // Body is extracted to a helper to reduce function size
            ModalBottomSheetBody(
                footerHeight = footerHeight,
                titleHeight = titleHeight,
                content = content,
            )

            // Non-scrolling title, laid out as a sibling rather than a weighted child.
            if (title != null) {
                ModalBottomSheetTitle(
                    title = title,
                    modifier = Modifier.align(Alignment.TopCenter),
                    heightProvider = { h -> titleHeight = h },
                )
            }

            val bottomRowBackground = colorScheme.background
            NavigationBarOverlay(bottomRowBackground)

            // Footer extracted to helper
            ModalBottomSheetFooter(
                bottomRowBackground = bottomRowBackground,
                footerHeightProvider = { h -> footerHeight = h },
                onCancel = { hideSheet(onDismiss) },
                onSubmit = { hideSheet(onSubmit) },
                canSubmit = canSubmit,
            )
        }
    }
}

/**
 * Makes the sheet's own window resize for the keyboard.
 *
 * Material 3's sheet sets its window to `SOFT_INPUT_ADJUST_NOTHING` on API 30 and newer, which leaves the
 * keyboard out of the window's visible frame. An exposed dropdown measures its room with that frame, so its
 * menu opens below the field, under the keyboard. Material 3 lays the sheet window out behind the system bars
 * (`decorFitsSystemWindows = false`), so resizing changes only that frame, not the window's size.
 *
 * Material 3 sets its mode again whenever its sheet dialog recomposes, so this applies ours before every draw.
 */
@Composable
private fun ResizeSheetWindowForKeyboard() {
    val window = LocalView.current.findDialogWindow() ?: return
    DisposableEffect(window) {
        val decorView = window.decorView
        val listener = ViewTreeObserver.OnPreDrawListener {
            window.resizeForKeyboard()
            true
        }
        window.resizeForKeyboard()
        decorView.viewTreeObserver.addOnPreDrawListener(listener)
        onDispose { decorView.viewTreeObserver.removeOnPreDrawListener(listener) }
    }
}

// Any mode but ADJUST_NOTHING puts the keyboard into the visible frame; RESIZE is the one that doesn't pan.
@Suppress("DEPRECATION")
private fun Window.resizeForKeyboard() {
    val mode = attributes.softInputMode
    if (mode and SOFT_INPUT_MASK_ADJUST != SOFT_INPUT_ADJUST_RESIZE) {
        setSoftInputMode(mode and SOFT_INPUT_MASK_ADJUST.inv() or SOFT_INPUT_ADJUST_RESIZE)
    }
}

/** The 32x4dp grab affordance, on [ThemeColorTokens.Background.default] to match the title below it. */
@Composable
private fun ChatTheme.ModalBottomSheetDragHandle() {
    val dragHandleDescription = stringResource(string.content_description_drag_handle)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(chatColors.token.background.default),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier
                .padding(vertical = space.large)
                .semantics {
                    contentDescription = dragHandleDescription
                },
            color = chatColors.token.content.tertiary,
            shape = MaterialTheme.shapes.extraLarge,
        ) {
            Box(Modifier.size(width = 32.dp, height = 4.dp))
        }
    }
}

/** The sheet's title, on [ThemeColorTokens.Background.default], separated from the body by a divider. */
@Composable
private fun ChatTheme.ModalBottomSheetTitle(
    title: String,
    modifier: Modifier = Modifier,
    heightProvider: (Int) -> Unit,
) {
    val horizontalPadding = wideScreenHorizontalPadding().coerceAtLeast(space.large)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(chatColors.token.background.default)
            .onGloballyPositioned { layoutCoordinates ->
                heightProvider(layoutCoordinates.size.height)
            }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = horizontalPadding)
                .padding(top = space.medium, bottom = space.large)
                // So TalkBack still announces the title as a heading, as the list picker's own header
                // did before it started sharing this title with the pre-chat survey and inactivity sheets.
                .semantics(mergeDescendants = true) { heading() },
            contentAlignment = Alignment.CenterStart,
        ) {
            Text(text = title, style = chatTypography.surveyTitle)
        }
        HorizontalDivider(color = chatColors.token.border.default)
    }
}

// New helper composable for the body/content area
@Composable
private fun ChatTheme.ModalBottomSheetBody(
    footerHeight: Int,
    titleHeight: Int,
    content: @Composable (contentPadding: PaddingValues) -> Unit,
) {
    val horizontalPadding = wideScreenHorizontalPadding()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                top = with(LocalDensity.current) { titleHeight.toDp() } + space.xl,
                start = horizontalPadding,
                end = horizontalPadding,
                bottom = with(LocalDensity.current) { footerHeight.toDp() }
            )
    ) {
        content(sheetBodyContentPadding(horizontalPadding))
    }
}

/**
 * Side padding the body's scrolling list applies itself, which puts its scrollbar where the design does.
 *
 * - **Compact** ([wideScreenPadding] is `0.dp`): the list spans the sheet and insets its content by the
 *   design's 16dp on both sides, so the scrollbar draws inside that margin rather than over the content.
 * - **Wide**: the body is already inset by [wideScreenPadding], and the design draws the scrollbar inside
 *   that column, so the content keeps only the strip at its end clear for it.
 */
@Composable
private fun sheetBodyContentPadding(wideScreenPadding: Dp): PaddingValues =
    if (wideScreenPadding == 0.dp) {
        PaddingValues(horizontal = space.large)
    } else {
        PaddingValues(end = space.scrollbarThickness + space.scrollbarInset + space.small)
    }

// New helper composable for the footer row
@Composable
private fun ChatTheme.ModalBottomSheetFooter(
    bottomRowBackground: Color,
    footerHeightProvider: (Int) -> Unit,
    onCancel: () -> Unit,
    onSubmit: () -> Unit,
    canSubmit: Boolean,
) {
    val horizontalPadding = wideScreenHorizontalPadding()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(bottomRowBackground)
            .onGloballyPositioned { layoutCoordinates ->
                footerHeightProvider(layoutCoordinates.size.height)
            }
    ) {
        // Separates the footer from the body, matching the divider below the title.
        HorizontalDivider(color = chatColors.token.border.default)
        Row(
            horizontalArrangement = SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 3.dp, start = 2.dp + horizontalPadding, end = 2.dp + horizontalPadding)
        ) {
            TextButton(
                modifier = Modifier.testTag("cancel_button"),
                onClick = onCancel
            ) {
                Text(
                    modifier = Modifier,
                    text = stringResource(string.cancel),
                    style = chatTypography.bottomSheetFooterButtonText
                )
            }
            TextButton(
                modifier = Modifier.testTag("submit_button"),
                onClick = onSubmit,
                colors = buttonColors(
                    containerColor = chatColors.token.background.default,
                    disabledContainerColor = chatColors.token.background.default,
                    contentColor = chatColors.token.brand.primary,
                    disabledContentColor = chatColors.token.content.tertiary
                ),
                enabled = canSubmit
            ) {
                Text(
                    text = stringResource(string.submit),
                    style = chatTypography.bottomSheetFooterButtonText
                )
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
            enabledValues = SheetAnchors.FullHeightOnly,
            initialValue = Expanded,
            positionalThreshold = { 0f },
            velocityThreshold = { 0f },
        )
    }
    var canSubmit by remember { mutableStateOf(false) }
    val previewContent: @Composable (PaddingValues) -> Unit = { contentPadding ->
        LazyColumn(
            modifier = Modifier.padding(contentPadding),
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
