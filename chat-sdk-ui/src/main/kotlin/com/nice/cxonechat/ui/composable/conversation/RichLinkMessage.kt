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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize.Max
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nice.cxonechat.message.Media
import com.nice.cxonechat.ui.R
import com.nice.cxonechat.ui.R.string
import com.nice.cxonechat.ui.composable.conversation.model.Message.RichLink
import com.nice.cxonechat.ui.composable.generic.AutoLinkedText
import com.nice.cxonechat.ui.composable.generic.PresetAsyncImage
import com.nice.cxonechat.ui.composable.theme.ChatColors.ColorPair
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import com.nice.cxonechat.ui.composable.theme.ChatTheme.chatTypography
import com.nice.cxonechat.ui.composable.theme.ChatTheme.colorScheme
import com.nice.cxonechat.ui.composable.theme.ChatTheme.space
import com.nice.cxonechat.ui.composable.theme.isLandscapeWindow
import com.nice.cxonechat.ui.util.openWithAndroid
import com.nice.cxonechat.ui.util.preview.message.UiSdkRichLink

/**
 * Derives the height budget from the window the chat is drawn into, not from [android.content.res.Configuration]:
 * in split-screen / multi-window the device can be landscape while this window is tall and narrow,
 * and `Configuration.screenHeightDp` is the display height, not this window's. This matches
 * `ChatConversation.kt`, `MessageFrame.kt` and `ThreadViewTopBar.kt`, which all read layout from
 * `LocalWindowInfo.current.containerSize`.
 *
 * The card lives in a vertically-scrolling list, so [BoxWithConstraints.maxHeight] is effectively
 * infinite there and can't bound the card's height in either orientation. The real window height
 * is used instead, for both the landscape 60% cap and the portrait 1/3 cap. The width is capped once,
 * by the message bubble around the card.
 */
@Composable
internal fun RichLinkMessage(
    message: RichLink,
    textColor: ColorPair,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    // Window height, not Configuration; see function KDoc.
    val isLandscape = isLandscapeWindow()
    val containerSize = LocalWindowInfo.current.containerSize
    val windowHeightDp = with(LocalDensity.current) { containerSize.height.toDp() }
    val maxHeight = if (isLandscape) {
        windowHeightDp.times(space.richLinkLandscapeMaxHeightFraction)
    } else {
        windowHeightDp.div(3)
    }
    BoxWithConstraints {
        // The bubble around the card already caps its width (Space.messageBubbleMaxWidthFraction),
        // so the card takes what it is given instead of applying a second fraction.
        val maxWidth = this.maxWidth
        val url = message.url
        val onClickLabel = stringResource(string.on_click_label_url_link)
        Column(
            modifier = modifier
                .testTag("rich_link_message")
                .clickable(
                    onClickLabel = onClickLabel
                ) {
                    context.openWithAndroid(url = url, mimeType = null)
                }
                .width(Max)
                // Keep the whole message (image + title + link) within the height cap in both
                // orientations; RichLinkMedia leaves the title/link their needed space via weight(fill = false).
                .heightIn(max = maxHeight)
                .semantics {
                    isTraversalGroup = true
                },
            horizontalAlignment = Alignment.Start,
        ) {
            RichLinkMedia(message.media, maxWidth, maxHeight, message.title, isLandscape)
            RichLinkTitle(message.title, textColor)
            RichLinkUrl(url)
        }
    }
}

/**
 * Classifies a loaded image by its intrinsic dimensions for the landscape rich-link layout.
 *
 * A *tall* image (height greater than width) fills the card width so it shows no side gutters; a
 * *wide* or square image keeps its natural size. Extracted from the layout so the tall/wide contract
 * that drives [RichLinkMedia] can be asserted directly, independent of the rendered pixels.
 */
internal fun isTallImage(width: Int, height: Int): Boolean = height > width

