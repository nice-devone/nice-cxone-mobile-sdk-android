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

internal val ChatIcons.Expired: ImageVector
    get() {
        if (_expired != null) {
            return _expired!!
        }
        _expired = Builder(
            name = "Expired",
            defaultWidth = 83.0.dp,
            defaultHeight = 76.0.dp,
            viewportWidth = 83.0f,
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
                moveTo(38.5f, 70.719f)
                curveTo(20.813f, 70.719f, 6.219f, 56.125f, 6.219f, 38.438f)
                curveTo(6.219f, 20.781f, 20.781f, 6.156f, 38.469f, 6.156f)
                curveTo(54.906f, 6.156f, 68.688f, 18.781f, 70.531f, 34.781f)
                curveTo(68.563f, 34.281f, 66.0f, 34.188f, 64.063f, 34.5f)
                curveTo(62.188f, 22.031f, 51.469f, 12.531f, 38.469f, 12.531f)
                curveTo(24.094f, 12.531f, 12.594f, 24.063f, 12.594f, 38.438f)
                curveTo(12.594f, 52.813f, 24.125f, 64.375f, 38.5f, 64.375f)
                curveTo(41.656f, 64.375f, 44.656f, 63.781f, 47.438f, 62.75f)
                curveTo(48.25f, 64.75f, 49.375f, 66.625f, 50.844f, 68.219f)
                curveTo(47.0f, 69.844f, 42.813f, 70.719f, 38.5f, 70.719f)
                close()
                moveTo(22.688f, 42.313f)
                curveTo(21.281f, 42.313f, 20.219f, 41.219f, 20.219f, 39.844f)
                curveTo(20.219f, 38.438f, 21.281f, 37.375f, 22.688f, 37.375f)
                horizontalLineTo(36.0f)
                verticalLineTo(19.344f)
                curveTo(36.0f, 17.969f, 37.094f, 16.875f, 38.469f, 16.875f)
                curveTo(39.875f, 16.875f, 40.938f, 17.969f, 40.938f, 19.344f)
                verticalLineTo(39.844f)
                curveTo(40.938f, 41.219f, 39.875f, 42.313f, 38.469f, 42.313f)
                horizontalLineTo(22.688f)
                close()
                moveTo(66.219f, 70.875f)
                curveTo(57.406f, 70.875f, 50.125f, 63.625f, 50.125f, 54.781f)
                curveTo(50.125f, 45.969f, 57.406f, 38.719f, 66.219f, 38.719f)
                curveTo(75.031f, 38.719f, 82.313f, 45.969f, 82.313f, 54.781f)
                curveTo(82.313f, 63.563f, 74.969f, 70.875f, 66.219f, 70.875f)
                close()
                moveTo(65.844f, 58.719f)
                curveTo(67.031f, 58.719f, 67.75f, 58.125f, 67.938f, 57.063f)
                curveTo(68.094f, 55.906f, 68.719f, 55.344f, 69.906f, 54.469f)
                curveTo(71.688f, 53.156f, 73.313f, 52.188f, 73.313f, 49.625f)
                curveTo(73.313f, 46.5f, 70.625f, 44.219f, 66.594f, 44.219f)
                curveTo(63.156f, 44.219f, 59.813f, 45.969f, 59.813f, 48.531f)
                curveTo(59.813f, 49.563f, 60.531f, 50.344f, 61.719f, 50.344f)
                curveTo(62.688f, 50.344f, 63.188f, 49.75f, 63.75f, 49.156f)
                curveTo(64.438f, 48.438f, 65.25f, 47.875f, 66.531f, 47.875f)
                curveTo(67.969f, 47.875f, 68.938f, 48.688f, 68.938f, 49.844f)
                curveTo(68.938f, 51.156f, 67.969f, 51.813f, 66.313f, 52.938f)
                curveTo(64.906f, 53.938f, 63.844f, 54.938f, 63.844f, 56.75f)
                verticalLineTo(56.875f)
                curveTo(63.844f, 58.031f, 64.656f, 58.719f, 65.844f, 58.719f)
                close()
                moveTo(65.844f, 65.125f)
                curveTo(67.281f, 65.125f, 68.406f, 64.063f, 68.406f, 62.656f)
                curveTo(68.406f, 61.281f, 67.281f, 60.188f, 65.844f, 60.188f)
                curveTo(64.406f, 60.188f, 63.281f, 61.25f, 63.281f, 62.656f)
                curveTo(63.281f, 64.063f, 64.406f, 65.125f, 65.844f, 65.125f)
                close()
            }
        }
            .build()
        return _expired!!
    }

private var _expired: ImageVector? = null

@Preview
@Composable
private fun PreviewExpired() {
    Box(modifier = Modifier.padding(12.dp)) {
        Image(imageVector = ChatIcons.Expired, contentDescription = "")
    }
}
