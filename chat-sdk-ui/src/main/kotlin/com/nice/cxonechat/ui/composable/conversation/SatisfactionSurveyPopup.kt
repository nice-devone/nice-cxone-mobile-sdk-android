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

package com.nice.cxonechat.ui.composable.conversation

import android.content.res.Configuration
import android.view.View
import android.view.Window
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.window.DialogWindowProvider
import com.nice.cxonechat.Popup
import com.nice.cxonechat.ui.R.string
import com.nice.cxonechat.ui.composable.generic.BottomSheetTitle
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import com.nice.cxonechat.ui.composable.theme.ChatTheme.chatColors
import com.nice.cxonechat.ui.composable.theme.ChatTheme.chatTypography
import com.nice.cxonechat.ui.composable.theme.ChatTheme.colorScheme
import com.nice.cxonechat.ui.composable.theme.ChatTheme.space
import com.nice.cxonechat.ui.composable.theme.HideStatusBar
import com.nice.cxonechat.ui.composable.theme.SubtleModalBottomSheet

/**
 * Native shell for the Satisfaction Survey popup: dims the chat, embeds the survey as a WebView,
 * and owns the only dismiss control. No swipe-to-dismiss - [onDismiss] is reached only through
 * the explicit action row below.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SatisfactionSurveyPopup(popup: Popup.SatisfactionSurvey, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true) {
        it === SheetValue.Expanded
    }
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    ChatTheme.SubtleModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        // ModalBottomSheet always caps width at 640dp internally; Dp.Unspecified is its own
        // documented way to go full width, needed only in landscape where screens exceed that.
        sheetMaxWidth = if (isLandscape) Dp.Unspecified else BottomSheetDefaults.SheetMaxWidth,
        // One flat background spans the title, the survey content, and the cancel row.
        containerColor = colorScheme.background,
        // Was already meant to be off (see class doc) - without it, the sheet's own drag/scroll
        // gesture detection competes with the WebView for every touch inside it.
        sheetGesturesEnabled = false,
        bottomSheetTag = "satisfaction_survey_bottom_sheet",
        // Material3's ModalBottomSheet always wraps the dragHandle slot in its own
        // Modifier.clickable, regardless of sheetGesturesEnabled, but only fills in that wrapper's
        // semantics (dismiss/expand/collapse labels) when sheetGesturesEnabled is true. With it
        // false (required - see class doc), the wrapper is an unlabeled, 32dp-wide clickable node
        // no matter what's drawn inside it or what semantics are put on it - real accessibility
        // scanners (TouchTargetSizeCheck, SpeakableTextPresentCheck) flag it regardless. Passing
        // null here skips that wrapper entirely; the pill shape is drawn as plain decorative
        // content below instead, which never gets wrapped in a clickable.
        dragHandler = null,
        content = {
            SatisfactionSurveyContent(popup, onDismiss, isLandscape)
        },
    )
}

@Composable
private fun SatisfactionSurveyContent(popup: Popup.SatisfactionSurvey, onDismiss: () -> Unit, isLandscape: Boolean) {
    // This content is composed inside ModalBottomSheet's own dialog window, which doesn't inherit
    // any status-bar handling the host activity does, so hide it here too in landscape.
    LocalView.current.findDialogWindow()?.let { window ->
        HideStatusBar(window, enabled = isLandscape)
    }
    val horizontalPadding = if (isLandscape) space.satisfactionSurveyLandscapeContentPadding else space.semiLarge
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .semantics { testTag = "satisfaction_survey_popup" },
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(vertical = space.satisfactionSurveyDragHandleVerticalPadding)
                .size(width = space.satisfactionSurveyDragHandleWidth, height = space.satisfactionSurveyDragHandleHeight)
                .clip(RoundedCornerShape(50))
                .background(chatColors.token.content.tertiary),
        )
        BottomSheetTitle(
            message = popup.text,
            // BottomSheetTitle's own title Text clears its semantics down to just a test tag, so
            // without this TalkBack would read nothing for the backend-provided prompt. The height
            // cap+scroll stops a long backend prompt from squeezing out the WebView and Cancel row.
            modifier = Modifier
                .heightIn(max = space.satisfactionSurveyTitleMaxHeight)
                .verticalScroll(rememberScrollState())
                .padding(bottom = space.medium)
                .semantics(mergeDescendants = true) { contentDescription = popup.text },
            horizontalPadding = if (isLandscape) space.satisfactionSurveyLandscapeContentPadding else space.xl,
        )
        SatisfactionSurveyWebView(
            surveyUrl = popup.surveyUrl,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        )
        HorizontalDivider(color = chatColors.token.border.default)
        CancelRow(onDismiss, horizontalPadding)
    }
}

/**
 * Matches the Figma "Rate" mock exactly: a plain left-aligned "Cancel" link, no leading icon slot -
 * unlike [BottomSheetActionRow], which always reserves one even when none is supplied.
 */
@Composable
private fun CancelRow(onDismiss: () -> Unit, horizontalPadding: Dp = space.semiLarge) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(space.bottomSheetActionRowHeight)
            .padding(horizontal = horizontalPadding),
        contentAlignment = Alignment.CenterStart,
    ) {
        // Clickable area is bounded to the text, not the full row - a user expects to be able to
        // tap elsewhere in the row without dismissing the survey. minimumInteractiveComponentSize
        // expands this Box to the accessibility minimum, centering its content inside - verified via
        // the "satisfaction survey dialog chrome" screenshot that "Cancel" still sits flush against
        // the row padding, since its own rendered width already exceeds the 48dp floor.
        Box(
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .clickable(role = Role.Button, onClick = onDismiss)
                .semantics { testTag = "satisfaction_survey_cancel_button" },
            contentAlignment = Alignment.CenterStart,
        ) {
            Text(
                text = stringResource(string.cancel),
                color = colorScheme.primary,
                style = chatTypography.bottomSheetActionRowText,
            )
        }
    }
}

/** Walks up this [View]'s parents to find the [Window] of the [DialogWindowProvider] hosting it, if any. */
private tailrec fun View.findDialogWindow(): Window? {
    val provider = this as? DialogWindowProvider
    return provider?.window ?: (parent as? View)?.findDialogWindow()
}

@Composable
@PreviewLightDark
private fun PreviewSurveyContent() {
    ChatTheme {
        SatisfactionSurveyContent(
            popup = createPreviewSurveyPopup(),
            onDismiss = {},
            isLandscape = false,
        )
    }
}

@Composable
@Preview(name = "Landscape", widthDp = 852, heightDp = 400)
private fun PreviewSurveyContentLandscape() {
    ChatTheme {
        SatisfactionSurveyContent(
            popup = createPreviewSurveyPopup(),
            onDismiss = {},
            isLandscape = true,
        )
    }
}
