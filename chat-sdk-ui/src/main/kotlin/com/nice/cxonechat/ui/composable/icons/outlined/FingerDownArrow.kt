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

internal val ChatIcons.FingerDownArrow: ImageVector
    get() {
        if (_icon != null) {
            return _icon!!
        }
        _icon = Builder(
            name = "FingerDownArrow", defaultWidth = 12.0.dp, defaultHeight = 16.0.dp,
            viewportWidth = 12.0f, viewportHeight = 16.0f
        ).apply {
            path(
                fill = SolidColor(Color(0xFF254FE6)), stroke = null, strokeLineWidth = 0.0f,
                strokeLineCap = Butt, strokeLineJoin = Miter, strokeLineMiter = 4.0f,
                pathFillType = NonZero
            ) {
                moveTo(5.05f, 0.907f)
                curveTo(7.463f, 0.907f, 9.151f, 2.206f, 10.061f, 4.77f)
                lineTo(11.264f, 8.153f)
                curveTo(11.346f, 8.393f, 11.387f, 8.666f, 11.387f, 8.871f)
                curveTo(11.387f, 9.623f, 10.799f, 10.047f, 10.136f, 10.047f)
                curveTo(9.65f, 10.047f, 9.254f, 9.76f, 9.021f, 9.227f)
                lineTo(8.563f, 8.133f)
                curveTo(8.557f, 8.105f, 8.536f, 8.085f, 8.509f, 8.085f)
                curveTo(8.475f, 8.085f, 8.454f, 8.112f, 8.454f, 8.146f)
                verticalLineTo(13.854f)
                curveTo(8.454f, 14.716f, 7.88f, 15.29f, 7.066f, 15.29f)
                curveTo(6.26f, 15.29f, 5.686f, 14.716f, 5.686f, 13.854f)
                verticalLineTo(11.578f)
                curveTo(5.521f, 11.626f, 5.357f, 11.646f, 5.2f, 11.646f)
                curveTo(4.605f, 11.646f, 4.154f, 11.311f, 3.977f, 10.758f)
                curveTo(3.785f, 10.826f, 3.587f, 10.86f, 3.382f, 10.86f)
                curveTo(2.808f, 10.86f, 2.397f, 10.553f, 2.24f, 10.054f)
                curveTo(0.969f, 10.04f, 0.224f, 9.117f, 0.224f, 7.538f)
                verticalLineTo(6.048f)
                curveTo(0.224f, 2.849f, 2.158f, 0.907f, 5.05f, 0.907f)
                close()
                moveTo(5.016f, 1.851f)
                curveTo(2.603f, 1.851f, 1.126f, 3.409f, 1.126f, 6.157f)
                verticalLineTo(7.401f)
                curveTo(1.126f, 8.495f, 1.475f, 9.117f, 2.083f, 9.117f)
                verticalLineTo(8.331f)
                curveTo(2.083f, 8.085f, 2.281f, 7.914f, 2.507f, 7.914f)
                curveTo(2.739f, 7.914f, 2.951f, 8.085f, 2.951f, 8.331f)
                verticalLineTo(9.397f)
                curveTo(2.951f, 9.76f, 3.149f, 9.979f, 3.498f, 9.979f)
                curveTo(3.628f, 9.979f, 3.785f, 9.944f, 3.901f, 9.89f)
                verticalLineTo(8.598f)
                curveTo(3.901f, 8.345f, 4.106f, 8.174f, 4.332f, 8.174f)
                curveTo(4.564f, 8.174f, 4.77f, 8.345f, 4.77f, 8.598f)
                verticalLineTo(10.184f)
                curveTo(4.77f, 10.546f, 4.975f, 10.765f, 5.316f, 10.765f)
                curveTo(5.453f, 10.765f, 5.61f, 10.731f, 5.727f, 10.676f)
                verticalLineTo(8.857f)
                curveTo(5.727f, 8.618f, 5.918f, 8.434f, 6.15f, 8.434f)
                curveTo(6.39f, 8.434f, 6.595f, 8.618f, 6.595f, 8.857f)
                verticalLineTo(13.916f)
                curveTo(6.595f, 14.203f, 6.786f, 14.408f, 7.066f, 14.408f)
                curveTo(7.347f, 14.408f, 7.545f, 14.203f, 7.545f, 13.916f)
                verticalLineTo(6.827f)
                curveTo(7.545f, 6.513f, 7.75f, 6.287f, 8.037f, 6.287f)
                curveTo(8.276f, 6.287f, 8.488f, 6.396f, 8.639f, 6.738f)
                lineTo(9.568f, 8.816f)
                curveTo(9.671f, 9.049f, 9.808f, 9.192f, 10.04f, 9.192f)
                curveTo(10.314f, 9.192f, 10.491f, 8.987f, 10.491f, 8.748f)
                curveTo(10.491f, 8.632f, 10.471f, 8.543f, 10.436f, 8.44f)
                lineTo(9.227f, 5.063f)
                curveTo(8.393f, 2.732f, 6.93f, 1.851f, 5.016f, 1.851f)
                close()
            }
        }
            .build()
        return _icon!!
    }

private var _icon: ImageVector? = null

@Preview
@Composable
private fun FingerDownArrowPreview() {
    Box(modifier = Modifier.padding(12.dp)) {
        Image(imageVector = ChatIcons.FingerDownArrow, contentDescription = "")
    }
}
