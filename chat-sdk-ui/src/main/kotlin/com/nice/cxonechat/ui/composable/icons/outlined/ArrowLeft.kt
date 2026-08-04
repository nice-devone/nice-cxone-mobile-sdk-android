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

internal val ChatIcons.ArrowLeft: ImageVector
    get() {
        if (arrowLeft != null) {
            return arrowLeft!!
        }
        arrowLeft = Builder(
            name = "ArrowLeft", defaultWidth = 32.0.dp,
            defaultHeight = 32.0.dp, viewportWidth = 32.0f, viewportHeight = 32.0f
        ).apply {
            path(
                fill = SolidColor(Color(0xFF254FE6)), stroke = null, strokeLineWidth = 0.0f,
                strokeLineCap = Butt, strokeLineJoin = Miter, strokeLineMiter = 4.0f,
                pathFillType = NonZero
            ) {
                moveTo(14.049f, 23.498f)
                curveTo(13.762f, 23.498f, 13.505f, 23.387f, 13.277f, 23.166f)
                lineTo(6.842f, 16.74f)
                curveTo(6.607f, 16.512f, 6.49f, 16.245f, 6.49f, 15.939f)
                curveTo(6.49f, 15.633f, 6.607f, 15.366f, 6.842f, 15.139f)
                lineTo(13.268f, 8.723f)
                curveTo(13.385f, 8.605f, 13.509f, 8.518f, 13.639f, 8.459f)
                curveTo(13.769f, 8.4f, 13.906f, 8.371f, 14.049f, 8.371f)
                curveTo(14.348f, 8.371f, 14.599f, 8.469f, 14.801f, 8.664f)
                curveTo(15.003f, 8.859f, 15.104f, 9.103f, 15.104f, 9.396f)
                curveTo(15.104f, 9.553f, 15.074f, 9.696f, 15.016f, 9.826f)
                curveTo(14.957f, 9.95f, 14.879f, 10.061f, 14.781f, 10.158f)
                lineTo(12.594f, 12.375f)
                lineTo(8.707f, 15.939f)
                lineTo(12.594f, 19.504f)
                lineTo(14.781f, 21.711f)
                curveTo(14.879f, 21.809f, 14.957f, 21.923f, 15.016f, 22.053f)
                curveTo(15.074f, 22.183f, 15.104f, 22.323f, 15.104f, 22.473f)
                curveTo(15.104f, 22.766f, 15.003f, 23.01f, 14.801f, 23.205f)
                curveTo(14.599f, 23.4f, 14.348f, 23.498f, 14.049f, 23.498f)
                close()
                moveTo(11.998f, 17.004f)
                lineTo(8.609f, 16.809f)
                curveTo(8.349f, 16.809f, 8.134f, 16.727f, 7.965f, 16.564f)
                curveTo(7.802f, 16.402f, 7.721f, 16.193f, 7.721f, 15.939f)
                curveTo(7.721f, 15.679f, 7.802f, 15.471f, 7.965f, 15.314f)
                curveTo(8.134f, 15.152f, 8.349f, 15.07f, 8.609f, 15.07f)
                lineTo(11.998f, 14.865f)
                horizontalLineTo(23.863f)
                curveTo(24.189f, 14.865f, 24.449f, 14.966f, 24.645f, 15.168f)
                curveTo(24.846f, 15.363f, 24.947f, 15.62f, 24.947f, 15.939f)
                curveTo(24.947f, 16.252f, 24.846f, 16.509f, 24.645f, 16.711f)
                curveTo(24.449f, 16.906f, 24.189f, 17.004f, 23.863f, 17.004f)
                horizontalLineTo(11.998f)
                close()
            }
        }
            .build()
        return arrowLeft!!
    }

private var arrowLeft: ImageVector? = null

@Preview
@Composable
private fun PreviewArrowLeft() {
    Box(modifier = Modifier.padding(12.dp)) {
        Image(imageVector = ChatIcons.ArrowLeft, contentDescription = "")
    }
}
