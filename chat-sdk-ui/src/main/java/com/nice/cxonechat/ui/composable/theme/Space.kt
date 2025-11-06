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

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp

@Immutable
internal data class Space(
    val customer: Dp = 40.dp,
    val xxl: Dp = 40.dp,
    val xl: Dp = 24.dp,
    val large: Dp = 16.dp,
    val semiLarge: Dp = 12.dp,
    val medium: Dp = 8.dp,
    val small: Dp = 4.dp,
    val xSmall: Dp = 2.dp,

    val defaultPadding: PaddingValues = PaddingValues(vertical = medium, horizontal = large),
    val customerPadding: PaddingValues = PaddingValues(start = customer),
    val agentPadding: PaddingValues = PaddingValues(end = customer),
    val messagePadding: PaddingValues = PaddingValues(horizontal = xl, vertical = semiLarge),
    val emojiPadding: PaddingValues = PaddingValues(vertical = xSmall),
    val unsupportedMessagePadding: PaddingValues = PaddingValues(start = xl, end = xl, top = semiLarge, bottom = medium),
    val audioMessagePadding: PaddingValues = PaddingValues(start = xl, end = xl, top = semiLarge, bottom = 3.dp),
    val audioRecordingTextPadding: PaddingValues = PaddingValues(start = 6.dp, end = medium),
    val userInputUnderlinePadding: PaddingValues = PaddingValues(start = semiLarge, end = semiLarge, bottom = 6.dp),
    val quickReplyMessagePadding: PaddingValues = PaddingValues(start = xl, end = xl, top = semiLarge, bottom = medium),
    val attachmentMessagePadding: PaddingValues = PaddingValues(horizontal = semiLarge, vertical = semiLarge),
    val richListPickerTextPadding: PaddingValues = PaddingValues(start = xl, end = xl, top = semiLarge, bottom = medium),

    val clickableSize: Dp = 48.dp,
    val treeFieldIndent: Dp = large,
    val dismissThreshold: Dp = 56.dp,

    val agentImageSize: Dp = 40.dp,
    val messageAvatarBottomPadding: Dp = 22.dp,
    val messageAvatarSize: Dp = 24.dp,
    val listPickerIconSize: Dp = 64.dp,
    val bottomSheetTitleIconSize: Dp = 40.dp,
    val bottomSheetActionItemSize: Dp = 32.dp,
    val listPickerItemImageSize: Dp = 32.dp,

    val chipMinSize: Dp = xxl,
    val chipSpace: Dp = medium,
    val chipPadding: PaddingValues = PaddingValues(vertical = 10.dp, horizontal = xl),
    val chipIconSize: Dp = xl,
    val menuElementHeight: Dp = 100.dp,
    val titleBarLogoSize: Dp = 24.dp, // per android recommendations
    val titleBarLogoPadding: Float = 2f, // per android recommendations this is in Pixels, not Dp.

    /** how many rows of attachment items to display in the message window. */
    val smallAttachmentRowCount: Int = 2,
    /** how many items per row to display in the message window. */
    val smallAttachmentRowSizeCount: Int = 2,
    /** size of icons in the message window. */
    val smallAttachmentSize: Dp = 44.dp,
    /** padding around icons in the message window. */
    val smallAttachmentPadding: PaddingValues = PaddingValues(small),

    /** size of icons in the attachment sharing dialog. */
    val largeAttachmentSize: Dp = 60.dp,
    /** padding around icons in the attachment sharing dialog. */
    val largeAttachmentPadding: PaddingValues = PaddingValues(small),
    /** Preview item frame stroke width. */
    val framePreviewWidth: Dp = 1.dp,
    /** Size of the attachment preview item when it is displayed for review before upload. */
    val attachmentUploadPreviewSize: DpSize = DpSize(width = 72.dp, height = 64.dp),
    /** Clickable size of the icon for removing the attachment before upload. */
    val attachmentUploadRemoveClickableSize: Dp = 32.dp,
    /** size of loading progress indicator. */
    val loadingIndicatorSize: Dp = 72.dp,
    /** Size of the icon which will trigger video playback. */
    val playVideoIconSize: Dp = 32.dp,
    /** Size of the icon used as a placeholder for the video when it's player is preparing or in case of an error. */
    val playStatusIconSize: Dp = 128.dp,
    /** Space between the audio player icons. */
    val audioPlayerIconSpacing: Dp = large,
    /** Size of the icon used to play/pause the audio. */
    val audioPlayerPlayIconSize: Dp = clickableSize,
    /** Size of the icons used to seek the audio track. */
    val audioPlayerSecondaryIconSize: Dp = 35.dp,

    /** Minimal height of the popup button. */
    val popupButtonMinHeight: Dp = 49.5.dp,
    /** Padding around the popup root layout ([androidx.compose.material3.ElevatedCard]. */
    val popupPaddingValues: PaddingValues = PaddingValues(start = 25.5.dp, top = 32.dp, end = 25.5.dp, bottom = 20.dp),
    /** Maximum width of the popup content. */
    val popupMaxWidth: Dp = 361.dp,
    /** Elevation of the popup [androidx.compose.material3.ElevatedCard]. */
    val popupElevation: Dp = 16.dp,
    /** Height of the popup header slot. */
    val popupHeaderHeight: Dp = 135.dp,
    /** Padding around the popup header slot. */
    val popupHeaderPaddingValues: PaddingValues = PaddingValues(start = 8.dp, top = 8.dp, end = 8.dp, bottom = 22.dp),
    /** Padding around the popup body slot. */
    val popupContentPaddingValues: PaddingValues = PaddingValues(start = 24.dp, top = 28.dp, end = 24.dp, bottom = 24.dp),
    /** Size of the icon used in the tooltip message subframe. */
    val tooltipIconSize: Dp = 16.dp,
    /** Minimal size of the attachment preview - large variant. */
    val attachmentPreviewLargeSize: DpSize = DpSize(width = 170.dp, height = 217.dp),
    /** Minimal size of the attachment preview - regular variant - as a width percentage fraction. */
    val attachmentPreviewRegularWidthPercentage: Float = 0.2718f,
    /** Minimal size of the attachment preview - small variant. */
    val attachmentPreviewSmallSize: DpSize = DpSize(width = 72.dp, height = 64.dp),
    /** Size of the icon used in the position in queue screen. */
    val positionInQueueIconSize: Dp = 56.dp,
    /** Extra padding for the leading icon vector in the header bar. */
    val headerBarLeadingIconPadding: Dp = 11.2.dp,
    /** Size of the fallback thumbnail in the attachment preview - large variant. */
    val attachmentPreviewFallbackLargeSize: DpSize = DpSize(width = 126.dp, height = 164.dp),
    /** Height of the label shown on top of the fallback thumbnail in the attachment preview - small variant. */
    val attachmentPreviewFallbackSmallSizeLabelHeight: Dp = 14.dp,
    /** Size of the fallback thumbnail in the attachment preview - small variant. */
    val attachmentPreviewFallbackSmallSize: DpSize = DpSize(width = 40.dp, height = 52.dp),
    /** Padding around the attachment preview content. */
    val attachmentPreviewPaddingValues: PaddingValues = PaddingValues(horizontal = small, vertical = 6.dp),

    val bottomSheetCardHeight: Dp = 72.dp,
    val bottomSheetBorderWidth: Dp = 1.dp,
    val bottomSheetActionRowHeight: Dp = 56.dp,
    /** Horizontal padding for the image in the offline screen. */
    val offlineImageHorizontalPadding: Dp = 55.dp,
    /** Minimum screen height for showing the image in the offline screen. */
    val offlineImageMinimumScreenHeight: Dp = 530.dp,
    /** Size of the icon in the offline screen header bar. */
    val offlineIconSize: Dp = 56.dp,
    /** Size of the icon in the swipe to dismiss component. */
    val swipeToDismissIconSize: Dp = 30.dp,
    /** Extra padding for the leading icon vector in the header bar. */
    val offlineLeadingIconPadding: Dp = 11.2.dp,
)

internal val LocalSpace = staticCompositionLocalOf {
    Space()
}
