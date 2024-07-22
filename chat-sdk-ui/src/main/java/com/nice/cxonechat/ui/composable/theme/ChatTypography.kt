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

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign

@Immutable
internal data class ChatTypography(
    val threadListName: TextStyle = Typography.body1.copy(fontWeight = FontWeight.Bold),
    val threadListLastMessage: TextStyle = Typography.body2,
    val chatAgentName: TextStyle = Typography.subtitle1,
    val chatMessage: TextStyle = Typography.body1,
    val chatStatus: TextStyle = Typography.caption,
    val chatAttachmentCaption: TextStyle = Typography.caption,
    val chatAttachmentMessage: TextStyle = Typography.subtitle2,
    val chatDayHeader: TextStyle = Typography.subtitle1,
    val chatLoadMoreCaption: TextStyle = Typography.caption,
    val dialogTitle: TextStyle = Typography.h6,
    val chatCardTitle: TextStyle = Typography.subtitle1.copy(
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center
    ),
    val chatCardSubtitle: TextStyle = Typography.subtitle1.copy(
        fontWeight = FontWeight.Normal,
        textAlign = TextAlign.Center
    ),
    val offlineBanner: TextStyle = Typography.h6,
    val offlineMessage: TextStyle = Typography.body1,
)

internal val LocalChatTypography = staticCompositionLocalOf {
    ChatTypography()
}
