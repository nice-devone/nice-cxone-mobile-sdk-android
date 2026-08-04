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

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons.Outlined
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade

internal data class AsyncImagePainters(
    val fallback: Painter,
    val error: Painter,
)

@Composable
internal fun asyncImagePainters(
    fallback: Painter = rememberVectorPainter(image = Outlined.Description),
    error: Painter = rememberVectorPainter(image = Outlined.ErrorOutline),
) = AsyncImagePainters(
    fallback = fallback,
    error = error
)

@Composable
internal fun PresetAsyncImage(
    model: Any?,
    modifier: Modifier = Modifier,
    contentDescription: String?,
    cacheKey: String? = null,
    isGroupAttachment: Boolean,
    showLoadingBorder: Boolean = true,
    alignment: Alignment = Alignment.Center,
    contentScale: ContentScale = ContentScale.Fit,
    painters: AsyncImagePainters = asyncImagePainters(),
) {
    val tint = ColorFilter.tint(LocalContentColor.current)
    var imageLoaded by remember(model) { mutableStateOf(false) }
    val fallback = remember(tint, painters.fallback) {
        forwardingPainter(
            painter = painters.fallback,
            colorFilter = tint
        )
    }
    val error = remember(tint, painters.error) {
        forwardingPainter(
            painter = painters.error,
            colorFilter = tint
        )
    }
    Box(contentAlignment = Alignment.Center) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(model)
                .crossfade(true)
                .apply {
                    cacheKey?.let {
                        diskCacheKey(it)
                        memoryCacheKey(it)
                    }
                }
                .build(),
            contentDescription = contentDescription,
            fallback = fallback,
            error = error,
            onSuccess = { imageLoaded = true },
            modifier = modifier,
            alignment = alignment,
            contentScale = contentScale,
        )
        if (!imageLoaded) {
            LoadingSpinner(
                modifier = Modifier.matchParentSize(),
                isGroupAttachment = isGroupAttachment,
                showLoadingBorder = showLoadingBorder
            )
        }
    }
}

@Preview
@Composable
private fun PresetAsyncImagePreview() {
    PresetAsyncImage(
        model = "https://www.nice.com/favicon.ico",
        contentDescription = "Preset Async Image",
        isGroupAttachment = false
    )
}
