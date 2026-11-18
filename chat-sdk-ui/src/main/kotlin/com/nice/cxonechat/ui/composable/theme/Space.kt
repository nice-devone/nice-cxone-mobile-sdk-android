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

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.nice.cxonechat.ui.composable.generic.MinTouchTarget

@Immutable
internal data class Space(
    val customer: Dp = 40.dp,
    val xxl: Dp = 40.dp,
    val semiXxl: Dp = 32.dp,
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
    /**
     * Inset of an audio attachment cell's content from its bubble.
     *
     * The design declares 16dp on all four sides, but draws its 48dp play glyph overflowing the 28dp row that
     * holds it, which leaves 6dp of visible clearance below. Compose lays that button out at its full height
     * instead of letting it overflow, so the bottom inset is the design's *visible* one rather than its declared
     * one; the two describe the same pixels.
     */
    val audioMessagePadding: PaddingValues = PaddingValues(start = large, end = large, top = large, bottom = 6.dp),
    val audioRecordingTextPadding: PaddingValues = PaddingValues(start = 6.dp, end = medium),
    val userInputUnderlinePadding: PaddingValues = PaddingValues(start = semiLarge, end = semiLarge, bottom = 6.dp),
    val attachmentMessagePadding: PaddingValues = PaddingValues(horizontal = semiLarge, vertical = semiLarge),
    val richListPickerTextPadding: PaddingValues = PaddingValues(start = xl, end = xl, top = semiLarge, bottom = medium),
    val richTimePickerTextPadding: PaddingValues = PaddingValues(start = xl, end = xl, top = semiLarge, bottom = medium),

    /** Smallest tappable size, defaulted to the platform accessibility floor so the two cannot drift. */
    val clickableSize: Dp = MinTouchTarget,
    val treeFieldIndent: Dp = large,
    val dismissThreshold: Dp = 56.dp,

    val agentImageSize: Dp = 40.dp,
    val messageAvatarBottomPadding: Dp = 22.dp,
    val messageAvatarSize: Dp = 24.dp,
    /** Padding around the placeholder icon inside [messageAvatarSize]-sized avatars; scales with the target avatar size. */
    val messageAvatarIconPadding: Dp = 6.dp,
    val bottomSheetTitleIconSize: Dp = 40.dp,
    val bottomSheetActionItemSize: Dp = semiXxl,
    val listPickerItemImageSize: Dp = 64.dp,
    /** Narrowest a time slot may be; the time picker stretches its columns from here to fill the row. */
    val timeSlotMinWidth: Dp = 104.dp,
    val timeSlotHeight: Dp = 58.dp,
    val timeSlotPadding: PaddingValues = PaddingValues(vertical = 10.dp, horizontal = medium),

    val chipMinSize: Dp = xxl,
    val chipSpace: Dp = medium,
    val chipPadding: PaddingValues = PaddingValues(vertical = 10.dp, horizontal = xl),
    val chipIconSize: Dp = xl,
    val menuElementHeight: Dp = 100.dp,
    val actionMenuIconSize: Dp = 24.dp,

    /** how many rows of attachment items to display in the message window. */
    val smallAttachmentRowCount: Int = 2,
    /** how many items per row to display in the message window, can't be equal to 0. */
    val smallAttachmentRowSizeCount: Int = 2,
    /** size of icons in the message window. */
    val smallAttachmentSize: Dp = 44.dp,
    /** padding around icons in the message window. */
    val smallAttachmentPadding: PaddingValues = PaddingValues(small),

    /** size of icons in the attachment sharing dialog. */
    val largeAttachmentSize: Dp = 60.dp,
    /** padding around icons in the attachment sharing dialog. */
    val largeAttachmentPadding: PaddingValues = PaddingValues(small),
    /**
     * Size of the share glyph on an attachment in a conversation, inside its 34dp button.
     *
     * An [androidx.compose.material3.Icon] defaults to 24dp, which crowds the button's outline.
     */
    val attachmentShareIconSize: Dp = 16.dp,
    /** Preview item frame stroke width. */
    val framePreviewWidth: Dp = 1.dp,
    /** Size of the attachment preview item when it is displayed for review before upload. */
    val attachmentUploadPreviewSize: DpSize = DpSize(width = 72.dp, height = 64.dp),
    /**
     * Side of the square attachment preview item shown for review before upload while the bar is compact, as a
     * share of the window's shorter edge.
     */
    val attachmentUploadPreviewCompactSizeFraction: Float = 1f / 8,
    /** Gap between attachment preview items shown for review before upload, where the remove button's target stops. */
    val attachmentUploadPreviewGap: Dp = medium,
    /** Size of the circle drawn for removing the attachment before upload. */
    val attachmentUploadRemoveCircleSize: Dp = semiXxl,
    /**
     * Touch target of the button removing the attachment before upload: Material's minimum, around a smaller circle.
     *
     * Always its height. Its width gives way to [attachmentUploadRemoveCentreClearance] wherever the attachment is too
     * narrow for both.
     */
    val attachmentUploadRemoveTouchTargetSize: Dp = 48.dp,
    /**
     * How far short of the centre of its attachment the remove button's touch target stops, so a tap there opens the
     * attachment rather than removing it. The target still covers the whole circle.
     */
    val attachmentUploadRemoveCentreClearance: Dp = small,
    /**
     * How far the remove icon hangs past the top trailing corner of the attachment it belongs to, on both
     * axes.
     *
     * Also the trailing room the row has to keep scrollable for the last attachment's icon to stay whole:
     * a horizontally scrollable container clips its content exactly on the main axis, so anything past the
     * last attachment is cut off unless the row scrolls that far.
     */
    val attachmentUploadRemoveOverhang: Dp = 11.dp,
    /**
     * Minimum height of the attachment preview row; the design's height whenever the content fits.
     *
     * A floor rather than a fixed height, so the item labels grow instead of clipping when text scales
     * up, as [headerBarMinHeight] does. At the default text scale the floor is what applies, giving the
     * design's 100.dp.
     */
    val attachmentPreviewBarMinHeight: Dp = 100.dp,
    /** Inset between the top of the attachment preview row and the attachments inside it. */
    val attachmentPreviewBarContentTopPadding: Dp = 10.dp,
    /**
     * Inset between the attachments and the scrollbar strip reserved below them.
     *
     * The design insets the row by [attachmentPreviewBarContentTopPadding] at both ends; this is 2.dp short of
     * it because an attachment measures 82.dp against the design's 80.dp. The design pins its label box to
     * 14.dp under the 16.sp line height it gives that same label, where ours keeps `bodySmall`'s line box
     * whole. Absorbing those 2.dp here rather than clipping the label puts every drawn edge where the design
     * has it, and keeps this gap the same however tall text scaling makes the labels — slack inside
     * [attachmentPreviewBarMinHeight] would be the first thing they grew out of.
     */
    val attachmentPreviewBarContentBottomPadding: Dp = 8.dp,
    /**
     * Width of the fade drawn over the trailing edge of the attachment preview row.
     *
     * One width at every window size. The compact and wide design frames differ by 0.4.dp, which is an
     * artefact of the design file rather than a distinction to reproduce.
     */
    val attachmentPreviewBarFadeWidth: Dp = 80.dp,
    /** size of loading progress indicator. */
    val loadingIndicatorSize: Dp = 72.dp,
    /** Size of the icon which will trigger video playback. */
    val playVideoIconSize: Dp = semiXxl,
    /** Size of the icon used as a placeholder for the video when it's player is preparing or in case of an error. */
    val playStatusIconSize: Dp = 128.dp,
    /**
     * Gap between the audio player's control buttons.
     *
     * The iOS design's gap, which takes precedence while the two Android frames disagree — the compact one
     * draws 35dp between controls of unequal size, the wide one 20dp between equal ones. Kept as a gap rather
     * than converted to a centre distance, which would be a different number again, since each button here is
     * an [androidx.compose.material3.IconButton] measuring [clickableSize] whatever glyph it paints.
     */
    val audioPlayerIconSpacing: Dp = 20.dp,
    /**
     * Size of the icon used to play/pause the audio, and of the button drawn around it.
     *
     * The design makes this the largest control in the playback row, half again the seek glyphs either side, so
     * that it reads as the primary one. Material 3 would clamp the glyph to its own 40dp small-icon-button
     * container and crop the overflow, which is why the player applies this to the container as well.
     */
    val audioPlayerPlayIconSize: Dp = clickableSize,
    /** Size of the icons used to seek the audio track. */
    val audioPlayerSecondaryIconSize: Dp = 34.dp,
    /** Height of the audio player's progress bar, the one internal measurement both design frames agree on. */
    val audioPlayerProgressBarHeight: Dp = 6.dp,
    /** Space between the audio player's progress bar and the elapsed and remaining times either side of it. */
    val audioPlayerProgressSpacing: Dp = 10.dp,

    /** Minimal height of the popup button. */
    val popupButtonMinHeight: Dp = 49.5.dp,
    /** Padding around the popup root layout ([androidx.compose.material3.ElevatedCard]. */
    val popupPaddingValues: PaddingValues = PaddingValues(start = 25.5.dp, top = semiXxl, end = 25.5.dp, bottom = 20.dp),
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
    /**
     * Elevation of the disabled/selected time picker message's tooltip.
     *
     * [androidx.compose.material3.PlainTooltip] has no shadow by default; the design draws one,
     * needed here since the tooltip's fill can be the same colour as the surface it appears over.
     */
    val tooltipShadowElevation: Dp = 3.dp,
    /**
     * Padding around the text in the disabled/selected time picker message's tooltip.
     *
     * [androidx.compose.material3.PlainTooltip] bakes in its own 8dp horizontal / 4dp vertical
     * content padding, which it does not expose a parameter to override; this tops it up to the
     * design's 16dp on every side (this padding's [medium] + the built-in 8dp horizontally,
     * [semiLarge] + the built-in [small] vertically) rather than fighting the built-in amount.
     */
    val tooltipTextPadding: PaddingValues = PaddingValues(horizontal = medium, vertical = semiLarge),
    /** Minimal size of the attachment preview - large variant. */
    val attachmentPreviewLargeSize: DpSize = DpSize(width = 170.dp, height = 217.dp),
    /** Minimal size of the attachment preview - regular variant - as a width percentage fraction. */
    val attachmentPreviewRegularWidthPercentage: Float = 0.2718f,
    /** Minimal size of the attachment preview - small variant. */
    val attachmentPreviewSmallSize: DpSize = DpSize(width = 72.dp, height = 64.dp),
    /** Size of the icon used in the position in queue screen. */
    val positionInQueueIconSize: Dp = 56.dp,
    /** Bottom padding for the header bar. */
    val headerBarBottomPadding: Dp = 10.dp,
    /**
     * Minimum height of the header bar; the design's height when the content fits.
     *
     * A floor rather than a fixed height, so the bar still grows instead of clipping when text scales up.
     * Stated rather than left to the list item default, which the current overload derives differently from
     * the deprecated one and which is still flag gated in the Material 3 alpha.
     */
    val headerBarMinHeight: Dp = 104.dp,
    /** Vertical inset of the header bar content; the list item default is 10dp, or 12dp for precision pointers. */
    val headerBarContentVerticalPadding: Dp = medium,
    /** Extra gap after the header bar leading icon, on top of the 12dp the list item already applies. */
    val headerBarLeadingGap: Dp = small,
    /** Extra padding for the leading icon vector in the header bar. */
    val headerBarLeadingIconPadding: Dp = 11.2.dp,
    /** Size of the fallback thumbnail in the attachment preview - large variant. */
    val attachmentPreviewFallbackLargeSize: DpSize = DpSize(width = 126.dp, height = 164.dp),
    /** Height of the label shown on top of the fallback thumbnail in the attachment preview - small variant. */
    val attachmentPreviewFallbackSmallSizeLabelHeight: Dp = 14.dp,
    /** Size of the fallback thumbnail in the attachment preview - small variant. */
    val attachmentPreviewFallbackSmallSize: DpSize = DpSize(width = 40.dp, height = 52.dp),
    /**
     * Width of the document glyph in the compact fallback thumbnail, as a share of the tile's width.
     *
     * The compact tile is sized from the window rather than by a token, so its contents are shares of it: this, the
     * glyph height and the label width are the design's 32 x 42.dp glyph and 42.dp label on its 56.dp tile.
     */
    val attachmentPreviewFallbackCompactGlyphWidthFraction: Float = 32f / 56,
    /** Height of the document glyph in the compact fallback thumbnail, as a share of the tile's height. */
    val attachmentPreviewFallbackCompactGlyphHeightFraction: Float = 42f / 56,
    /** Minimum width of the extension label in the compact fallback thumbnail, as a share of the tile's width. */
    val attachmentPreviewFallbackCompactLabelMinWidthFraction: Float = 42f / 56,
    /** Padding around the attachment preview content. */
    val attachmentPreviewPaddingValues: PaddingValues = PaddingValues(horizontal = small, vertical = 6.dp),

    val bottomSheetCardHeight: Dp = 72.dp,
    val bottomSheetBorderWidth: Dp = 1.dp,
    val bottomSheetActionRowHeight: Dp = 56.dp,
    /** Horizontal padding for the image in the offline screen. */
    val offlineImageHorizontalPadding: Dp = 55.dp,
    /** Minimum screen height for showing the image in the offline screen. */
    val offlineImageMinimumScreenHeight: Dp = 530.dp,
    /** Bottom padding of the offline screen close button, which is smaller than its remaining padding. */
    val offlineCloseButtonBottomPadding: Dp = 32.dp,
    /** Size of the icon in the offline screen header bar. */
    val offlineIconSize: Dp = 56.dp,
    /** Size of the icon in the swipe to dismiss component. */
    val swipeToDismissIconSize: Dp = 30.dp,
    /** Extra padding for the leading icon vector in the header bar. */
    val offlineLeadingIconPadding: Dp = 11.2.dp,
    /** Maximum width of the loading overlay dialog content. */
    val loadingOverlayMaxWidth: Dp = 312.dp,
    /** Maximum width of buttons suggested by Material 3 guidelines. */
    val maxButtonWidth: Dp = 320.dp,
    /** Minimum size of the rich link media's loading placeholder, before the real image size is known. */
    val richLinkMediaMinSize: Dp = 236.dp,
    /** Height of the archived thread info bar. */
    val archivedInfoBarHeight: Dp = 52.dp,
    /** Minimum screen width at which wide-screen horizontal padding is applied (Material3 expanded width class). */
    val expandedWidthBreakpoint: Dp = 840.dp,
    /** Maximum width of a message bubble as a fraction of the content column width. */
    val messageBubbleMaxWidthFraction: Float = 0.6f,
    /** Horizontal padding applied to the conversation scaffold (app bar, message list, input bar) on wide/landscape screens. */
    val landscapeHorizontalPadding: Dp = 64.dp,
    /**
     * Maximum width a component grows to, which a wider window centres rather than lets it fill.
     *
     * Shared by the inset header bar and by the media in a full screen viewer, so the two cannot drift. A
     * window narrower than this shrinks them to fit its own horizontal margin instead.
     */
    val componentMaxWidth: Dp = 640.dp,
    /** Maximum height of a Rich Link card in landscape, as a fraction of the window height. */
    val richLinkLandscapeMaxHeightFraction: Float = 0.6f,
    /**
     * Margin Material3 `Scaffold` applies to an end-positioned FAB (its internal `FabSpacing`).
     * Subtracted from the desired content inset so the FAB's trailing edge lines up with the content
     * column rather than being pushed a further 16dp inward on wide screens.
     */
    val scaffoldFabEndMargin: Dp = 16.dp,
    /** Fixed width of the thread list long-press context menu (wide-screen design frame, DE-163621). */
    val contextMenuWidth: Dp = 260.dp,
    /** Material3 `DropdownMenuItem` container height, used to estimate the context menu height. */
    val contextMenuItemHeight: Dp = 48.dp,
    /** Material3 `DropdownMenu` vertical content padding (applied at both top and bottom). */
    val contextMenuVerticalPadding: Dp = 8.dp,
    /** Material3 `MenuVerticalMargin` kept between the context menu and the window edges. */
    val contextMenuWindowMargin: Dp = 48.dp,
    /**
     * Extra horizontal inset which aligns a sheet top app bar's title with the sheet body content.
     *
     * Material 3 reserves a fixed start inset for the navigation icon slot even when no icon is
     * present, so only the remainder has to be added to reach the sheet content alignment.
     */
    val sheetTopBarStartInset: Dp = 8.dp,
    /** Padding from the top of a sheet without a drag handle to the first line of its content. */
    val sheetContentTopPadding: Dp = semiXxl,
    /**
     * Bottom padding which lets the send transcript content scroll clear of its sticky footer.
     *
     * The footer is aligned over the content rather than laid out below it, so the content has to
     * reserve room for it.
     */
    val sendTranscriptContentBottomPadding: Dp = 72.dp,
    /**
     * Thickness of a scrollbar's indicator, and of the track it travels along.
     *
     * Too thin to hit on its own, so `ScrollbarGrabMargin` claims a fixed 4.dp either side of it. That
     * margin does not scale with this value: making the bar thinner narrows what is reachable, and the
     * total stays well short of a 48.dp target either way, deliberately.
     */
    val scrollbarThickness: Dp = 4.dp,
    /**
     * Inset between the viewport edges and the scrollbar drawn over them.
     *
     * Placement only. It does not widen the area a press may grab the bar by.
     */
    val scrollbarInset: Dp = 4.dp,
    /** Wider, uniform horizontal gutter for the Satisfaction Survey title and cancel row in landscape. */
    val satisfactionSurveyLandscapeContentPadding: Dp = 64.dp,
    /** Caps the Satisfaction Survey title's height so a long backend prompt scrolls instead of squeezing out the WebView. */
    val satisfactionSurveyTitleMaxHeight: Dp = 72.dp,
    /** Width of the decorative (non-interactive) Satisfaction Survey drag handle pill. */
    val satisfactionSurveyDragHandleWidth: Dp = 32.dp,
    /** Height of the decorative (non-interactive) Satisfaction Survey drag handle pill. */
    val satisfactionSurveyDragHandleHeight: Dp = 4.dp,
    /** Vertical padding around the drag handle pill - matches [BottomSheetDefaults.DragHandle]'s own spacing (48dp total). */
    val satisfactionSurveyDragHandleVerticalPadding: Dp = 22.dp,
)

internal val LocalSpace = staticCompositionLocalOf {
    Space()
}
