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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import com.nice.cxonechat.ui.R
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
    val zoomHint = stringResource(R.string.content_description_zoomable_image_hint)
    PresetAsyncImage(
        model = image,
        contentDescription = contentDescription,
        isGroupAttachment = false,
        showLoadingBorder = false, // Added to remove loading border for full screen image
        modifier = modifier
            .semantics { stateDescription = zoomHint }
            .zoomable(rememberZoomState()),
    )
}
