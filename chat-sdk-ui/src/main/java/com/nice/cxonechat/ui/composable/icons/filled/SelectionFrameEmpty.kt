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

package com.nice.cxonechat.ui.composable.icons.filled

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType.Companion.NonZero
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap.Companion.Butt
import androidx.compose.ui.graphics.StrokeJoin.Companion.Miter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.ImageVector.Builder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nice.cxonechat.ui.composable.icons.ChatIcons

internal val ChatIcons.SelectionFrameEmpty: ImageVector
    get() {
        if (_selection_frame_empty != null) {
            return _selection_frame_empty!!
        }
        _selection_frame_empty = Builder(
            name = "Selection frame empty",
            defaultWidth = 32.0.dp,
            defaultHeight = 31.0.dp,
            viewportWidth = 32.0f,
            viewportHeight = 31.0f
        ).apply {
            path(
                fill = SolidColor(Color(0x00000000)),
                stroke = SolidColor(Color(0xFFD4D5D8)),
                strokeLineWidth = 3.0f,
                strokeLineCap = Butt,
                strokeLineJoin = Miter,
                strokeLineMiter = 4.0f,
                pathFillType = NonZero
            ) {
                moveTo(16.224f, 2.017f)
                curveTo(23.89f, 2.017f, 30.146f, 8.137f, 30.146f, 15.738f)
                curveTo(30.146f, 23.338f, 23.89f, 29.457f, 16.224f, 29.457f)
                curveTo(8.557f, 29.457f, 2.3f, 23.338f, 2.3f, 15.738f)
                curveTo(2.3f, 8.137f, 8.557f, 2.017f, 16.224f, 2.017f)
                close()
            }
        }.build()
        return _selection_frame_empty!!
    }

private var _selection_frame_empty: ImageVector? = null

@Preview
@Composable
private fun Preview() {
    Box(modifier = Modifier.padding(12.dp)) {
        Image(imageVector = ChatIcons.SelectionFrameEmpty, contentDescription = "")
    }
}
