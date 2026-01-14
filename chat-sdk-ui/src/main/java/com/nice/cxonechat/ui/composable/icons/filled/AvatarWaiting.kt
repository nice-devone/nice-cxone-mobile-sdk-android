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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
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
import com.nice.cxonechat.ui.composable.theme.ChatTheme

internal val ChatIcons.AvatarWaiting: ImageVector
    get() {
        if (_avatarWaiting != null) {
            return _avatarWaiting!!
        }
        _avatarWaiting = Builder(
            name = "AvatarWaiting",
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
                moveTo(44.018f, 70.761f)
                curveTo(41.922f, 70.761f, 39.857f, 70.556f, 37.823f, 70.147f)
                curveTo(35.809f, 69.758f, 33.878f, 69.196f, 32.029f, 68.459f)
                curveTo(33.775f, 66.577f, 35.142f, 64.428f, 36.128f, 62.014f)
                curveTo(37.134f, 59.619f, 37.638f, 57.082f, 37.638f, 54.402f)
                curveTo(37.638f, 52.908f, 37.474f, 51.424f, 37.145f, 49.951f)
                curveTo(38.172f, 49.726f, 39.261f, 49.552f, 40.412f, 49.429f)
                curveTo(41.562f, 49.286f, 42.764f, 49.215f, 44.018f, 49.215f)
                curveTo(46.647f, 49.215f, 49.072f, 49.501f, 51.291f, 50.074f)
                curveTo(53.51f, 50.626f, 55.472f, 51.373f, 57.177f, 52.315f)
                curveTo(58.883f, 53.235f, 60.311f, 54.258f, 61.461f, 55.384f)
                curveTo(63.639f, 53.215f, 65.344f, 50.667f, 66.577f, 47.741f)
                curveTo(67.83f, 44.815f, 68.457f, 41.685f, 68.457f, 38.349f)
                curveTo(68.457f, 34.973f, 67.82f, 31.822f, 66.546f, 28.896f)
                curveTo(65.293f, 25.949f, 63.547f, 23.361f, 61.307f, 21.131f)
                curveTo(59.068f, 18.9f, 56.468f, 17.161f, 53.51f, 15.913f)
                curveTo(50.572f, 14.644f, 47.408f, 14.01f, 44.018f, 14.01f)
                curveTo(40.997f, 14.01f, 38.141f, 14.511f, 35.45f, 15.514f)
                curveTo(32.779f, 16.516f, 30.375f, 17.928f, 28.238f, 19.749f)
                curveTo(26.101f, 21.55f, 24.324f, 23.668f, 22.906f, 26.103f)
                curveTo(21.489f, 28.517f, 20.523f, 31.147f, 20.009f, 33.991f)
                curveTo(18.674f, 33.725f, 17.277f, 33.623f, 15.818f, 33.684f)
                curveTo(14.38f, 33.725f, 13.024f, 33.93f, 11.75f, 34.298f)
                curveTo(12.264f, 30.328f, 13.445f, 26.625f, 15.294f, 23.187f)
                curveTo(17.164f, 19.749f, 19.547f, 16.741f, 22.444f, 14.163f)
                curveTo(25.362f, 11.585f, 28.649f, 9.569f, 32.306f, 8.117f)
                curveTo(35.984f, 6.664f, 39.888f, 5.938f, 44.018f, 5.938f)
                curveTo(48.476f, 5.938f, 52.667f, 6.787f, 56.592f, 8.485f)
                curveTo(60.516f, 10.163f, 63.978f, 12.496f, 66.978f, 15.483f)
                curveTo(69.978f, 18.471f, 72.32f, 21.918f, 74.005f, 25.826f)
                curveTo(75.71f, 29.735f, 76.563f, 33.909f, 76.563f, 38.349f)
                curveTo(76.563f, 42.79f, 75.72f, 46.964f, 74.035f, 50.872f)
                curveTo(72.351f, 54.78f, 70.008f, 58.228f, 67.009f, 61.215f)
                curveTo(64.029f, 64.203f, 60.567f, 66.536f, 56.623f, 68.214f)
                curveTo(52.698f, 69.912f, 48.496f, 70.761f, 44.018f, 70.761f)
                close()
                moveTo(44.018f, 44.426f)
                curveTo(42.004f, 44.426f, 40.196f, 43.915f, 38.593f, 42.892f)
                curveTo(36.991f, 41.848f, 35.707f, 40.447f, 34.741f, 38.687f)
                curveTo(33.796f, 36.927f, 33.323f, 34.963f, 33.323f, 32.794f)
                curveTo(33.303f, 30.727f, 33.765f, 28.834f, 34.71f, 27.116f)
                curveTo(35.676f, 25.376f, 36.97f, 23.985f, 38.593f, 22.941f)
                curveTo(40.216f, 21.898f, 42.025f, 21.376f, 44.018f, 21.376f)
                curveTo(46.011f, 21.376f, 47.808f, 21.898f, 49.411f, 22.941f)
                curveTo(51.034f, 23.985f, 52.318f, 25.376f, 53.263f, 27.116f)
                curveTo(54.208f, 28.834f, 54.681f, 30.727f, 54.681f, 32.794f)
                curveTo(54.681f, 34.983f, 54.208f, 36.958f, 53.263f, 38.718f)
                curveTo(52.318f, 40.477f, 51.034f, 41.879f, 49.411f, 42.923f)
                curveTo(47.808f, 43.946f, 46.011f, 44.447f, 44.018f, 44.426f)
                close()
                moveTo(16.804f, 70.577f)
                curveTo(14.585f, 70.577f, 12.49f, 70.157f, 10.517f, 69.318f)
                curveTo(8.565f, 68.479f, 6.85f, 67.313f, 5.37f, 65.82f)
                curveTo(3.87f, 64.326f, 2.699f, 62.597f, 1.857f, 60.632f)
                curveTo(0.994f, 58.688f, 0.563f, 56.612f, 0.563f, 54.402f)
                curveTo(0.563f, 52.192f, 0.994f, 50.115f, 1.857f, 48.171f)
                curveTo(2.699f, 46.227f, 3.87f, 44.508f, 5.37f, 43.015f)
                curveTo(6.85f, 41.521f, 8.565f, 40.354f, 10.517f, 39.516f)
                curveTo(12.49f, 38.677f, 14.585f, 38.257f, 16.804f, 38.257f)
                curveTo(19.023f, 38.257f, 21.109f, 38.677f, 23.06f, 39.516f)
                curveTo(25.033f, 40.354f, 26.759f, 41.521f, 28.238f, 43.015f)
                curveTo(29.738f, 44.488f, 30.909f, 46.207f, 31.751f, 48.171f)
                curveTo(32.594f, 50.115f, 33.015f, 52.192f, 33.015f, 54.402f)
                curveTo(33.015f, 56.612f, 32.594f, 58.688f, 31.751f, 60.632f)
                curveTo(30.909f, 62.597f, 29.738f, 64.315f, 28.238f, 65.789f)
                curveTo(26.738f, 67.283f, 25.002f, 68.449f, 23.03f, 69.288f)
                curveTo(21.078f, 70.147f, 19.003f, 70.577f, 16.804f, 70.577f)
                close()
                moveTo(9.408f, 57.379f)
                horizontalLineTo(17.02f)
                curveTo(17.739f, 57.379f, 18.345f, 57.133f, 18.838f, 56.642f)
                curveTo(19.331f, 56.151f, 19.578f, 55.548f, 19.578f, 54.831f)
                verticalLineTo(45.869f)
                curveTo(19.578f, 45.173f, 19.331f, 44.58f, 18.838f, 44.089f)
                curveTo(18.345f, 43.598f, 17.739f, 43.352f, 17.02f, 43.352f)
                curveTo(16.301f, 43.352f, 15.695f, 43.598f, 15.202f, 44.089f)
                curveTo(14.708f, 44.58f, 14.462f, 45.173f, 14.462f, 45.869f)
                verticalLineTo(52.284f)
                horizontalLineTo(9.408f)
                curveTo(8.688f, 52.284f, 8.072f, 52.529f, 7.558f, 53.021f)
                curveTo(7.065f, 53.512f, 6.819f, 54.115f, 6.819f, 54.831f)
                curveTo(6.819f, 55.548f, 7.065f, 56.151f, 7.558f, 56.642f)
                curveTo(8.072f, 57.133f, 8.688f, 57.379f, 9.408f, 57.379f)
                close()
            }
        }
            .build()
        return _avatarWaiting!!
    }

private var _avatarWaiting: ImageVector? = null

@Preview
@Composable
private fun PreviewAvatarWaiting() {
    Box(modifier = Modifier.padding(12.dp)) {
        Icon(imageVector = ChatIcons.AvatarWaiting, contentDescription = "AvatarWaiting", tint = ChatTheme.chatColors.token.brand.primary)
    }
}
