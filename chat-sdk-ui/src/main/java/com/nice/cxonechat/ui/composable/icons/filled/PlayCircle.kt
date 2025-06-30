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
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nice.cxonechat.ui.composable.icons.ChatIcons

internal val ChatIcons.PlayCircle: ImageVector
    get() {
        if (_PlayCircle != null) {
            return _PlayCircle!!
        }
        _PlayCircle = Builder(
            name = "Play-circle",
            defaultWidth = 29.0.dp,
            defaultHeight = 29.0.dp,
            viewportWidth = 29.0f,
            viewportHeight = 29.0f
        ).apply {
            group {
                path(
                    fill = SolidColor(Color(0xFFffffff)),
                    stroke = null,
                    fillAlpha = 0.8f,
                    strokeAlpha = 0.8f,
                    strokeLineWidth = 0.0f,
                    strokeLineCap = Butt,
                    strokeLineJoin = Miter,
                    strokeLineMiter = 4.0f,
                    pathFillType = NonZero
                ) {
                    moveTo(14.566f, 14.434f)
                    moveToRelative(-13.892f, 0.0f)
                    arcToRelative(13.892f, 13.892f, 0.0f, true, true, 27.783f, 0.0f)
                    arcToRelative(13.892f, 13.892f, 0.0f, true, true, -27.783f, 0.0f)
                }
                path(
                    fill = SolidColor(Color(0xFF000000)),
                    stroke = null,
                    fillAlpha = 0.64f,
                    strokeAlpha = 0.8f,
                    strokeLineWidth = 0.0f,
                    strokeLineCap = Butt,
                    strokeLineJoin = Miter,
                    strokeLineMiter = 4.0f,
                    pathFillType = NonZero
                ) {
                    moveTo(20.74f, 14.434f)
                    lineTo(11.479f, 19.781f)
                    lineTo(11.479f, 9.087f)
                    lineTo(20.74f, 14.434f)
                    close()
                }
            }
        }
            .build()
        return _PlayCircle!!
    }

private var _PlayCircle: ImageVector? = null

@Preview
@Composable
private fun Preview() {
    Box(modifier = Modifier.padding(12.dp)) {
        Image(imageVector = ChatIcons.PlayCircle, contentDescription = "")
    }
}
