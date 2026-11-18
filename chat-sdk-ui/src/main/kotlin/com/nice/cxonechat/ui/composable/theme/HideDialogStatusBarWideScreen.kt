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

import android.view.View
import android.view.Window
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.DialogWindowProvider

/**
 * Hides the status bar on the caller's own dialog window while it is shown on a wide screen, and
 * restores it when the window leaves that state or is dismissed.
 *
 * Intended for sheets and dialogs that open their own window (e.g. Material 3's [ModalBottomSheet],
 * whose `ModalBottomSheetDialogWrapper` doesn't inherit the host activity's hidden status bar), so
 * they can go full height on a wide screen without a status-bar gap. Gated on the same [isWideScreen]
 * predicate the host activity uses to hide its own status bar (see `HideStatusBarOnWideScreen`), so
 * the two stay in lockstep instead of drifting apart.
 *
 * The status bar can always be revealed again by swiping from the edge.
 */
@Composable
internal fun HideDialogStatusBarWideScreen() {
    // LocalView.current is the AndroidComposeView the caller's own AbstractComposeView (e.g. a
    // ModalBottomSheet's dialogLayout) creates internally to host this composition, not that
    // AbstractComposeView itself, so the DialogWindowProvider has to be found by walking up its
    // View parents.
    val window = LocalView.current.findDialogWindow() ?: return
    HideStatusBar(window, enabled = isWideScreen())
}

/** Walks up this [View]'s parents to find the [Window] of the [DialogWindowProvider] hosting it, if any. */
internal tailrec fun View.findDialogWindow(): Window? {
    val provider = this as? DialogWindowProvider
    return provider?.window ?: (parent as? View)?.findDialogWindow()
}
