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

package com.nice.cxonechat.ui.composable.icons.notint

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

/**
 * Icon representing a document, used in the fallback thumbnail.
 * This version is intended for views with restricted space to display this regular sized icon.
 * The icon is not intended for tinting.
 */
internal val ChatIcons.Document: ImageVector
    get() {
        if (_document != null) {
            return _document!!
        }
        _document = Builder(
            name = "Document",
            defaultWidth = 68.0.dp,
            defaultHeight = 88.0.dp,
            viewportWidth = 68.0f,
            viewportHeight = 88.0f
        ).apply {
            group {
                path(
                    fill = SolidColor(Color.White),
                    stroke = null,
                    strokeLineWidth = 0.0f,
                    strokeLineCap = Butt,
                    strokeLineJoin = Miter,
                    strokeLineMiter = 4.0f,
                    pathFillType = NonZero
                ) {
                    moveTo(3.757f, -0.043f)
                    curveTo(3.935f, -0.043f, 4.113f, -0.044f, 4.291f, -0.044f)
                    curveTo(4.779f, -0.045f, 5.268f, -0.043f, 5.756f, -0.042f)
                    curveTo(6.283f, -0.04f, 6.81f, -0.041f, 7.337f, -0.041f)
                    curveTo(8.249f, -0.041f, 9.162f, -0.04f, 10.075f, -0.038f)
                    curveTo(11.394f, -0.035f, 12.714f, -0.034f, 14.033f, -0.034f)
                    curveTo(16.174f, -0.033f, 18.315f, -0.03f, 20.455f, -0.027f)
                    curveTo(22.535f, -0.024f, 24.614f, -0.021f, 26.694f, -0.02f)
                    curveTo(26.822f, -0.02f, 26.95f, -0.019f, 27.082f, -0.019f)
                    curveTo(27.725f, -0.019f, 28.368f, -0.018f, 29.012f, -0.018f)
                    curveTo(34.346f, -0.014f, 39.681f, -0.008f, 45.016f, 0.0f)
                    curveTo(45.016f, 0.083f, 45.016f, 0.167f, 45.017f, 0.252f)
                    curveTo(45.024f, 2.286f, 45.033f, 4.319f, 45.044f, 6.352f)
                    curveTo(45.049f, 7.336f, 45.054f, 8.319f, 45.057f, 9.303f)
                    curveTo(45.06f, 10.16f, 45.064f, 11.018f, 45.069f, 11.876f)
                    curveTo(45.072f, 12.33f, 45.074f, 12.783f, 45.075f, 13.237f)
                    curveTo(45.076f, 13.665f, 45.078f, 14.093f, 45.082f, 14.522f)
                    curveTo(45.083f, 14.751f, 45.083f, 14.98f, 45.083f, 15.209f)
                    curveTo(45.098f, 16.594f, 45.263f, 18.031f, 45.924f, 19.269f)
                    curveTo(45.965f, 19.348f, 46.007f, 19.428f, 46.05f, 19.51f)
                    curveTo(46.787f, 20.856f, 47.939f, 21.725f, 49.413f, 22.157f)
                    curveTo(50.15f, 22.334f, 50.856f, 22.365f, 51.611f, 22.374f)
                    curveTo(51.739f, 22.375f, 51.867f, 22.377f, 51.999f, 22.378f)
                    curveTo(55.578f, 22.423f, 59.157f, 22.466f, 62.736f, 22.507f)
                    curveTo(63.332f, 22.514f, 63.929f, 22.52f, 64.526f, 22.527f)
                    curveTo(65.684f, 22.541f, 66.842f, 22.554f, 68.0f, 22.567f)
                    curveTo(68.008f, 28.598f, 68.016f, 34.63f, 68.02f, 40.661f)
                    curveTo(68.02f, 40.794f, 68.02f, 40.794f, 68.021f, 40.931f)
                    curveTo(68.022f, 42.871f, 68.023f, 44.81f, 68.024f, 46.75f)
                    curveTo(68.025f, 47.701f, 68.026f, 48.652f, 68.026f, 49.603f)
                    curveTo(68.026f, 49.698f, 68.026f, 49.793f, 68.026f, 49.89f)
                    curveTo(68.029f, 52.959f, 68.032f, 56.027f, 68.036f, 59.096f)
                    curveTo(68.041f, 62.249f, 68.044f, 65.402f, 68.045f, 68.555f)
                    curveTo(68.045f, 69.0f, 68.045f, 69.445f, 68.045f, 69.89f)
                    curveTo(68.045f, 70.022f, 68.045f, 70.022f, 68.045f, 70.156f)
                    curveTo(68.046f, 71.567f, 68.048f, 72.978f, 68.051f, 74.39f)
                    curveTo(68.054f, 75.808f, 68.055f, 77.226f, 68.054f, 78.645f)
                    curveTo(68.054f, 79.413f, 68.054f, 80.182f, 68.057f, 80.951f)
                    curveTo(68.059f, 81.654f, 68.059f, 82.357f, 68.058f, 83.061f)
                    curveTo(68.058f, 83.315f, 68.058f, 83.57f, 68.06f, 83.825f)
                    curveTo(68.062f, 84.171f, 68.061f, 84.517f, 68.059f, 84.862f)
                    curveTo(68.061f, 85.011f, 68.061f, 85.011f, 68.063f, 85.163f)
                    curveTo(68.053f, 85.966f, 67.816f, 86.578f, 67.298f, 87.193f)
                    curveTo(67.223f, 87.257f, 67.147f, 87.322f, 67.07f, 87.388f)
                    curveTo(66.995f, 87.454f, 66.921f, 87.52f, 66.844f, 87.589f)
                    curveTo(66.157f, 88.023f, 65.434f, 88.035f, 64.646f, 88.03f)
                    curveTo(64.538f, 88.031f, 64.43f, 88.031f, 64.319f, 88.032f)
                    curveTo(63.958f, 88.033f, 63.596f, 88.032f, 63.234f, 88.031f)
                    curveTo(62.972f, 88.032f, 62.71f, 88.032f, 62.448f, 88.033f)
                    curveTo(61.728f, 88.034f, 61.008f, 88.034f, 60.288f, 88.033f)
                    curveTo(59.592f, 88.033f, 58.895f, 88.034f, 58.199f, 88.034f)
                    curveTo(56.598f, 88.036f, 54.997f, 88.036f, 53.396f, 88.036f)
                    curveTo(52.16f, 88.036f, 50.924f, 88.036f, 49.688f, 88.037f)
                    curveTo(49.511f, 88.037f, 49.333f, 88.037f, 49.156f, 88.037f)
                    curveTo(48.888f, 88.037f, 48.62f, 88.037f, 48.353f, 88.037f)
                    curveTo(45.865f, 88.038f, 43.378f, 88.039f, 40.89f, 88.038f)
                    curveTo(40.749f, 88.038f, 40.749f, 88.038f, 40.606f, 88.038f)
                    curveTo(40.131f, 88.038f, 39.655f, 88.038f, 39.18f, 88.038f)
                    curveTo(39.085f, 88.038f, 38.991f, 88.038f, 38.894f, 88.038f)
                    curveTo(38.703f, 88.038f, 38.512f, 88.038f, 38.321f, 88.038f)
                    curveTo(35.35f, 88.037f, 32.379f, 88.038f, 29.408f, 88.04f)
                    curveTo(26.07f, 88.043f, 22.733f, 88.044f, 19.395f, 88.043f)
                    curveTo(19.039f, 88.043f, 18.684f, 88.043f, 18.328f, 88.043f)
                    curveTo(18.24f, 88.043f, 18.153f, 88.043f, 18.062f, 88.043f)
                    curveTo(16.739f, 88.043f, 15.416f, 88.044f, 14.093f, 88.045f)
                    curveTo(12.589f, 88.047f, 11.084f, 88.047f, 9.58f, 88.045f)
                    curveTo(8.812f, 88.045f, 8.044f, 88.045f, 7.277f, 88.046f)
                    curveTo(6.574f, 88.047f, 5.871f, 88.047f, 5.169f, 88.045f)
                    curveTo(4.914f, 88.045f, 4.66f, 88.045f, 4.405f, 88.046f)
                    curveTo(4.06f, 88.047f, 3.714f, 88.046f, 3.369f, 88.045f)
                    curveTo(3.27f, 88.046f, 3.171f, 88.047f, 3.069f, 88.047f)
                    curveTo(2.21f, 88.039f, 1.455f, 87.839f, 0.797f, 87.254f)
                    curveTo(0.683f, 87.157f, 0.683f, 87.157f, 0.567f, 87.058f)
                    curveTo(-0.069f, 86.294f, -0.051f, 85.509f, -0.045f, 84.557f)
                    curveTo(-0.046f, 84.343f, -0.046f, 84.343f, -0.047f, 84.125f)
                    curveTo(-0.048f, 83.729f, -0.048f, 83.333f, -0.047f, 82.937f)
                    curveTo(-0.046f, 82.506f, -0.047f, 82.075f, -0.048f, 81.645f)
                    curveTo(-0.049f, 80.889f, -0.05f, 80.134f, -0.049f, 79.379f)
                    curveTo(-0.048f, 78.256f, -0.049f, 77.133f, -0.05f, 76.011f)
                    curveTo(-0.052f, 74.005f, -0.053f, 72.0f, -0.052f, 69.995f)
                    curveTo(-0.052f, 68.24f, -0.052f, 66.485f, -0.052f, 64.73f)
                    curveTo(-0.052f, 64.494f, -0.053f, 64.259f, -0.053f, 64.024f)
                    curveTo(-0.053f, 63.669f, -0.053f, 63.315f, -0.053f, 62.96f)
                    curveTo(-0.054f, 59.664f, -0.055f, 56.367f, -0.054f, 53.071f)
                    curveTo(-0.054f, 52.947f, -0.054f, 52.822f, -0.054f, 52.694f)
                    curveTo(-0.054f, 51.685f, -0.053f, 50.675f, -0.053f, 49.666f)
                    curveTo(-0.052f, 45.727f, -0.053f, 41.789f, -0.055f, 37.851f)
                    curveTo(-0.059f, 33.428f, -0.06f, 29.004f, -0.059f, 24.581f)
                    curveTo(-0.059f, 24.11f, -0.059f, 23.638f, -0.059f, 23.167f)
                    curveTo(-0.059f, 23.051f, -0.059f, 22.935f, -0.059f, 22.815f)
                    curveTo(-0.059f, 21.061f, -0.06f, 19.307f, -0.061f, 17.553f)
                    curveTo(-0.063f, 15.559f, -0.063f, 13.565f, -0.061f, 11.571f)
                    curveTo(-0.06f, 10.455f, -0.06f, 9.338f, -0.062f, 8.222f)
                    curveTo(-0.063f, 7.477f, -0.063f, 6.731f, -0.061f, 5.986f)
                    curveTo(-0.06f, 5.561f, -0.06f, 5.136f, -0.061f, 4.711f)
                    curveTo(-0.063f, 4.254f, -0.062f, 3.796f, -0.06f, 3.338f)
                    curveTo(-0.061f, 3.206f, -0.062f, 3.074f, -0.063f, 2.938f)
                    curveTo(-0.054f, 1.97f, 0.12f, 1.312f, 0.811f, 0.619f)
                    curveTo(1.788f, -0.052f, 2.595f, -0.051f, 3.757f, -0.043f)
                    close()
                }
                path(
                    fill = SolidColor(Color(0xFFffffff)),
                    stroke = null,
                    strokeLineWidth = 0.0f,
                    strokeLineCap = Butt,
                    strokeLineJoin = Miter,
                    strokeLineMiter = 4.0f,
                    pathFillType = NonZero
                ) {
                    moveTo(46.802f, 2.03f)
                    curveTo(47.144f, 2.195f, 47.389f, 2.351f, 47.656f, 2.622f)
                    curveTo(47.727f, 2.694f, 47.798f, 2.765f, 47.871f, 2.839f)
                    curveTo(47.946f, 2.916f, 48.021f, 2.993f, 48.099f, 3.072f)
                    curveTo(48.178f, 3.152f, 48.257f, 3.232f, 48.339f, 3.314f)
                    curveTo(48.591f, 3.57f, 48.843f, 3.826f, 49.095f, 4.082f)
                    curveTo(49.345f, 4.336f, 49.595f, 4.59f, 49.846f, 4.844f)
                    curveTo(50.001f, 5.001f, 50.157f, 5.159f, 50.312f, 5.317f)
                    curveTo(50.677f, 5.687f, 51.048f, 6.043f, 51.441f, 6.383f)
                    curveTo(51.95f, 6.823f, 52.422f, 7.293f, 52.894f, 7.772f)
                    curveTo(52.983f, 7.862f, 53.072f, 7.952f, 53.164f, 8.044f)
                    curveTo(53.54f, 8.423f, 53.915f, 8.803f, 54.291f, 9.182f)
                    curveTo(54.567f, 9.461f, 54.843f, 9.74f, 55.12f, 10.019f)
                    curveTo(55.203f, 10.103f, 55.286f, 10.188f, 55.372f, 10.275f)
                    curveTo(55.759f, 10.665f, 56.15f, 11.042f, 56.566f, 11.401f)
                    curveTo(57.083f, 11.848f, 57.562f, 12.327f, 58.041f, 12.813f)
                    curveTo(58.179f, 12.952f, 58.179f, 12.952f, 58.319f, 13.093f)
                    curveTo(58.706f, 13.483f, 59.093f, 13.875f, 59.48f, 14.266f)
                    curveTo(59.764f, 14.553f, 60.049f, 14.84f, 60.334f, 15.127f)
                    curveTo(60.42f, 15.214f, 60.505f, 15.301f, 60.594f, 15.391f)
                    curveTo(61.064f, 15.864f, 61.546f, 16.313f, 62.051f, 16.749f)
                    curveTo(62.444f, 17.097f, 62.814f, 17.47f, 63.184f, 17.842f)
                    curveTo(63.265f, 17.923f, 63.346f, 18.004f, 63.429f, 18.087f)
                    curveTo(63.597f, 18.256f, 63.765f, 18.424f, 63.933f, 18.593f)
                    curveTo(64.191f, 18.853f, 64.451f, 19.112f, 64.71f, 19.371f)
                    curveTo(64.874f, 19.536f, 65.038f, 19.7f, 65.201f, 19.865f)
                    curveTo(65.279f, 19.942f, 65.357f, 20.02f, 65.437f, 20.1f)
                    curveTo(65.976f, 20.643f, 65.976f, 20.643f, 65.976f, 20.776f)
                    curveTo(64.024f, 20.785f, 62.073f, 20.791f, 60.121f, 20.795f)
                    curveTo(59.215f, 20.796f, 58.309f, 20.799f, 57.403f, 20.803f)
                    curveTo(56.613f, 20.807f, 55.822f, 20.809f, 55.032f, 20.81f)
                    curveTo(54.614f, 20.81f, 54.196f, 20.811f, 53.778f, 20.814f)
                    curveTo(53.31f, 20.817f, 52.843f, 20.817f, 52.375f, 20.817f)
                    curveTo(52.238f, 20.818f, 52.1f, 20.819f, 51.959f, 20.821f)
                    curveTo(50.522f, 20.815f, 49.287f, 20.438f, 48.204f, 19.462f)
                    curveTo(47.355f, 18.57f, 46.787f, 17.461f, 46.788f, 16.216f)
                    curveTo(46.788f, 16.091f, 46.787f, 15.966f, 46.787f, 15.837f)
                    curveTo(46.788f, 15.7f, 46.788f, 15.563f, 46.789f, 15.422f)
                    curveTo(46.789f, 15.278f, 46.789f, 15.134f, 46.789f, 14.985f)
                    curveTo(46.789f, 14.589f, 46.789f, 14.193f, 46.79f, 13.798f)
                    curveTo(46.791f, 13.384f, 46.791f, 12.97f, 46.791f, 12.557f)
                    curveTo(46.791f, 11.773f, 46.792f, 10.99f, 46.794f, 10.207f)
                    curveTo(46.795f, 9.315f, 46.796f, 8.424f, 46.796f, 7.532f)
                    curveTo(46.797f, 5.698f, 46.799f, 3.864f, 46.802f, 2.03f)
                    close()
                }
            }
        }.build()
        return _document!!
    }

private var _document: ImageVector? = null

@Preview
@Composable
private fun PreviewBlankFile() {
    Box(modifier = Modifier.padding(12.dp)) {
        Image(imageVector = ChatIcons.Document, contentDescription = "")
    }
}
