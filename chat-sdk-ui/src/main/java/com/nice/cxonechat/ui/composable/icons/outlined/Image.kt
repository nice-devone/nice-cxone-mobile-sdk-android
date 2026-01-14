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
import androidx.compose.ui.graphics.StrokeCap.Companion.Butt
import androidx.compose.ui.graphics.StrokeJoin.Companion.Round
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.ImageVector.Builder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nice.cxonechat.ui.composable.icons.ChatIcons

internal val ChatIcons.Image: ImageVector
    get() {
        if (_image != null) {
            return _image!!
        }
        _image = Builder(
            name = "Image",
            defaultWidth = 32.0.dp,
            defaultHeight = 33.0.dp,
            viewportWidth = 32.0f,
            viewportHeight = 33.0f
        ).apply {
            path(
                fill = SolidColor(Color(0x00000000)),
                stroke = SolidColor(Color(0xFF666A76)),
                strokeLineWidth = 1.5f,
                strokeLineCap = Butt,
                strokeLineJoin = Round,
                strokeLineMiter = 4.0f,
                pathFillType = NonZero
            ) {
                moveTo(8.0f, 26.126f)
                curveTo(8.0f, 26.126f, 12.963f, 20.204f, 16.0f, 17.167f)
                curveTo(19.037f, 14.13f, 20.667f, 14.5f, 22.373f, 15.684f)
                curveTo(24.08f, 16.868f, 26.565f, 19.833f, 26.565f, 19.833f)
                moveTo(13.63f, 12.352f)
                curveTo(13.63f, 13.334f, 12.834f, 14.13f, 11.852f, 14.13f)
                curveTo(10.87f, 14.13f, 10.074f, 13.334f, 10.074f, 12.352f)
                curveTo(10.074f, 11.37f, 10.87f, 10.574f, 11.852f, 10.574f)
                curveTo(12.834f, 10.574f, 13.63f, 11.37f, 13.63f, 12.352f)
                close()
                moveTo(5.333f, 23.167f)
                curveTo(5.333f, 25.376f, 7.124f, 27.167f, 9.333f, 27.167f)
                horizontalLineTo(22.667f)
                curveTo(24.876f, 27.167f, 26.667f, 25.376f, 26.667f, 23.167f)
                verticalLineTo(9.833f)
                curveTo(26.667f, 7.624f, 24.876f, 5.833f, 22.667f, 5.833f)
                horizontalLineTo(9.333f)
                curveTo(7.124f, 5.833f, 5.333f, 7.624f, 5.333f, 9.833f)
                lineTo(5.333f, 23.167f)
                close()
            }
        }.build()
        return _image!!
    }

private var _image: ImageVector? = null

@Preview
@Composable
private fun PreviewImage() {
    Box(modifier = Modifier.padding(12.dp)) {
        Image(imageVector = ChatIcons.Image, contentDescription = "")
    }
}
