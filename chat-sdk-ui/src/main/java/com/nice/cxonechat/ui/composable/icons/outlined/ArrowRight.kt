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
import androidx.compose.ui.graphics.StrokeJoin.Companion.Miter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.ImageVector.Builder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nice.cxonechat.ui.composable.icons.ChatIcons

internal val ChatIcons.ArrowRight: ImageVector
    get() {
        if (_arrowRight != null) {
            return _arrowRight!!
        }
        _arrowRight = Builder(
            name = "ArrowRight",
            defaultWidth = 32.0.dp,
            defaultHeight = 33.0.dp,
            viewportWidth = 32.0f,
            viewportHeight = 33.0f
        ).apply {
            path(
                fill = SolidColor(Color(0x00000000)),
                stroke = SolidColor(Color(0xFF254FE6)),
                strokeLineWidth = 1.5f,
                strokeLineCap = Round,
                strokeLineJoin = Miter,
                strokeLineMiter = 4.0f,
                pathFillType = NonZero
            ) {
                moveTo(19.333f, 9.167f)
                lineTo(26.497f, 16.329f)
                curveTo(26.61f, 16.44f, 26.667f, 16.586f, 26.667f, 16.731f)
                moveTo(19.333f, 23.833f)
                lineTo(26.497f, 17.133f)
                curveTo(26.61f, 17.022f, 26.667f, 16.877f, 26.667f, 16.731f)
                moveTo(26.667f, 16.731f)
                horizontalLineTo(5.333f)
            }
        }.build()
        return _arrowRight!!
    }

private var _arrowRight: ImageVector? = null

@Preview
@Composable
private fun PreviewArrowRight() {
    Box(modifier = Modifier.padding(12.dp)) {
        Image(imageVector = ChatIcons.ArrowRight, contentDescription = "")
    }
}
