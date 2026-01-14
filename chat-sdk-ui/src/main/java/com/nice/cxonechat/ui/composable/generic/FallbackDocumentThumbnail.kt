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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible

@Composable
internal fun FallbackThumbnail(
    uri: String,
    modifier: Modifier = Modifier,
    mimeType: String? = null,
    thumbnailSize: ThumbnailSize = ThumbnailSize.LARGE,
) {
    val extension by getFileExtension(mimeType, uri)
    when (thumbnailSize) {
        ThumbnailSize.LARGE -> FallbackThumbnailLargeContent(extension, modifier)
        ThumbnailSize.REGULAR -> FallbackThumbnailContent(extension, modifier)
        ThumbnailSize.SMALL -> FallbackThumbnailSmallContent(extension, modifier)
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
}

@Composable
private fun FallbackThumbnailLargeContent(
    extension: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.size(space.attachmentPreviewFallbackLargeSize),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = ChatIcons.DocumentLarge,
            contentDescription = stringResource(string.content_description_document_preview, extension),
            tint = colorScheme.contentColorFor(colorScheme.primary),
            modifier = Modifier
        )
        Text(
            text = extension,
            color = colorScheme.contentColorFor(colorScheme.primary),
            modifier = Modifier
                .align(Alignment.Center)
                .background(color = colorScheme.primary, shape = chatShapes.documentTypeLabelShape)
                .padding(horizontal = 8.dp, vertical = 2.dp),
            style = chatTypography.documentFallackText,
        )
    }
}

@Composable
private fun FallbackThumbnailContent(
    extension: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = ChatIcons.Document,
            contentDescription = stringResource(string.content_description_document_preview, extension),
            tint = colorScheme.contentColorFor(colorScheme.primary),
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
    modifier: Modifier = Modifier,
) {
    val iconMod = Modifier.size(space.attachmentPreviewFallbackSmallSize)
    if (extension.isEmpty()) {
        Icon(
            imageVector = ChatIcons.Document,
            contentDescription = "Preview thumbnail for a file",
            modifier = iconMod
        )
    } else {
        Box(
            contentAlignment = Alignment.Center,
            modifier = modifier.padding(space.attachmentPreviewPaddingValues)
        ) {
            Icon(
                imageVector = ChatIcons.Document,
                contentDescription = stringResource(string.content_description_document_preview, extension),
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
                FallbackThumbnailContent("TORRRRRRENT")
                HorizontalDivider()
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .border(width = 1.dp, color = chatColors.token.border.default, shape = chatShapes.smallSelectionFrame)
                        .size(space.attachmentUploadPreviewSize)
                        .background(color = chatColors.token.background.default, shape = chatShapes.smallSelectionFrame)
                ) {
                    FallbackThumbnailSmallContent("TORRENT")
                }
                HorizontalDivider()
                FallbackThumbnailLargeContent("TXT")
            }
        }
    }
}
