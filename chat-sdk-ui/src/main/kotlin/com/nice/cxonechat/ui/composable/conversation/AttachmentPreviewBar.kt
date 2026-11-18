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

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.zIndex
import com.nice.cxonechat.message.Attachment
import com.nice.cxonechat.ui.R.string
import com.nice.cxonechat.ui.composable.conversation.attachments.AttachmentFramedSmallPreview
import com.nice.cxonechat.ui.composable.generic.ThumbnailSize
import com.nice.cxonechat.ui.composable.generic.scrollbar.LocalScrollbarConfig
import com.nice.cxonechat.ui.composable.generic.scrollbar.ScrollbarConfig
import com.nice.cxonechat.ui.composable.generic.scrollbar.ScrollbarState
import com.nice.cxonechat.ui.composable.generic.scrollbar.rememberScrollbarState
import com.nice.cxonechat.ui.composable.generic.scrollbar.reservedThickness
import com.nice.cxonechat.ui.composable.generic.scrollbar.scrollbar
import com.nice.cxonechat.ui.composable.icons.ChatIcons
import com.nice.cxonechat.ui.composable.icons.filled.Cancel
import com.nice.cxonechat.ui.composable.icons.filled.CancelDark
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import com.nice.cxonechat.ui.composable.theme.ChatTheme.chatColors
import com.nice.cxonechat.ui.composable.theme.ChatTheme.colorScheme
import com.nice.cxonechat.ui.composable.theme.ChatTheme.space
import com.nice.cxonechat.ui.composable.theme.ChatTheme.typography
import com.nice.cxonechat.ui.composable.theme.Space
import com.nice.cxonechat.ui.composable.theme.wideScreenHorizontalPadding
import com.nice.cxonechat.ui.util.ofWindowShorterEdge
import com.nice.cxonechat.ui.util.typeAndName

/**
 * Attachments prepared for upload, shown above the message input.
 *
 * @param attachments Attachments awaiting upload, in the order they are shown. An empty list hides the bar.
 * @param onAttachmentClick Called when an attachment is opened.
 * @param onAttachmentRemoved Called when an attachment is removed from the selection.
 * @param horizontalPadding Wide-screen content inset, which replaces the compact one rather than adding to
 * it: the wide design frame puts the first attachment at the inset itself.
 * @param compact Whether to draw smaller attachments without their names, for when the bar has to share little
 * height with the message input. Changes animate.
 */
@Composable
internal fun AttachmentPreviewBar(
    attachments: List<Attachment>,
    onAttachmentClick: (Attachment) -> Unit,
    onAttachmentRemoved: (Attachment) -> Unit,
    horizontalPadding: Dp = 0.dp,
    compact: Boolean = false,
) {
    val compactness by animateFloatAsState(
        targetValue = if (compact) 1f else 0f,
        animationSpec = tween(),
        label = "compactness",
    )
    AnimatedVisibility(
        modifier = Modifier.testTag("attachment_preview_bar"),
        visible = attachments.isNotEmpty()
    ) {
        Column {
            HorizontalDivider()
            AttachmentPreviewRow(
                attachments = attachments,
                onAttachmentClick = onAttachmentClick,
                onAttachmentRemoved = onAttachmentRemoved,
                contentInset = maxOf(space.semiLarge, horizontalPadding),
                compact = compact,
                compactness = compactness,
            )
        }
    }
}

/**
 * The scrollable strip of attachments, the scrollbar below it, and the fade over its trailing edge.
 *
 * [contentInset] is applied as content padding rather than as a layout inset, which earns three things at
 * once. The trailing inset sits past the last attachment in scroll space, so it stays out of sight until
 * the row reaches its end and needs no animating away. It leaves the viewport width alone, so it cannot
 * shrink the scroll range the fade is shown from. And it falls inside the row's clip, so it is what keeps
 * the last [Space.attachmentUploadRemoveOverhang] whole.
 *
 * @param attachments Attachments prepared for upload, in the order they are shown.
 * @param onAttachmentClick Called when an attachment is opened.
 * @param onAttachmentRemoved Called when an attachment is removed from the selection.
 * @param contentInset Inset between the row's edges and the attachments inside it, and the one the
 * scrollbar's track ends at so that the two line up.
 * @param compact Whether the row is compact or on its way there.
 * @param compactness How far the row has animated from its regular layout, at 0, to its compact one, at 1.
 */
