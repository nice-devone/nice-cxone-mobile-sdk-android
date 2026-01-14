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

internal val ChatIcons.PressFinger: ImageVector
    get() {
        if (_finger != null) {
            return _finger!!
        }
        _finger = Builder(
            name = "Finger", defaultWidth = 15.0.dp, defaultHeight = 17.0.dp,
            viewportWidth = 15.0f, viewportHeight = 17.0f
        ).apply {
            path(
                fill = SolidColor(Color(0xFF254FE6)), stroke = null, strokeLineWidth = 0.0f,
                strokeLineCap = Butt, strokeLineJoin = Miter, strokeLineMiter = 4.0f,
                pathFillType = NonZero
            ) {
                moveTo(3.874f, 0.027f)
                curveTo(5.795f, 0.027f, 7.395f, 1.538f, 7.483f, 3.473f)
                curveTo(7.49f, 3.562f, 7.497f, 3.644f, 7.49f, 3.705f)
                curveTo(7.47f, 3.992f, 7.23f, 4.184f, 6.943f, 4.184f)
                curveTo(6.677f, 4.184f, 6.465f, 4.013f, 6.438f, 3.691f)
                curveTo(6.438f, 3.63f, 6.431f, 3.589f, 6.431f, 3.548f)
                curveTo(6.376f, 2.181f, 5.248f, 1.094f, 3.874f, 1.094f)
                curveTo(2.445f, 1.094f, 1.29f, 2.249f, 1.29f, 3.671f)
                curveTo(1.29f, 4.279f, 1.502f, 4.854f, 1.857f, 5.291f)
                curveTo(2.172f, 5.694f, 1.851f, 6.159f, 1.447f, 6.159f)
                curveTo(1.283f, 6.159f, 1.112f, 6.091f, 0.982f, 5.906f)
                curveTo(0.511f, 5.298f, 0.224f, 4.491f, 0.224f, 3.671f)
                curveTo(0.224f, 1.661f, 1.857f, 0.027f, 3.874f, 0.027f)
                close()
                moveTo(10.396f, 15.565f)
                curveTo(8.044f, 16.42f, 5.918f, 15.757f, 4.134f, 13.61f)
                lineTo(1.816f, 10.814f)
                curveTo(1.652f, 10.616f, 1.502f, 10.363f, 1.427f, 10.144f)
                curveTo(1.153f, 9.379f, 1.57f, 8.716f, 2.268f, 8.463f)
                curveTo(2.739f, 8.292f, 3.218f, 8.401f, 3.628f, 8.798f)
                lineTo(4.298f, 9.522f)
                curveTo(4.318f, 9.55f, 4.339f, 9.557f, 4.366f, 9.55f)
                curveTo(4.4f, 9.536f, 4.407f, 9.509f, 4.394f, 9.468f)
                lineTo(2.534f, 4.375f)
                curveTo(2.22f, 3.486f, 2.589f, 2.693f, 3.437f, 2.386f)
                curveTo(4.264f, 2.085f, 5.057f, 2.461f, 5.378f, 3.343f)
                lineTo(6.082f, 5.277f)
                curveTo(6.205f, 5.195f, 6.321f, 5.134f, 6.451f, 5.086f)
                curveTo(7.019f, 4.874f, 7.572f, 5.031f, 7.948f, 5.496f)
                curveTo(8.099f, 5.373f, 8.263f, 5.284f, 8.447f, 5.216f)
                curveTo(9.008f, 5.011f, 9.534f, 5.154f, 9.876f, 5.571f)
                curveTo(11.127f, 5.147f, 12.193f, 5.783f, 12.747f, 7.308f)
                lineTo(13.308f, 8.846f)
                curveTo(14.442f, 11.963f, 13.267f, 14.519f, 10.396f, 15.565f)
                close()
                moveTo(10.04f, 14.458f)
                curveTo(12.323f, 13.631f, 13.171f, 11.669f, 12.234f, 9.105f)
                lineTo(11.735f, 7.738f)
                curveTo(11.4f, 6.829f, 10.929f, 6.419f, 10.409f, 6.61f)
                lineTo(10.648f, 7.26f)
                curveTo(10.744f, 7.526f, 10.594f, 7.8f, 10.354f, 7.889f)
                curveTo(10.102f, 7.984f, 9.808f, 7.875f, 9.712f, 7.602f)
                lineTo(9.343f, 6.59f)
                curveTo(9.233f, 6.296f, 8.987f, 6.173f, 8.707f, 6.275f)
                curveTo(8.591f, 6.316f, 8.468f, 6.392f, 8.386f, 6.474f)
                lineTo(8.81f, 7.649f)
                curveTo(8.912f, 7.916f, 8.769f, 8.176f, 8.516f, 8.271f)
                curveTo(8.27f, 8.354f, 7.982f, 8.244f, 7.887f, 7.984f)
                lineTo(7.333f, 6.467f)
                curveTo(7.224f, 6.166f, 6.978f, 6.05f, 6.704f, 6.152f)
                curveTo(6.581f, 6.187f, 6.465f, 6.269f, 6.376f, 6.351f)
                lineTo(6.991f, 8.019f)
                curveTo(7.087f, 8.278f, 6.943f, 8.552f, 6.69f, 8.641f)
                curveTo(6.438f, 8.736f, 6.15f, 8.62f, 6.055f, 8.36f)
                lineTo(4.332f, 3.644f)
                curveTo(4.25f, 3.397f, 4.024f, 3.288f, 3.785f, 3.37f)
                curveTo(3.546f, 3.459f, 3.443f, 3.691f, 3.532f, 3.938f)
                lineTo(5.973f, 10.644f)
                curveTo(6.096f, 10.985f, 5.952f, 11.3f, 5.651f, 11.409f)
                curveTo(5.405f, 11.498f, 5.152f, 11.464f, 4.879f, 11.19f)
                lineTo(3.245f, 9.557f)
                curveTo(3.102f, 9.393f, 2.944f, 9.324f, 2.76f, 9.393f)
                curveTo(2.521f, 9.481f, 2.432f, 9.7f, 2.507f, 9.898f)
                curveTo(2.541f, 10.001f, 2.589f, 10.063f, 2.65f, 10.138f)
                lineTo(4.981f, 12.893f)
                curveTo(6.554f, 14.752f, 8.222f, 15.121f, 10.04f, 14.458f)
                close()
            }
        }
            .build()
        return _finger!!
    }

private var _finger: ImageVector? = null

@Preview
@Composable
private fun Preview() {
    Box(modifier = Modifier.padding(12.dp)) {
        Image(imageVector = ChatIcons.PressFinger, contentDescription = "")
    }
}
