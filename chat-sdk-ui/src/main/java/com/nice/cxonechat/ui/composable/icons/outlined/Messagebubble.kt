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

internal val ChatIcons.MessageBubble: ImageVector
    get() {
        if (messageBubble != null) {
            return messageBubble!!
        }
        messageBubble = Builder(
            name = "MessageBubble",
            defaultWidth = 29.0.dp,
            defaultHeight = 23.0.dp,
            viewportWidth = 29.0f,
            viewportHeight = 23.0f
        ).apply {
            path(
                fill = SolidColor(Color(0xFF254FE6)), stroke = null, strokeLineWidth = 0.0f,
                strokeLineCap = Butt, strokeLineJoin = Miter, strokeLineMiter = 4.0f,
                pathFillType = NonZero
            ) {
                moveTo(16.338f, 10.996f)
                curveTo(16.11f, 10.996f, 15.921f, 10.921f, 15.771f, 10.771f)
                curveTo(15.628f, 10.615f, 15.557f, 10.433f, 15.557f, 10.225f)
                curveTo(15.557f, 9.997f, 15.628f, 9.808f, 15.771f, 9.658f)
                curveTo(15.921f, 9.508f, 16.11f, 9.434f, 16.338f, 9.434f)
                horizontalLineTo(23.066f)
                curveTo(23.288f, 9.434f, 23.473f, 9.508f, 23.623f, 9.658f)
                curveTo(23.773f, 9.801f, 23.848f, 9.99f, 23.848f, 10.225f)
                curveTo(23.848f, 10.433f, 23.773f, 10.615f, 23.623f, 10.771f)
                curveTo(23.473f, 10.921f, 23.288f, 10.996f, 23.066f, 10.996f)
                horizontalLineTo(16.338f)
                close()
                moveTo(16.338f, 14.561f)
                curveTo(16.11f, 14.561f, 15.921f, 14.489f, 15.771f, 14.346f)
                curveTo(15.628f, 14.196f, 15.557f, 14.01f, 15.557f, 13.789f)
                curveTo(15.557f, 13.568f, 15.628f, 13.379f, 15.771f, 13.223f)
                curveTo(15.921f, 13.066f, 16.11f, 12.988f, 16.338f, 12.988f)
                horizontalLineTo(21.221f)
                curveTo(21.442f, 12.988f, 21.628f, 13.066f, 21.777f, 13.223f)
                curveTo(21.927f, 13.379f, 22.002f, 13.568f, 22.002f, 13.789f)
                curveTo(22.002f, 14.01f, 21.927f, 14.196f, 21.777f, 14.346f)
                curveTo(21.628f, 14.489f, 21.442f, 14.561f, 21.221f, 14.561f)
                horizontalLineTo(16.338f)
                close()
                moveTo(12.5f, 16.748f)
                horizontalLineTo(11.133f)
                lineTo(7.793f, 19.717f)
                curveTo(7.461f, 20.01f, 7.174f, 20.225f, 6.934f, 20.361f)
                curveTo(6.699f, 20.498f, 6.445f, 20.566f, 6.172f, 20.566f)
                curveTo(5.775f, 20.566f, 5.465f, 20.44f, 5.244f, 20.185f)
                curveTo(5.029f, 19.938f, 4.922f, 19.596f, 4.922f, 19.16f)
                verticalLineTo(16.748f)
                horizontalLineTo(4.521f)
                curveTo(3.577f, 16.748f, 2.767f, 16.569f, 2.09f, 16.211f)
                curveTo(1.419f, 15.846f, 0.902f, 15.322f, 0.537f, 14.639f)
                curveTo(0.179f, 13.955f, 0.0f, 13.132f, 0.0f, 12.168f)
                verticalLineTo(4.58f)
                curveTo(0.0f, 3.617f, 0.179f, 2.793f, 0.537f, 2.109f)
                curveTo(0.895f, 1.426f, 1.413f, 0.905f, 2.09f, 0.547f)
                curveTo(2.773f, 0.182f, 3.607f, 0.0f, 4.59f, 0.0f)
                horizontalLineTo(17.041f)
                curveTo(18.024f, 0.0f, 18.854f, 0.182f, 19.531f, 0.547f)
                curveTo(20.215f, 0.905f, 20.736f, 1.426f, 21.094f, 2.109f)
                curveTo(21.452f, 2.793f, 21.631f, 3.617f, 21.631f, 4.58f)
                verticalLineTo(6.191f)
                horizontalLineTo(19.707f)
                verticalLineTo(4.648f)
                curveTo(19.707f, 3.75f, 19.482f, 3.07f, 19.033f, 2.607f)
                curveTo(18.59f, 2.145f, 17.91f, 1.914f, 16.992f, 1.914f)
                horizontalLineTo(4.639f)
                curveTo(3.721f, 1.914f, 3.037f, 2.145f, 2.588f, 2.607f)
                curveTo(2.145f, 3.07f, 1.924f, 3.75f, 1.924f, 4.648f)
                verticalLineTo(12.1f)
                curveTo(1.924f, 12.998f, 2.145f, 13.678f, 2.588f, 14.141f)
                curveTo(3.037f, 14.596f, 3.721f, 14.824f, 4.639f, 14.824f)
                horizontalLineTo(5.85f)
                curveTo(6.123f, 14.824f, 6.322f, 14.886f, 6.445f, 15.01f)
                curveTo(6.576f, 15.134f, 6.641f, 15.342f, 6.641f, 15.635f)
                verticalLineTo(18.408f)
                lineTo(9.746f, 15.352f)
                curveTo(9.948f, 15.137f, 10.137f, 14.997f, 10.313f, 14.932f)
                curveTo(10.495f, 14.86f, 10.736f, 14.824f, 11.035f, 14.824f)
                horizontalLineTo(11.816f)
                lineTo(12.5f, 16.748f)
                close()
                moveTo(15.068f, 18.975f)
                curveTo(14.131f, 18.975f, 13.337f, 18.809f, 12.686f, 18.477f)
                curveTo(12.041f, 18.145f, 11.55f, 17.663f, 11.211f, 17.031f)
                curveTo(10.879f, 16.4f, 10.713f, 15.632f, 10.713f, 14.727f)
                verticalLineTo(9.189f)
                curveTo(10.713f, 8.271f, 10.879f, 7.49f, 11.211f, 6.846f)
                curveTo(11.55f, 6.201f, 12.041f, 5.713f, 12.686f, 5.381f)
                curveTo(13.337f, 5.042f, 14.131f, 4.873f, 15.068f, 4.873f)
                horizontalLineTo(24.277f)
                curveTo(25.215f, 4.873f, 26.006f, 5.042f, 26.65f, 5.381f)
                curveTo(27.295f, 5.713f, 27.783f, 6.201f, 28.115f, 6.846f)
                curveTo(28.454f, 7.49f, 28.623f, 8.271f, 28.623f, 9.189f)
                verticalLineTo(14.727f)
                curveTo(28.623f, 15.632f, 28.457f, 16.4f, 28.125f, 17.031f)
                curveTo(27.799f, 17.663f, 27.318f, 18.145f, 26.68f, 18.477f)
                curveTo(26.042f, 18.809f, 25.264f, 18.975f, 24.346f, 18.975f)
                horizontalLineTo(24.16f)
                verticalLineTo(21.211f)
                curveTo(24.16f, 21.641f, 24.049f, 21.982f, 23.828f, 22.236f)
                curveTo(23.613f, 22.49f, 23.31f, 22.617f, 22.92f, 22.617f)
                curveTo(22.666f, 22.617f, 22.412f, 22.542f, 22.158f, 22.393f)
                curveTo(21.911f, 22.249f, 21.628f, 22.041f, 21.309f, 21.768f)
                lineTo(18.057f, 18.975f)
                horizontalLineTo(15.068f)
                close()
                moveTo(15.215f, 17.1f)
                horizontalLineTo(18.018f)
                curveTo(18.252f, 17.1f, 18.463f, 17.142f, 18.652f, 17.227f)
                curveTo(18.848f, 17.305f, 19.04f, 17.432f, 19.229f, 17.607f)
                lineTo(22.441f, 20.508f)
                verticalLineTo(17.969f)
                curveTo(22.441f, 17.676f, 22.52f, 17.458f, 22.676f, 17.315f)
                curveTo(22.838f, 17.171f, 23.037f, 17.1f, 23.271f, 17.1f)
                horizontalLineTo(24.131f)
                curveTo(25.016f, 17.1f, 25.671f, 16.882f, 26.094f, 16.445f)
                curveTo(26.517f, 16.009f, 26.729f, 15.361f, 26.729f, 14.502f)
                verticalLineTo(9.336f)
                curveTo(26.729f, 8.483f, 26.517f, 7.839f, 26.094f, 7.402f)
                curveTo(25.671f, 6.966f, 25.016f, 6.748f, 24.131f, 6.748f)
                horizontalLineTo(15.215f)
                curveTo(14.323f, 6.748f, 13.665f, 6.966f, 13.242f, 7.402f)
                curveTo(12.825f, 7.839f, 12.617f, 8.483f, 12.617f, 9.336f)
                verticalLineTo(14.512f)
                curveTo(12.617f, 15.365f, 12.825f, 16.009f, 13.242f, 16.445f)
                curveTo(13.665f, 16.882f, 14.323f, 17.1f, 15.215f, 17.1f)
                close()
            }
        }
            .build()
        return messageBubble!!
    }

private var messageBubble: ImageVector? = null

@Preview
@Composable
private fun PreviewMessageBubble() {
    Box(modifier = Modifier.padding(12.dp)) {
        Image(imageVector = ChatIcons.MessageBubble, contentDescription = "")
    }
}
