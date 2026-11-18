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

import android.webkit.MimeTypeMap
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import com.nice.cxonechat.ui.R.string
import com.nice.cxonechat.ui.composable.icons.ChatIcons
import com.nice.cxonechat.ui.composable.icons.notint.Document
import com.nice.cxonechat.ui.composable.icons.notint.DocumentLarge
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import com.nice.cxonechat.ui.composable.theme.ChatTheme.chatColors
import com.nice.cxonechat.ui.composable.theme.ChatTheme.chatShapes
import com.nice.cxonechat.ui.composable.theme.ChatTheme.chatTypography
import com.nice.cxonechat.ui.composable.theme.ChatTheme.colorScheme
import com.nice.cxonechat.ui.composable.theme.ChatTheme.space
import com.nice.cxonechat.ui.composable.theme.LocalMessageColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible

@Composable
internal fun FallbackThumbnail(
    uri: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    mimeType: String? = null,
    thumbnailSize: ThumbnailSize = ThumbnailSize.LARGE,
) {
    val extension by getFileExtension(mimeType, uri)
    when (thumbnailSize) {
        ThumbnailSize.LARGE -> FallbackThumbnailLargeContent(extension, contentDescription, modifier)
        ThumbnailSize.REGULAR -> FallbackThumbnailContent(extension, contentDescription, modifier)
        ThumbnailSize.SMALL -> FallbackThumbnailSmallContent(extension, contentDescription, modifier)
        ThumbnailSize.COMPACT -> FallbackThumbnailCompactTile(extension, contentDescription, modifier)
    }
}

@Composable
private fun getFileExtension(mimeType: String?, uri: String): State<String> {
    val context = LocalContext.current
    return produceState(stringResource(string.fallback_document_initial_extension)) {
        runInterruptible(Dispatchers.IO) {
            val ext = (mimeType ?: context.contentResolver.getType(uri.toUri()))
                ?.let { MimeTypeMap.getSingleton().getExtensionFromMimeType(it) }
                ?: MimeTypeMap.getFileExtensionFromUrl(uri)
            value = ext.uppercase(Locale.current.platformLocale)
        }
    }
}

/**
 * Defines the layout & size of the produced thumbnail.
 */
internal enum class ThumbnailSize {
    /** The thumbnail is suitable for display for detailed list. */
    LARGE,

    /** The thumbnail is suitable for display as an icon. */
    REGULAR,

    /** Small thumbnail displays only the extension. */
    SMALL,

    /** A smaller [SMALL], sized by the slot it is given rather than by a fixed token. */
    COMPACT,
}

@Composable
private fun FallbackThumbnailLargeContent(
    extension: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.size(space.attachmentPreviewFallbackLargeSize),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = ChatIcons.DocumentLarge,
            contentDescription = contentDescription,
            // The surface tone the design draws this tile in, and deliberately not the sender's colour: the
            // size lands on the picker's grid, which has no message, and on a PDF still rendering its first
            // page, which may have one. Its low contrast on the picker sheet is the design's own call.
            tint = chatColors.token.background.surface.variant,
        )
        Text(
            text = extension,
            color = colorScheme.contentColorFor(colorScheme.primary),
            modifier = Modifier
                .align(Alignment.Center)
                .background(color = colorScheme.primary, shape = chatShapes.documentTypeLabelShape)
                .padding(horizontal = 8.dp, vertical = 2.dp),
        )
    }
}

@Composable
private fun FallbackThumbnailContent(
    extension: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
) {
    // The design draws the tile's frame and glyph in one tone, whichever bubble sits behind them.
    val tileForeground = chatColors.token.brand.onPrimary
    Box(
        modifier = modifier
            // The backdrop the message frame leaves to the tile when a lone attachment stands in for the bubble.
            .background(
                color = LocalMessageColors.current?.background ?: Color.Transparent,
                shape = chatShapes.selectionFrame,
            )
            .border(width = 1.dp, color = tileForeground, shape = chatShapes.selectionFrame),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = ChatIcons.Document,
            contentDescription = contentDescription,
            tint = tileForeground,
            modifier = Modifier.padding(horizontal = 22.dp, vertical = 12.dp)
        )
        if (extension.isNotEmpty()) {
            Text(
                text = extension,
                color = colorScheme.contentColorFor(colorScheme.primary),
                modifier = Modifier
                    .align(Alignment.Center)
                    .background(color = colorScheme.primary, shape = chatShapes.documentTypeLabelShape)
                    .padding(horizontal = 8.dp, vertical = 2.dp),
                softWrap = false,
                maxLines = 1,
                style = chatTypography.documentFallackTextSmall,
            )
        }
    }
}