@Composable
private fun AttachmentPreviewRow(
    attachments: List<Attachment>,
    onAttachmentClick: (Attachment) -> Unit,
    onAttachmentRemoved: (Attachment) -> Unit,
    contentInset: Dp,
    compact: Boolean,
    compactness: Float,
) {
    val state = rememberLazyListState()
    val scrollbarState = rememberScrollbarState()
    val compactSide = space.attachmentUploadPreviewCompactSizeFraction.ofWindowShorterEdge()
    val itemSize = lerp(space.attachmentUploadPreviewSize, DpSize(compactSide, compactSide), compactness)

    Box(modifier = Modifier.fillMaxWidth()) {
        LazyRow(
            state = state,
            horizontalArrangement = Arrangement.spacedBy(space.attachmentUploadPreviewGap),
            contentPadding = PaddingValues(
                start = contentInset,
                end = contentInset,
                top = space.attachmentPreviewBarContentTopPadding,
                // The regular gap only makes up for the name's line box; with no name the design insets both ends
                // alike.
                bottom = lerp(
                    space.attachmentPreviewBarContentBottomPadding,
                    space.attachmentPreviewBarContentTopPadding,
                    compactness,
                ),
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("attachment_preview_row")
                .scrollbarBelowRow(
                    state = state,
                    scrollbarState = scrollbarState,
                    contentInset = contentInset,
                    // The floor exists for names growing with text scale, which the compact row does not show.
                    minHeight = lerp(space.attachmentPreviewBarMinHeight, 0.dp, compactness),
                ),
        ) {
            itemsIndexed(attachments, key = { _, item -> item.hashCode() }) { index, attachment ->
                val typeAndName = attachment.typeAndName
                val contentDescriptionText = if (attachments.size == 1) {
                    stringResource(string.content_description_attachment_single, typeAndName)
                } else {
                    stringResource(
                        id = string.content_description_attachment_item,
                        typeAndName,
                        index + 1,
                        attachments.size
                    )
                }
                AttachmentPreviewItem(
                    attachment = attachment,
                    onAttachmentClick = onAttachmentClick,
                    onAttachmentRemoved = onAttachmentRemoved,
                    modifier = Modifier.testTag("attachment_preview_item_$index"),
                    contentDescriptionText = contentDescriptionText,
                    size = itemSize,
                    showName = !compact,
                    // The compact tile scales with the size it is given, so it serves the whole transition and
                    // the small one, which has a fixed layout, returns only once the row is regular again.
                    thumbnailSize = if (compactness > 0f) ThumbnailSize.COMPACT else ThumbnailSize.SMALL,
                )
            }
        }
        // The list's own verdict: true while anything is left past the trailing edge, false on an empty list.
        OverflowEffect(Modifier.matchParentSize(), showOverflow = state.canScrollForward)
    }
}

/**
 * Floors the row at [minHeight] and draws a horizontal scrollbar in a strip reserved below it, its track
 * ending where the attachments do.
 *
 * Order is the whole trick, since `scrollbar` measures nothing and reserves no space of its own: `heightIn`
 * floors the content, the padding grows the node by [ScrollbarConfig.reservedThickness], and `scrollbar` —
 * outermost of the three — therefore draws over the grown node and centres its bar in that strip.
 *
 * The track is shortened to [contentInset] at each end rather than run the full viewport, so the bar starts
 * under the first attachment and the indicator reaches the last one's edge at the end of its travel. The
 * geometry takes that from the config's own main-axis padding, which is why this overrides the ends and
 * copies the theme's cross-axis inset — the one that centres the bar in the strip — through unchanged.
 */
@Composable
private fun Modifier.scrollbarBelowRow(
    state: LazyListState,
    scrollbarState: ScrollbarState,
    contentInset: Dp,
    minHeight: Dp,
): Modifier {
    val config = LocalScrollbarConfig.current
    val alignedToAttachments = remember(config, contentInset) {
        config.copy(
            padding = PaddingValues(
                start = contentInset,
                end = contentInset,
                top = config.padding.calculateTopPadding(),
                bottom = config.padding.calculateBottomPadding(),
            )
        )
    }
    return this
        .scrollbar(
            lazyListState = state,
            scrollbarState = scrollbarState,
            orientation = Orientation.Horizontal,
            config = alignedToAttachments,
        )
        .padding(bottom = config.reservedThickness)
        .heightIn(min = minHeight)
}

/**
 * A semitransparent gradient effect that has animated alpha transition when the [showOverflow] changes.
 *
 * Spans the attachments and stops above the reserved scrollbar strip, so it covers the content it stands
 * for at any text scale while leaving the scrollbar below them uncovered.
 *
 * @param modifier Modifier to be applied to the effect; pass `Modifier.matchParentSize()` so the fade
 * tracks the row's measured height without contributing to it.
 * @param showOverflow Whether the effect should be visible or not.
 */
@Composable
private fun OverflowEffect(modifier: Modifier = Modifier, showOverflow: Boolean) {
    val animatedAlpha = animateFloatAsState(
        animationSpec = tween(),
        targetValue = if (showOverflow) 1f else 0f
    )
    Box(
        modifier = modifier.padding(bottom = LocalScrollbarConfig.current.reservedThickness),
        contentAlignment = Alignment.TopEnd,
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(space.attachmentPreviewBarFadeWidth)
                .background(
                    brush = Brush.horizontalGradient(fadeToTrailingEdge()),
                    alpha = animatedAlpha.value
                )
                .testTag("overflow_effect")
        )
    }
}

