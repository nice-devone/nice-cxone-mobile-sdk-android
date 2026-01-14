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

package com.nice.cxonechat.ui.composable.icons.outlined

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType.Companion.NonZero
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap.Companion.Round
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.ImageVector.Builder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nice.cxonechat.ui.composable.icons.ChatIcons

internal val ChatIcons.VideoAdd: ImageVector
    get() {
        if (_videoAdd != null) {
            return _videoAdd!!
        }
        _videoAdd = Builder(
            name = "Video-add",
            defaultWidth = 32.0.dp,
            defaultHeight = 33.0.dp,
            viewportWidth = 32.0f,
            viewportHeight = 33.0f
        ).apply {
            path(
                fill = SolidColor(Color(0x00000000)),
                stroke = SolidColor(Color(0xFF666A76)),
                strokeLineWidth = 1.5f,
                strokeLineCap = Round,
                strokeLineJoin = StrokeJoin.Round,
                strokeLineMiter = 4.0f,
                pathFillType = NonZero
            ) {
                moveTo(17.333f, 27.167f)
                horizontalLineTo(8.0f)
                curveTo(6.527f, 27.167f, 5.333f, 25.973f, 5.333f, 24.5f)
                verticalLineTo(8.5f)
                curveTo(5.333f, 7.027f, 6.527f, 5.833f, 8.0f, 5.833f)
                horizontalLineTo(24.0f)
                curveTo(25.473f, 5.833f, 26.667f, 7.027f, 26.667f, 8.5f)
                verticalLineTo(17.833f)
                moveTo(20.229f, 23.948f)
                horizontalLineTo(26.667f)
                moveTo(23.448f, 27.167f)
                verticalLineTo(20.729f)
                moveTo(19.333f, 15.345f)
                lineTo(14.0f, 12.266f)
                curveTo(13.111f, 11.753f, 12.0f, 12.394f, 12.0f, 13.421f)
                verticalLineTo(19.579f)
                curveTo(12.0f, 20.606f, 13.111f, 21.247f, 14.0f, 20.734f)
                lineTo(19.333f, 17.655f)
                curveTo(20.222f, 17.142f, 20.222f, 15.858f, 19.333f, 15.345f)
                close()
            }
        }.build()
        return _videoAdd!!
    }

private var _videoAdd: ImageVector? = null

@Preview
@Composable
private fun PreviewVideoAdd() {
    Box(modifier = Modifier.padding(12.dp)) {
        Image(imageVector = ChatIcons.VideoAdd, contentDescription = "")
    }
}
