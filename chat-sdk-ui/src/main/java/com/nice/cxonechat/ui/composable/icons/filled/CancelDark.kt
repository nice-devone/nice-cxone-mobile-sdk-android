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
import androidx.compose.ui.graphics.StrokeCap.Companion.Round
import androidx.compose.ui.graphics.StrokeJoin.Companion.Miter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.ImageVector.Builder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nice.cxonechat.ui.composable.icons.ChatIcons

internal val ChatIcons.CancelDark: ImageVector
    get() {
        if (_cancelDark != null) {
            return _cancelDark!!
        }
        _cancelDark = Builder(
            name = "Delete button dark",
            defaultWidth = 32.0.dp,
            defaultHeight = 32.0.dp,
            viewportWidth = 32.0f,
            viewportHeight = 32.0f
        ).apply {
            path(
                fill = SolidColor(Color(0xFFFF7A9A)),
                stroke = null,
                strokeLineWidth = 0.0f,
                strokeLineCap = Butt,
                strokeLineJoin = Miter,
                strokeLineMiter = 4.0f,
                pathFillType = NonZero
            ) {
                moveTo(26.667f, 16.0f)
                curveTo(26.667f, 21.891f, 21.891f, 26.667f, 16.0f, 26.667f)
                curveTo(10.109f, 26.667f, 5.333f, 21.891f, 5.333f, 16.0f)
                curveTo(5.333f, 10.109f, 10.109f, 5.333f, 16.0f, 5.333f)
                curveTo(21.891f, 5.333f, 26.667f, 10.109f, 26.667f, 16.0f)
                close()
            }
            path(
                fill = SolidColor(Color(0x00000000)),
                stroke = SolidColor(Color(0xFF030712)),
                strokeLineWidth = 1.5f,
                strokeLineCap = Round,
                strokeLineJoin = Miter,
                strokeLineMiter = 4.0f,
                pathFillType = NonZero
            ) {
                moveTo(19.017f, 12.983f)
                lineTo(16.0f, 16.0f)
                moveTo(16.0f, 16.0f)
                lineTo(12.983f, 19.017f)
                moveTo(16.0f, 16.0f)
                lineTo(19.017f, 19.017f)
                moveTo(16.0f, 16.0f)
                lineTo(12.983f, 12.983f)
                moveTo(26.667f, 16.0f)
                curveTo(26.667f, 21.891f, 21.891f, 26.667f, 16.0f, 26.667f)
                curveTo(10.109f, 26.667f, 5.333f, 21.891f, 5.333f, 16.0f)
                curveTo(5.333f, 10.109f, 10.109f, 5.333f, 16.0f, 5.333f)
                curveTo(21.891f, 5.333f, 26.667f, 10.109f, 26.667f, 16.0f)
                close()
            }
        }.build()
        return _cancelDark!!
    }

private var _cancelDark: ImageVector? = null

@Preview
@Composable
private fun PreviewCancelDark() {
    Box(modifier = Modifier.padding(12.dp)) {
        Image(imageVector = ChatIcons.CancelDark, contentDescription = "")
    }
}
