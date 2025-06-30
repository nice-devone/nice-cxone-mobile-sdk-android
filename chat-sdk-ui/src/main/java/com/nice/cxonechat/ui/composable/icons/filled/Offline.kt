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

package com.nice.cxonechat.ui.composable.icons.filled

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
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
import com.nice.cxonechat.ui.composable.theme.ChatTheme

internal val ChatIcons.Offline: ImageVector
    get() {
        if (_offline != null) {
            return _offline!!
        }
        _offline = Builder(
            name = "Offline",
            defaultWidth = 77.0.dp,
            defaultHeight = 76.0.dp,
            viewportWidth = 77.0f,
            viewportHeight = 76.0f
        ).apply {
            path(
                fill = SolidColor(Color(0xFF013B72)),
                stroke = null,
                strokeLineWidth = 0.0f,
                strokeLineCap = Butt,
                strokeLineJoin = Miter,
                strokeLineMiter = 4.0f,
                pathFillType = NonZero
            ) {
                moveTo(53.423f, 14.853f)
                curveTo(54.572f, 14.853f, 55.37f, 15.587f, 55.37f, 16.673f)
                curveTo(55.37f, 17.854f, 54.572f, 18.524f, 53.423f, 18.524f)
                horizontalLineTo(42.57f)
                curveTo(41.389f, 18.524f, 40.591f, 17.758f, 40.591f, 16.705f)
                curveTo(40.591f, 15.971f, 40.846f, 15.428f, 41.548f, 14.534f)
                lineTo(49.113f, 4.862f)
                verticalLineTo(4.703f)
                horizontalLineTo(42.378f)
                curveTo(41.229f, 4.703f, 40.431f, 4.001f, 40.431f, 2.851f)
                curveTo(40.431f, 1.734f, 41.229f, 1.0f, 42.378f, 1.0f)
                horizontalLineTo(52.752f)
                curveTo(53.933f, 1.0f, 54.795f, 1.734f, 54.795f, 2.851f)
                curveTo(54.795f, 3.522f, 54.54f, 4.096f, 53.838f, 4.99f)
                lineTo(46.305f, 14.694f)
                verticalLineTo(14.853f)
                horizontalLineTo(53.423f)
                close()
                moveTo(68.681f, 27.302f)
                curveTo(69.766f, 27.302f, 70.5f, 27.973f, 70.5f, 29.026f)
                curveTo(70.5f, 30.079f, 69.766f, 30.718f, 68.681f, 30.718f)
                horizontalLineTo(60.445f)
                curveTo(59.36f, 30.718f, 58.594f, 30.015f, 58.594f, 28.994f)
                curveTo(58.594f, 28.292f, 58.849f, 27.685f, 59.52f, 26.887f)
                lineTo(64.627f, 20.312f)
                verticalLineTo(20.184f)
                horizontalLineTo(60.254f)
                curveTo(59.168f, 20.184f, 58.434f, 19.546f, 58.434f, 18.428f)
                curveTo(58.434f, 17.407f, 59.168f, 16.737f, 60.254f, 16.737f)
                horizontalLineTo(68.01f)
                curveTo(69.127f, 16.737f, 69.957f, 17.407f, 69.957f, 18.46f)
                curveTo(69.957f, 19.163f, 69.67f, 19.705f, 69.032f, 20.535f)
                lineTo(63.829f, 27.175f)
                verticalLineTo(27.302f)
                horizontalLineTo(68.681f)
                close()
                moveTo(34.941f, 75.534f)
                curveTo(18.342f, 75.534f, 6.5f, 63.5f, 6.5f, 47.38f)
                curveTo(6.5f, 35.41f, 13.395f, 24.813f, 23.641f, 20.759f)
                curveTo(25.11f, 20.184f, 26.259f, 20.28f, 26.897f, 21.014f)
                curveTo(27.472f, 21.684f, 27.503f, 22.77f, 26.865f, 24.11f)
                curveTo(25.716f, 26.504f, 24.95f, 30.622f, 24.95f, 34.325f)
                curveTo(24.95f, 48.338f, 34.334f, 57.371f, 48.667f, 57.371f)
                curveTo(52.178f, 57.371f, 55.37f, 56.797f, 58.019f, 55.711f)
                curveTo(59.136f, 55.232f, 60.222f, 55.328f, 60.764f, 55.903f)
                curveTo(61.467f, 56.573f, 61.53f, 57.722f, 60.956f, 59.095f)
                curveTo(56.583f, 69.373f, 46.815f, 75.534f, 34.941f, 75.534f)
                close()
                moveTo(54.317f, 38.315f)
                curveTo(55.338f, 38.315f, 56.008f, 38.953f, 56.008f, 39.943f)
                curveTo(56.008f, 40.932f, 55.306f, 41.571f, 54.317f, 41.571f)
                horizontalLineTo(46.911f)
                curveTo(45.858f, 41.571f, 45.155f, 40.9f, 45.155f, 39.975f)
                curveTo(45.155f, 39.272f, 45.411f, 38.762f, 46.049f, 37.964f)
                lineTo(50.422f, 32.314f)
                verticalLineTo(32.186f)
                horizontalLineTo(46.72f)
                curveTo(45.73f, 32.186f, 45.028f, 31.58f, 45.028f, 30.558f)
                curveTo(45.028f, 29.569f, 45.73f, 28.93f, 46.72f, 28.93f)
                horizontalLineTo(53.71f)
                curveTo(54.731f, 28.93f, 55.498f, 29.537f, 55.498f, 30.558f)
                curveTo(55.498f, 31.165f, 55.242f, 31.675f, 54.636f, 32.473f)
                lineTo(50.199f, 38.187f)
                verticalLineTo(38.315f)
                horizontalLineTo(54.317f)
                close()
            }
        }
            .build()
        return _offline!!
    }

private var _offline: ImageVector? = null

@Preview
@Composable
private fun Preview() {
    Box(modifier = Modifier.padding(12.dp)) {
        Icon(imageVector = ChatIcons.Offline, contentDescription = "", tint = ChatTheme.chatColors.onAccentHeader)
    }
}
