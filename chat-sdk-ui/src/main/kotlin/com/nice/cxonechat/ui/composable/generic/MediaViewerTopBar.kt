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

import androidx.compose.runtime.Composable
import com.nice.cxonechat.ui.composable.theme.BackButton
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import com.nice.cxonechat.ui.composable.theme.ChatTheme.chatColors
import com.nice.cxonechat.ui.composable.theme.ShareButton
import com.nice.cxonechat.ui.composable.theme.TopBar

/**
 * The header of a full screen media viewer, carrying only its dismiss and share actions.
 *
 * Shared by [ImageViewerDialogCard] and [VideoView] so both headers stay identical: the two differ in the
 * media they show, not in the actions over it.
 *
 * Built on the shared app bar to inherit its wide-screen insets, so the actions line up with the
 * conversation's content column instead of sitting against the window edge. The bar shows no title: the media
 * occupies the whole screen behind it, and its name is already announced through the share action.
 *
 * The divider below it belongs to each viewer: the shared app bar draws none of its own.
 *
 * @param title Passed to [ShareButton] as the subject of the share intent.
 * @param onDismiss An action triggered when the back button is clicked.
 * @param onShare An action triggered when the share button is clicked.
 */
@Composable
internal fun MediaViewerTopBar(
    title: String?,
    onDismiss: () -> Unit,
    onShare: () -> Unit,
) {
    ChatTheme.TopBar(
        title = null,
        containerColor = chatColors.token.background.default,
        contentColor = chatColors.token.content.primary,
        navigationIcon = { BackButton(onDismiss) },
        actions = { ShareButton(title, onShare) },
    )
}
