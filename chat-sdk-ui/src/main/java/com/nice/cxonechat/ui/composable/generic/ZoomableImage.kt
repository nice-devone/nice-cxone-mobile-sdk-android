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

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import net.engawapg.lib.zoomable.rememberZoomState
import net.engawapg.lib.zoomable.zoomable

/**
 * A [PresetAsyncImage] with applied [zoomable] modifier.
 *
 * @param image A model for [PresetAsyncImage].
 * @param modifier A [Modifier] for [PresetAsyncImage].
 * @param contentDescription A contentDescription for [PresetAsyncImage].
 */
@Composable
internal fun ZoomableImage(
    image: Any?,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    PresetAsyncImage(
        model = image,
        contentDescription = contentDescription,
        modifier = modifier.zoomable(rememberZoomState()),
    )
}
