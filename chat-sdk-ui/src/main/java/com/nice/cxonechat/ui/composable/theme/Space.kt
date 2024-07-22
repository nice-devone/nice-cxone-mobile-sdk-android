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

package com.nice.cxonechat.ui.composable.theme

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
internal data class Space(
    val customer: Dp = 40.dp,
    val large: Dp = 16.dp,
    val medium: Dp = 8.dp,
    val small: Dp = 4.dp,

    val defaultPadding: PaddingValues = PaddingValues(vertical = medium, horizontal = large),
    val customerPadding: PaddingValues = PaddingValues(start = customer),
    val agentPadding: PaddingValues = PaddingValues(end = customer),
    val messagePadding: Dp = large,
    val clickableSize: Dp = 48.dp,
    val treeFieldIndent: Dp = large,
    val dismissThreshold: Dp = 56.dp,
    val agentImageSize: Dp = 56.dp,
    val chipIconSize: Dp = 24.dp,
    val chipSpace: Dp = medium,
    val chipPadding: PaddingValues = PaddingValues(vertical = medium, horizontal = large),
    val menuElementHeight: Dp = 100.dp,
    val titleBarLogoSize: Dp = 24.dp, // per android recommendations
    val titleBarLogoPadding: Float = 2f, // per android recommendations this is in Pixels, not Dp.

    /** how many icons to display in the message window. */
    val smallAttachmentCount: Int = 4,
    /** size of icons in the message window. */
    val smallAttachmentSize: Dp = 44.dp,
    /** padding around icons in the message window. */
    val smallAttachmentPadding: PaddingValues = PaddingValues(small),

    /** size of icons in the attachment sharing dialog. */
    val largeAttachmentSize: Dp = 60.dp,
    /** padding around icons in the attachment sharing dialog. */
    val largeAttachmentPadding: PaddingValues = PaddingValues(small),

    /** selected frame stroke width. */
    val selectedFrameWidth: Dp = 3.dp,
    /** unselected frame stroke width. */
    val unselectedFrameWidth: Dp = 1.dp,
)

internal val LocalSpace = staticCompositionLocalOf {
    Space()
}
