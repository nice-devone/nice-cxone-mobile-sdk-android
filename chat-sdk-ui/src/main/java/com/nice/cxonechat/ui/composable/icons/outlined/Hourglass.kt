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

internal val ChatIcons.Hourglass: ImageVector
    get() {
        if (hourglass != null) {
            return hourglass!!
        }
        hourglass = Builder(
            name = "Hourglass",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 24.0f,
            viewportHeight = 24.0f
        ).apply {
            path(
                fill = SolidColor(Color(0x00000000)),
                stroke = SolidColor(Color(0xFF704900)),
                strokeLineWidth = 1.5f,
                strokeLineCap = Round,
                strokeLineJoin = StrokeJoin.Companion.Round,
                strokeLineMiter = 4.0f,
                pathFillType = NonZero
            ) {
                moveTo(9.0f, 18.5f)
                horizontalLineTo(15.0f)
                moveTo(6.6f, 2.0f)
                horizontalLineTo(17.4f)
                curveTo(17.96f, 2.0f, 18.24f, 2.0f, 18.454f, 2.109f)
                curveTo(18.642f, 2.205f, 18.795f, 2.358f, 18.891f, 2.546f)
                curveTo(19.0f, 2.76f, 19.0f, 3.04f, 19.0f, 3.6f)
                verticalLineTo(5.675f)
                curveTo(19.0f, 6.164f, 19.0f, 6.408f, 18.945f, 6.638f)
                curveTo(18.896f, 6.843f, 18.815f, 7.038f, 18.705f, 7.217f)
                curveTo(18.582f, 7.418f, 18.409f, 7.591f, 18.063f, 7.937f)
                lineTo(15.131f, 10.869f)
                curveTo(14.735f, 11.265f, 14.537f, 11.463f, 14.463f, 11.691f)
                curveTo(14.398f, 11.892f, 14.398f, 12.108f, 14.463f, 12.309f)
                curveTo(14.537f, 12.537f, 14.735f, 12.735f, 15.131f, 13.131f)
                lineTo(18.063f, 16.063f)
                curveTo(18.409f, 16.409f, 18.582f, 16.582f, 18.705f, 16.783f)
                curveTo(18.815f, 16.962f, 18.896f, 17.157f, 18.945f, 17.361f)
                curveTo(19.0f, 17.592f, 19.0f, 17.836f, 19.0f, 18.326f)
                verticalLineTo(20.4f)
                curveTo(19.0f, 20.96f, 19.0f, 21.24f, 18.891f, 21.454f)
                curveTo(18.795f, 21.642f, 18.642f, 21.795f, 18.454f, 21.891f)
                curveTo(18.24f, 22.0f, 17.96f, 22.0f, 17.4f, 22.0f)
                horizontalLineTo(6.6f)
                curveTo(6.04f, 22.0f, 5.76f, 22.0f, 5.546f, 21.891f)
                curveTo(5.358f, 21.795f, 5.205f, 21.642f, 5.109f, 21.454f)
                curveTo(5.0f, 21.24f, 5.0f, 20.96f, 5.0f, 20.4f)
                verticalLineTo(18.326f)
                curveTo(5.0f, 17.836f, 5.0f, 17.592f, 5.055f, 17.361f)
                curveTo(5.104f, 17.157f, 5.185f, 16.962f, 5.295f, 16.783f)
                curveTo(5.418f, 16.582f, 5.591f, 16.409f, 5.937f, 16.063f)
                lineTo(8.869f, 13.131f)
                curveTo(9.265f, 12.735f, 9.463f, 12.537f, 9.537f, 12.309f)
                curveTo(9.602f, 12.108f, 9.602f, 11.892f, 9.537f, 11.691f)
                curveTo(9.463f, 11.463f, 9.265f, 11.265f, 8.869f, 10.869f)
                lineTo(5.937f, 7.937f)
                curveTo(5.591f, 7.591f, 5.418f, 7.418f, 5.295f, 7.217f)
                curveTo(5.185f, 7.038f, 5.104f, 6.843f, 5.055f, 6.638f)
                curveTo(5.0f, 6.408f, 5.0f, 6.164f, 5.0f, 5.675f)
                verticalLineTo(3.6f)
                curveTo(5.0f, 3.04f, 5.0f, 2.76f, 5.109f, 2.546f)
                curveTo(5.205f, 2.358f, 5.358f, 2.205f, 5.546f, 2.109f)
                curveTo(5.76f, 2.0f, 6.04f, 2.0f, 6.6f, 2.0f)
                close()
            }
        }.build()
        return hourglass!!
    }

private var hourglass: ImageVector? = null

@Preview
@Composable
private fun PreviewHourglass() {
    Box(modifier = Modifier.padding(12.dp)) {
        Image(imageVector = ChatIcons.Hourglass, contentDescription = "")
    }
}
