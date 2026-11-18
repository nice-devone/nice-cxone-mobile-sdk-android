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

package com.nice.cxonechat.ui.screen

import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import com.nice.cxonechat.ui.composable.theme.HideStatusBar
import com.nice.cxonechat.ui.composable.theme.isWideScreen

/**
 * Hides the system status bar while the chat is shown on a wide screen, reclaiming its space for
 * content, and shows it again otherwise.
 *
 * Only the status bar is hidden. The navigation bar stays visible, since hiding it would remove the
 * only visible back affordance on devices using three-button navigation.
 *
 * The status bar can always be revealed again by swiping from the edge.
 */
@Composable
internal fun HideStatusBarOnWideScreen() {
    // No activity means no window to control, so nothing to hide.
    val activity = LocalActivity.current ?: return
    HideStatusBar(activity.window, enabled = isWideScreen())
}