/**
 * Whether an image of [imageWidth] x [imageHeight] would overflow [maxHeight] if scaled to fill
 * [maxWidth] while preserving its aspect ratio.
 *
 * Drives the portrait rich-link crop decision: below the cap the image keeps [ContentScale.FillWidth]
 * so the card stays content-driven; at or above it, [ContentScale.Crop] fills the remaining height
 * instead of letting `FillWidth`'s uncropped overflow bleed past the card.
 */
internal fun exceedsHeightCap(imageWidth: Int, imageHeight: Int, maxWidth: Dp, maxHeight: Dp): Boolean =
    imageHeight.toFloat() / imageWidth > maxHeight.value / maxWidth.value

@Composable
private fun ColumnScope.RichLinkMedia(media: Media, maxWidth: Dp, maxHeight: Dp, description: String, isLandscape: Boolean) {
    val imageModel: String? = media.url.ifBlank { null }
    val imageContentDescription: String? = imageModel?.let {
        stringResource(string.content_description_rich_link_image, description)
    }
    if (isLandscape) {
        LandscapeRichLinkMedia(imageModel, imageContentDescription, maxHeight)
    } else {
        PortraitRichLinkMedia(imageModel, imageContentDescription, maxWidth, maxHeight)
    }
}

/**
 * Landscape media: while loading, the box is floored at `space.richLinkMediaMinSize` in both
 * dimensions so the placeholder isn't a tall sliver under a short title. Once loaded, the box
 * always fills the card width — matching [PortraitRichLinkMedia] — instead of keeping the image's
 * own intrinsic width, so it can't end up centered narrower than the title/URL-driven card width
 * and leave gutters on both sides:
 *  - A TALL image uses FillWidth, filling the card width with its vertical overflow clipped by
 *    the height cap — no gutters, no distortion.
 *  - A WIDE/small image uses Fit inside that same full-width box; Coil scales it up to the card
 *    width while preserving its aspect ratio (verified against the small-image screenshot
 *    goldens — Fit is not limited to downscaling here).
 * A short title paired with a small image can therefore end up with a narrower final card than
 * while the width floor was still active post-load — intentional, to avoid the gutters, and
 * consistent with [PortraitRichLinkMedia] never applying a post-load width floor either.
 * The intrinsic size is only known once the image loads, so we read it back from
 * PresetAsyncImage and default to the wide/Fit path until then.
 *
 * `weight(fill = false)` gives the box only the space left after the title/URL are measured —
 * capped at [maxHeight] — so a large image can't push them out, and a small image lets the box
 * (and card) wrap to its natural height instead of stretching.
 */
@Composable
private fun ColumnScope.LandscapeRichLinkMedia(imageModel: String?, imageContentDescription: String?, maxHeight: Dp) {
    var useFillWidth by remember(imageModel) { mutableStateOf(false) }
    // Both floors are loading placeholders only, cleared once the real size is known — see KDoc.
    var isLoaded by remember(imageModel) { mutableStateOf(false) }
    val minHeight = if (isLoaded) 0.dp else space.richLinkMediaMinSize
    Box(
        modifier = Modifier
            // Matters only while loading (floor-sized box) — see KDoc.
            .align(Alignment.CenterHorizontally)
            // See KDoc.
            .weight(weight = 1f, fill = false)
            // Loading: floor width. Loaded: fill card width — see KDoc.
            .then(if (isLoaded) Modifier.fillMaxWidth() else Modifier.widthIn(min = space.richLinkMediaMinSize))
            .heightIn(min = minHeight, max = maxHeight)
            // The tall path relies on the height cap clipping the FillWidth overflow; make that
            // clipping explicit so it doesn't depend on the surrounding layout's default bounds clip.
            .clipToBounds(),
        contentAlignment = Alignment.Center,
    ) {
        PresetAsyncImage(
            model = imageModel,
            contentDescription = imageContentDescription,
            // Tall: FillWidth + height-cap clip. Wide/small: Fit, scaled up to the card width — see KDoc.
            contentScale = if (useFillWidth) ContentScale.FillWidth else ContentScale.Fit,
            isGroupAttachment = false,
            showLoadingBorder = true,
            modifier = Modifier
                .testTag("rich_link_image")
                .then(if (isLoaded) Modifier.fillMaxWidth() else Modifier.widthIn(min = space.richLinkMediaMinSize))
                .heightIn(min = minHeight, max = maxHeight),
            onImageLoaded = { width, height ->
                isLoaded = true
                useFillWidth = isTallImage(width, height)
            },
        )
    }
}

