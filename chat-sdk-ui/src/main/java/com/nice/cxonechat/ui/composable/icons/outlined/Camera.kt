/*
 * Copyright (c) 2021-2025. NICE Ltd. All rights reserved.
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

internal val ChatIcons.Camera: ImageVector
    get() {
        if (_camera != null) {
            return _camera!!
        }
        _camera = Builder(
            name = "Camera",
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
                moveTo(26.667f, 21.155f)
                curveTo(26.667f, 23.364f, 24.876f, 25.155f, 22.667f, 25.155f)
                horizontalLineTo(9.333f)
                curveTo(7.124f, 25.155f, 5.333f, 23.364f, 5.333f, 21.155f)
                lineTo(5.333f, 13.276f)
                curveTo(5.333f, 11.803f, 6.527f, 10.609f, 8.0f, 10.609f)
                horizontalLineTo(9.724f)
                curveTo(10.616f, 10.609f, 11.449f, 10.163f, 11.943f, 9.422f)
                lineTo(12.299f, 8.887f)
                curveTo(12.794f, 8.146f, 13.626f, 7.7f, 14.518f, 7.7f)
                horizontalLineTo(17.482f)
                curveTo(18.374f, 7.7f, 19.206f, 8.146f, 19.701f, 8.887f)
                lineTo(20.057f, 9.422f)
                curveTo(20.551f, 10.163f, 21.384f, 10.609f, 22.276f, 10.609f)
                horizontalLineTo(24.0f)
                curveTo(25.473f, 10.609f, 26.667f, 11.803f, 26.667f, 13.276f)
                verticalLineTo(21.155f)
                close()
            }
            path(
                fill = SolidColor(Color(0x00000000)),
                stroke = SolidColor(Color(0xFF666A76)),
                strokeLineWidth = 1.5f,
                strokeLineCap = Round,
                strokeLineJoin = StrokeJoin.Round,
                strokeLineMiter = 4.0f,
                pathFillType = NonZero
            ) {
                moveTo(16.0f, 21.276f)
                curveTo(18.142f, 21.276f, 19.879f, 19.539f, 19.879f, 17.397f)
                curveTo(19.879f, 15.255f, 18.142f, 13.518f, 16.0f, 13.518f)
                curveTo(13.858f, 13.518f, 12.121f, 15.255f, 12.121f, 17.397f)
                curveTo(12.121f, 19.539f, 13.858f, 21.276f, 16.0f, 21.276f)
                close()
            }
        }.build()
        return _camera!!
    }

private var _camera: ImageVector? = null

@Preview
@Composable
private fun PreviewCamera() {
    Box(modifier = Modifier.padding(12.dp)) {
        Image(imageVector = ChatIcons.Camera, contentDescription = "")
    }
}
