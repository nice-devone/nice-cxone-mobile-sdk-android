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
    val threadListName: TextStyle = Typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
    val threadListLastMessage: TextStyle = Typography.bodyMedium,
    val chatAgentName: TextStyle = Typography.titleMedium,
    val chatMessage: TextStyle = Typography.bodyLarge,
    val chatStatus: TextStyle = Typography.bodySmall,
    val chatAttachmentCaption: TextStyle = Typography.bodySmall,
    val chatAttachmentMessage: TextStyle = Typography.titleSmall,
    val chatDayHeader: TextStyle = Typography.titleMedium,
    val chatLoadMoreCaption: TextStyle = Typography.bodySmall,
    val dialogTitle: TextStyle = Typography.titleLarge,
    val dialogBody: TextStyle = Typography.bodyMedium,
    val chatCardTitle: TextStyle = Typography.titleMedium.copy(
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center
    ),
    val chatCardSubtitle: TextStyle = Typography.titleMedium.copy(
        fontWeight = FontWeight.Normal,
        textAlign = TextAlign.Center
    ),
    val offlineBanner: TextStyle = Typography.titleLarge,
    val offlineMessage: TextStyle = Typography.bodyLarge,
    val surveyListItem: TextStyle = Typography.bodySmall,
)

internal val LocalChatTypography = staticCompositionLocalOf {
    ChatTypography()
}
