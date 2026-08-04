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
import androidx.compose.ui.graphics.StrokeJoin.Companion.Miter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.ImageVector.Builder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nice.cxonechat.ui.composable.icons.ChatIcons

internal val ChatIcons.Folder: ImageVector
    get() {
        if (_folder != null) {
            return _folder!!
        }
        _folder = Builder(
            name = "Folder",
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
                strokeLineJoin = Miter,
                strokeLineMiter = 4.0f,
                pathFillType = NonZero
            ) {
                moveTo(5.333f, 9.167f)
                curveTo(5.333f, 7.694f, 6.527f, 6.5f, 8.0f, 6.5f)
                horizontalLineTo(11.411f)
                curveTo(12.626f, 6.5f, 13.775f, 7.052f, 14.534f, 8.001f)
                lineTo(15.199f, 8.833f)
                curveTo(15.705f, 9.465f, 16.468f, 9.833f, 17.278f, 9.833f)
                curveTo(18.606f, 9.833f, 20.752f, 9.833f, 22.668f, 9.833f)
                curveTo(24.877f, 9.833f, 26.667f, 11.624f, 26.667f, 13.833f)
                verticalLineTo(22.5f)
                curveTo(26.667f, 24.709f, 24.876f, 26.5f, 22.667f, 26.5f)
                horizontalLineTo(9.333f)
                curveTo(7.124f, 26.5f, 5.333f, 24.709f, 5.333f, 22.5f)
                lineTo(5.333f, 9.167f)
                close()
            }
        }.build()
        return _folder!!
    }

private var _folder: ImageVector? = null

@Preview
@Composable
private fun PreviewFolder() {
    Box(modifier = Modifier.padding(12.dp)) {
        Image(imageVector = ChatIcons.Folder, contentDescription = "")
    }
}
