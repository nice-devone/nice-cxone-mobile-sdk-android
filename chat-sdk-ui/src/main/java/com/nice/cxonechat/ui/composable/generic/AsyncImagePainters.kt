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

package com.nice.cxonechat.ui.composable.generic

import androidx.compose.material.LocalContentColor
import androidx.compose.material.icons.Icons.Outlined
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Downloading
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage

internal data class AsyncImagePainters(
    val placeholder: Painter,
    val fallback: Painter,
    val error: Painter,
)

@Composable
internal fun asyncImagePainters(
    placeholder: Painter = rememberVectorPainter(image = Outlined.Downloading),
    fallback: Painter = rememberVectorPainter(image = Outlined.Description),
    error: Painter = rememberVectorPainter(image = Outlined.ErrorOutline),
) = AsyncImagePainters(
    placeholder = placeholder,
    fallback = fallback,
    error = error
)

@Composable
internal fun PresetAsyncImage(
    model: Any?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    alignment: Alignment = Alignment.Center,
    contentScale: ContentScale = ContentScale.Fit,
    painters: AsyncImagePainters = asyncImagePainters()
) {
    val tint = ColorFilter.tint(LocalContentColor.current)
    val placeholder = forwardingPainter(
        painter = painters.placeholder,
        colorFilter = tint
    )
    val fallback = forwardingPainter(
        painter = painters.fallback,
        colorFilter = tint
    )
    val error = forwardingPainter(
        painter = painters.error,
        colorFilter = tint
    )
    AsyncImage(
        model = model,
        contentDescription = contentDescription,
        placeholder = placeholder,
        fallback = fallback,
        error = error,
        modifier = modifier,
        alignment = alignment,
        contentScale = contentScale,
    )
}
