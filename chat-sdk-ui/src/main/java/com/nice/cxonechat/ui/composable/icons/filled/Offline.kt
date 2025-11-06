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
import androidx.compose.ui.graphics.StrokeCap.Companion.Round
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.ImageVector.Builder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nice.cxonechat.ui.composable.icons.ChatIcons

internal val ChatIcons.Offline: ImageVector
    get() {
        if (_offline != null) {
            return _offline!!
        }
        _offline = Builder(
            name = "Offline",
            defaultWidth = 34.0.dp,
            defaultHeight = 34.0.dp,
            viewportWidth = 34.0f,
            viewportHeight = 34.0f
        ).apply {
            path(
                fill = SolidColor(Color(0x00000000)),
                stroke = SolidColor(Color(0xFFffffff)),
                strokeLineWidth = 2.1f,
                strokeLineCap = Round,
                strokeLineJoin = StrokeJoin.Companion.Round,
                strokeLineMiter = 4.0f,
                pathFillType = NonZero
            ) {
                moveTo(23.3f, 24.0f)
                horizontalLineTo(30.3f)
                lineTo(23.3f, 31.0f)
                horizontalLineTo(30.3f)
                moveTo(30.931f, 18.4f)
                curveTo(30.977f, 17.94f, 31.0f, 17.473f, 31.0f, 17.0f)
                curveTo(31.0f, 9.268f, 24.732f, 3.0f, 17.0f, 3.0f)
                curveTo(9.268f, 3.0f, 3.0f, 9.268f, 3.0f, 17.0f)
                curveTo(3.0f, 24.732f, 9.268f, 31.0f, 17.0f, 31.0f)
                curveTo(17.235f, 31.0f, 17.468f, 30.994f, 17.7f, 30.983f)
                curveTo(17.935f, 30.971f, 18.168f, 30.954f, 18.4f, 30.931f)
                moveTo(17.0f, 8.6f)
                verticalLineTo(17.0f)
                lineTo(22.234f, 19.617f)
            }
        }
            .build()
        return _offline!!
    }

private var _offline: ImageVector? = null

@Preview
@Composable
private fun PreviewOffline() {
    Box(modifier = Modifier.padding(12.dp)) {
        Icon(imageVector = ChatIcons.Offline, contentDescription = "")
    }
}