/**
 * Portrait media: below the height cap, FillWidth lets the image (and card) follow its own aspect
 * ratio so a small image doesn't force extra empty space. An image whose aspect ratio would overflow
 * the cap switches to Crop so it fills the remaining height instead of the card growing past its
 * maximum. The intrinsic size is only known once the image loads, so this defaults to the uncropped
 * FillWidth path until then, matching the landscape tall/wide switch.
 *
 * The 236dp floor is only a loading placeholder, cleared once the real (possibly smaller) image
 * size is known, so a small image isn't padded out with empty space before the title.
 */
@Composable
private fun ColumnScope.PortraitRichLinkMedia(imageModel: String?, imageContentDescription: String?, maxWidth: Dp, maxHeight: Dp) {
    var useCrop by remember(imageModel) { mutableStateOf(false) }
    // Floor only while loading; real size wins once loaded — see KDoc.
    var isLoaded by remember(imageModel) { mutableStateOf(false) }
    val minHeight = if (isLoaded) 0.dp else space.richLinkMediaMinSize
    Box(
        modifier = Modifier
            .align(Alignment.CenterHorizontally)
            // weight(fill = false) gives the image only the space left after the title/URL are
            // measured, so it can't push the card past maxHeight; a small image lets the card wrap
            // to its own height instead of stretching to fill it.
            .weight(weight = 1f, fill = false)
            .fillMaxWidth()
            .heightIn(min = minHeight, max = maxHeight)
            // The Crop path relies on the height cap clipping overflow beyond the visible area.
            .clipToBounds(),
        contentAlignment = Alignment.Center,
    ) {
        PresetAsyncImage(
            model = imageModel,
            contentDescription = imageContentDescription,
            contentScale = if (useCrop) ContentScale.Crop else ContentScale.FillWidth,
            isGroupAttachment = false,
            showLoadingBorder = true,
            modifier = Modifier
                .testTag("rich_link_image")
                .fillMaxWidth()
                .heightIn(min = minHeight, max = maxHeight),
            onImageLoaded = { width, height ->
                isLoaded = true
                useCrop = exceedsHeightCap(width, height, maxWidth, maxHeight)
            },
        )
    }
}

@Composable
private fun RichLinkTitle(title: String, textColor: ColorPair) {
    Text(
        text = title,
        style = chatTypography.chatCardTitle,
        color = textColor.foreground,
        modifier = Modifier.padding(start = space.semiLarge, top = space.medium, end = space.semiLarge)
    )
}

@Composable
private fun RichLinkUrl(url: String) {
    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier
            .padding(start = space.semiLarge, top = 3.dp, bottom = space.semiLarge, end = space.semiLarge)
    ) {
        val linkColor = colorScheme.primary
        AutoLinkedText(
            text = url,
            style = chatTypography.chatCardLink,
            linkColor = linkColor,
            linkDecoration = TextDecoration.None,
        )
        Icon(
            painterResource(R.drawable.ic_link),
            contentDescription = null, // Decorative icon
            tint = linkColor,
            modifier = Modifier
                .testTag("rich_link_icon")
                .padding(start = 2.dp, bottom = 2.dp)
                .size(width = 12.dp, height = 11.dp)
        )
    }
}

@PreviewLightDark
@PreviewScreenSizes
@Composable
private fun PreviewMessageRichLink() {
    ChatTheme {
        Box(
            Modifier
                .fillMaxHeight(0.8f)
        ) {
            Column {
                PreviewMessageItemBase(
                    message = RichLink(UiSdkRichLink()),
                )
            }
        }
    }
}