/**
 * Gradient stops running from transparent to the background colour in the direction of the trailing edge.
 *
 * `Brush.horizontalGradient` runs left to right whatever the layout direction, while the trailing edge the
 * fade is aligned to follows it, so the stops have to be reversed to keep the opaque end against the edge.
 */
@Composable
private fun fadeToTrailingEdge(): List<Color> {
    val stops = listOf(Color.Transparent, chatColors.token.background.default)
    return if (LocalLayoutDirection.current == LayoutDirection.Rtl) stops.asReversed() else stops
}

@Composable
private fun AttachmentPreviewItem(
    attachment: Attachment,
    onAttachmentClick: (Attachment) -> Unit,
    onAttachmentRemoved: (Attachment) -> Unit,
    modifier: Modifier = Modifier,
    contentDescriptionText: String,
    size: DpSize,
    showName: Boolean,
    thumbnailSize: ThumbnailSize,
) {
    Box(
        contentAlignment = Alignment.TopEnd,
        modifier = modifier,
    ) {
        Item(onAttachmentClick, attachment, contentDescriptionText, size, showName, thumbnailSize)
        // Measured after the attachment, so a target larger than a compact tile never grows the item.
        CancelIcon(onAttachmentRemoved, attachment, contentDescriptionText, size.width, Modifier.matchParentSize())
    }
}

@Composable
private fun Item(
    onAttachmentClick: (Attachment) -> Unit,
    attachment: Attachment,
    contentDescriptionText: String,
    size: DpSize,
    showName: Boolean,
    thumbnailSize: ThumbnailSize,
) {
    val friendlyName = attachment.friendlyName
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(
                onClickLabel = stringResource(string.content_description_prepared_attachment, contentDescriptionText),
                onClick = remember(attachment) { { onAttachmentClick(attachment) } }
            )
            .semantics(true) {
                testTag = "preview_item_$friendlyName"
                contentDescription = contentDescriptionText
            }
    ) {
        AttachmentFramedSmallPreview(
            modifier = Modifier.size(size),
            attachment = attachment,
            isGroupAttachment = true,
            thumbnailSize = thumbnailSize,
        )
        AnimatedVisibility(visible = showName, enter = expandVertically(tween()), exit = shrinkVertically(tween())) {
            Text(
                text = friendlyName,
                style = typography.bodySmall,
                overflow = TextOverflow.MiddleEllipsis,
                maxLines = 1,
                modifier = Modifier
                    .padding(top = space.xSmall)
                    // Bound by the tile rather than a token, so a name on its way out narrows with it.
                    .widthIn(max = size.width - space.xSmall)
                    .semantics {
                        hideFromAccessibility() // The title is already part of the preview item's content description
                    }
            )
        }
    }
}

