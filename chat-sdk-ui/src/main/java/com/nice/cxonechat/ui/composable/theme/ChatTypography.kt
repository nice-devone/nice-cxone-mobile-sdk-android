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

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontWeight.Companion.W400
import androidx.compose.ui.text.font.FontWeight.Companion.W500
import androidx.compose.ui.text.font.FontWeight.Companion.W700
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.sp

@Immutable
internal data class ChatTypography(
    val threadListName: TextStyle = Typography.titleMedium,
    val threadListLastMessage: TextStyle = Typography.bodyMedium,
    val threadListLastMessageTime: TextStyle = Typography.bodySmall,
    val chatAgentName: TextStyle = Typography.titleMedium,
    val chatMessage: TextStyle = Typography.bodyLarge,
    val chatEmojiMessage: TextStyle = Typography.headlineLarge.copy(
        fontSize = 50.sp,
        lineHeight = 53.sp,
    ),
    val chatStatus: TextStyle = Typography.bodySmall,
    val chatAttachmentCaption: TextStyle = Typography.bodySmall,
    val chatAttachmentMessage: TextStyle = Typography.titleSmall,
    val chatDayHeader: TextStyle = Typography.titleMedium.copy(
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
    ),
    val chatLoadMoreCaption: TextStyle = Typography.bodySmall.copy(
        fontSize = 12.sp,
        lineHeight = 18.sp,
        fontWeight = W400,
    ),
    val loadingScreenText: TextStyle = Typography.titleMedium,
    val dialogTitle: TextStyle = Typography.headlineSmall,
    val dialogBody: TextStyle = Typography.bodyMedium,
    val chatCardTitle: TextStyle = Typography.titleMedium.copy(
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center
    ),
    val chatCardSubtitle: TextStyle = Typography.titleMedium.copy(
        fontWeight = FontWeight.Normal,
        textAlign = TextAlign.Center
    ),
    val messageAvatarText: TextStyle = Typography.labelSmall.copy(
        fontSize = 10.sp,
        lineHeight = 12.sp,
        fontWeight = W700,
    ),
    val listPickerTitle: TextStyle = Typography.titleMedium.copy(
        fontWeight = FontWeight.SemiBold,
        fontSize = TextUnit(15f, TextUnitType.Sp),
    ),
    val listPickerText: TextStyle = Typography.bodyMedium,
    val quickReplySelectedText: TextStyle = Typography.labelSmall,
    val surveyListItem: TextStyle = Typography.bodySmall,
    val surveyLabel: TextStyle = Typography.labelLarge.copy(
        fontWeight = FontWeight.SemiBold,
        fontSize = TextUnit(14f, TextUnitType.Sp),
        lineHeight = TextUnit(32f, TextUnitType.Sp)
    ),
    val surveyTitle: TextStyle = Typography.titleMedium.copy(
        fontWeight = FontWeight.Medium,
        fontSize = TextUnit(20f, TextUnitType.Sp),
    ),
    val previewTitle: TextStyle = Typography.titleMedium.copy(
        fontWeight = W400,
        fontSize = 17.sp,
    ),
    val overflowText: TextStyle = Typography.titleMedium.copy(
        fontSize = 16.sp,
        lineHeight = 22.sp,
        fontWeight = W700,
        textAlign = TextAlign.Center,
        color = Color(0xFF131214),
    ),
    val chipText: TextStyle = Typography.labelMedium.copy(
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontWeight = W500,
        letterSpacing = 0.1.sp,
    ),
    val noThreads: TextStyle = TextStyle(
        fontSize = 18.sp,
        lineHeight = 27.sp,
        fontWeight = W400,
        textAlign = TextAlign.Center,
    ),
    val popupTitle: TextStyle = TextStyle(
        fontSize = 24.sp,
        lineHeight = 28.8.sp,
        fontWeight = W700,
        textAlign = TextAlign.Center,
    ),
    val popupSubtitle: TextStyle = TextStyle(
        fontSize = 15.sp,
        lineHeight = 21.sp,
        fontWeight = W500,
        textAlign = TextAlign.Center,
    ),
    val popupButton: TextStyle = TextStyle(
        fontSize = 16.sp,
        lineHeight = 16.sp,
        fontWeight = W700,
        textAlign = TextAlign.Center,
    ),
    val topBarTitle: TextStyle = Typography.labelSmall.copy(
        fontSize = 24.sp,
        lineHeight = 32.sp,
        fontWeight = W400,
    ),
    val timestampIndicator: TextStyle = Typography.labelSmall.copy(
        fontWeight = W400,
        fontSize = 11.sp,
        lineHeight = 13.sp,
        letterSpacing = 0.07.sp
    ),
    val audioRecordingTime: TextStyle = Typography.labelSmall.copy(
        fontSize = 13.sp,
        lineHeight = 13.sp,
        fontWeight = W400,
        textAlign = TextAlign.End,
    ),
    val audioRecordingLabel: TextStyle = Typography.labelMedium.copy(
        fontSize = 16.sp,
        lineHeight = 24.sp,
        fontWeight = W400,
    ),
    val documentFallackText: TextStyle = TextStyle(
        fontSize = 16.sp,
        lineHeight = 22.sp,
        fontWeight = W700,
        textAlign = TextAlign.Center,
    ),
    val documentFallackTextSmall: TextStyle = TextStyle(
        fontSize = 11.sp,
        lineHeight = 14.sp,
        fontWeight = W700,
        textAlign = TextAlign.Center,
    ),
    val documentFallackTextTiny: TextStyle = TextStyle(
        fontSize = 14.sp,
        lineHeight = 14.sp,
        fontWeight = W700,
        textAlign = TextAlign.Center,
    ),
    val selectAttachmentBottomBarText: TextStyle = Typography.labelSmall.copy(
        fontSize = 17.sp,
        lineHeight = 16.sp,
        fontWeight = W400,
    ),
)

internal val LocalChatTypography = staticCompositionLocalOf {
    ChatTypography()
}