@Composable
private fun FallbackThumbnailSmallContent(
    extension: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
) {
    val iconMod = Modifier.size(space.attachmentPreviewFallbackSmallSize)
    if (extension.isEmpty()) {
        Icon(
            imageVector = ChatIcons.Document,
            contentDescription = contentDescription,
            modifier = iconMod
        )
    } else {
        Box(
            contentAlignment = Alignment.Center,
            modifier = modifier.padding(space.attachmentPreviewPaddingValues)
        ) {
            Icon(
                imageVector = ChatIcons.Document,
                contentDescription = contentDescription,
                tint = chatColors.token.background.surface.variant,
                modifier = iconMod
            )
            Row(
                modifier = Modifier
                    .height(space.attachmentPreviewFallbackSmallSizeLabelHeight)
                    .background(color = colorScheme.primary, shape = chatShapes.documentTypeLabelShape)
                    .padding(horizontal = space.small),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = extension,
                    color = colorScheme.contentColorFor(colorScheme.primary),
                    softWrap = false,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = chatTypography.documentFallackTextTiny,
                )
            }
        }
    }
}

/**
 * The document glyph and extension label scaled to fill whatever slot they are given.
 *
 * The label has a minimum width rather than a fixed one, so a long extension widens it instead of being cut off.
 * Only an extension too long for the slot even then has its text shrunk, down to [COMPACT_LABEL_MIN_FONT_SIZE], so
 * the usual ones keep the design's size.
 */
@Composable
private fun FallbackThumbnailCompactTile(
    extension: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        contentAlignment = Alignment.Center,
        modifier = modifier.fillMaxSize(),
    ) {
        Icon(
            imageVector = ChatIcons.Document,
            contentDescription = contentDescription,
            tint = chatColors.token.background.surface.variant,
            modifier = Modifier
                .fillMaxWidth(space.attachmentPreviewFallbackCompactGlyphWidthFraction)
                .fillMaxHeight(space.attachmentPreviewFallbackCompactGlyphHeightFraction),
        )
        if (extension.isNotEmpty()) {
            // Centred, because shrunk text keeps the full line height and the default alignment leaves the slack
            // below it.
            val style = chatTypography.documentFallackTextSmall.copy(
                lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None),
            )
            Text(
                text = extension,
                color = colorScheme.contentColorFor(colorScheme.primary),
                softWrap = false,
                maxLines = 1,
                style = style,
                autoSize = TextAutoSize.StepBased(
                    minFontSize = COMPACT_LABEL_MIN_FONT_SIZE,
                    maxFontSize = style.fontSize,
                    stepSize = COMPACT_LABEL_FONT_SIZE_STEP,
                ),
                modifier = Modifier
                    .height(space.attachmentPreviewFallbackSmallSizeLabelHeight)
                    .widthIn(min = maxWidth * space.attachmentPreviewFallbackCompactLabelMinWidthFraction, max = maxWidth)
                    .background(color = colorScheme.primary, shape = chatShapes.documentTypeLabelShape)
                    .padding(horizontal = space.small),
            )
        }
    }
}

/** Smallest text the compact tile's extension label shrinks to before a very long extension is cut off. */
private val COMPACT_LABEL_MIN_FONT_SIZE = 8.sp

private val COMPACT_LABEL_FONT_SIZE_STEP = 0.5.sp

@Composable
@PreviewLightDark
private fun FallbackContentPreview() {
    ChatTheme {
        Surface {
            Column(
                Modifier
                    .wrapContentWidth()
                    .width(IntrinsicSize.Min)
            ) {
                FallbackThumbnailContent(
                    extension = "TORRRRRRENT",
                    contentDescription = null
                )
                HorizontalDivider()
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .border(width = 1.dp, color = chatColors.token.border.default, shape = chatShapes.smallSelectionFrame)
                        .size(space.attachmentUploadPreviewSize)
                        .background(color = chatColors.token.background.default, shape = chatShapes.smallSelectionFrame)
                ) {
                    FallbackThumbnailSmallContent(
                        extension = "TORRENT",
                        contentDescription = null
                    )
                }
                HorizontalDivider()
                FallbackThumbnailLargeContent(
                    extension = "TXT",
                    contentDescription = null
                )
            }
        }
    }
}