/**
 * The remove button: a [Space.attachmentUploadRemoveCircleSize] circle hanging past the attachment's top trailing
 * corner, inside a touch target [Space.attachmentUploadRemoveTouchTargetSize] tall.
 *
 * The circle already reaches over the gap to the next attachment, so a target centred on it would overlap that
 * attachment, which is drawn above it and would take the taps there. The target's trailing edge stops at the gap
 * instead, leaving the circle's outermost few dp outside it, and the rest of it lies over its own attachment. Its top is
 * level with the circle's, which is the top of the bar, since the input bar clips anything above that.
 *
 * Over its own attachment it stops [Space.attachmentUploadRemoveCentreClearance] short of the centre, where a tap opens
 * the attachment, so it is narrower than it is tall there. It still covers the circle on a tile too small for that.
 *
 * Given `Modifier.matchParentSize()`, the target is aligned within the attachment and reaches past it.
 */
@Composable
private fun CancelIcon(
    onAttachmentRemoved: (Attachment) -> Unit,
    attachment: Attachment,
    attachmentDescriptionText: String,
    tileWidth: Dp,
    modifier: Modifier,
) {
    val contentDescriptionText = stringResource(
        id = string.content_description_remove_prepared_attachment,
        attachmentDescriptionText
    )
    val overhang = space.attachmentUploadRemoveOverhang
    val gap = space.attachmentUploadPreviewGap
    val circleInsideTile = space.attachmentUploadRemoveCircleSize - overhang
    val reachInsideTile = (tileWidth / 2 - space.attachmentUploadRemoveCentreClearance).coerceAtLeast(circleInsideTile)
    val targetWidth = (gap + reachInsideTile).coerceAtMost(space.attachmentUploadRemoveTouchTargetSize)
    Box(
        modifier = modifier
            .zIndex(2f)
            .wrapContentSize(Alignment.TopEnd, unbounded = true)
            .offset(x = gap, y = -overhang)
            .size(width = targetWidth, height = space.attachmentUploadRemoveTouchTargetSize)
            .clickable(onClick = remember(attachment) { { onAttachmentRemoved(attachment) } })
            .semantics(true) {
                testTag = "preview_item_cancel_${attachment.friendlyName}"
                contentDescription = contentDescriptionText
            }
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .align(Alignment.TopEnd)
                // Puts the circle back where it hangs from the attachment's corner.
                .offset(x = overhang - gap)
                // A target narrower than the circle leaves it overflowing on the inward side.
                .wrapContentSize(Alignment.TopEnd, unbounded = true)
                .size(space.attachmentUploadRemoveCircleSize)
        ) {
            Image(
                imageVector = if (isSystemInDarkTheme()) ChatIcons.CancelDark else ChatIcons.Cancel,
                contentDescription = null, // Decorative - description moved to parent
            )
        }
    }
}

/** The last spec is wide enough for [wideScreenHorizontalPadding] to inset the attachments. */
@Suppress("VisualLintBounds") // Manual offset for cancel icon is causing issues
@PreviewLightDark
@Preview(device = "spec:width=852dp,height=412dp")
@Composable
private fun AttachmentPreviewBarPreview() {
    var showAttachment: Boolean by remember { mutableStateOf(true) }
    val choices = PreviewAttachments.getAttachments(20).toList()
    val attachments = remember { choices.toMutableStateList() }
    ChatTheme {
        Surface(color = colorScheme.background, modifier = Modifier.systemBarsPadding()) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(space.medium),
                    modifier = Modifier
                        .padding(space.medium)
                        .semantics(true) {
                            hideFromAccessibility() // For preview purposes only
                        }
                ) {
                    Switch(
                        modifier = Modifier.semantics {
                            hideFromAccessibility() // For preview purposes only
                        },
                        checked = showAttachment,
                        onCheckedChange = {
                            showAttachment = it
                            if (it) {
                                attachments.addAll(choices)
                            } else {
                                attachments.clear()
                            }
                        }
                    )
                    Text(
                        text = "Show attachment preview",
                        style = typography.labelLarge,
                        modifier = Modifier.semantics {
                            hideFromAccessibility() // For preview purposes only
                        }
                    )
                }
                AttachmentPreviewBar(
                    attachments = attachments,
                    onAttachmentClick = {},
                    onAttachmentRemoved = {
                        attachments.remove(it)
                        if (attachments.isEmpty()) {
                            showAttachment = false
                        }
                    },
                    horizontalPadding = wideScreenHorizontalPadding(),
                )
            }
        }
    }
}
