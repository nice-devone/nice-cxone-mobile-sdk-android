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

internal val ChatIcons.MessageChatCircle: ImageVector
    get() {
        if (_messageChatCircle != null) {
            return _messageChatCircle!!
        }
        _messageChatCircle = Builder(
            name = "MessageChatCircle",
            defaultWidth = 32.0.dp,
            defaultHeight = 32.0.dp,
            viewportWidth = 32.0f,
            viewportHeight = 32.0f
        ).apply {
            path(
                fill = SolidColor(Color(0x00000000)),
                stroke = SolidColor(Color(0xFF254FE6)),
                strokeLineWidth = 1.5f,
                strokeLineCap = Round,
                strokeLineJoin = StrokeJoin.Companion.Round,
                strokeLineMiter = 4.0f,
                pathFillType = NonZero
            ) {
                moveTo(16.108f, 22.406f)
                curveTo(16.035f, 22.054f, 15.999f, 21.695f, 16.0f, 21.333f)
                verticalLineTo(21.02f)
                curveTo(16.072f, 19.712f, 16.624f, 18.477f, 17.55f, 17.55f)
                curveTo(18.476f, 16.624f, 19.712f, 16.072f, 21.02f, 16.0f)
                horizontalLineTo(21.333f)
                curveTo(21.696f, 15.999f, 22.056f, 16.036f, 22.408f, 16.108f)
                moveTo(16.108f, 22.406f)
                curveTo(16.201f, 22.86f, 16.354f, 23.301f, 16.565f, 23.718f)
                curveTo(17.007f, 24.604f, 17.688f, 25.348f, 18.53f, 25.869f)
                curveTo(19.372f, 26.39f, 20.343f, 26.666f, 21.333f, 26.667f)
                curveTo(22.015f, 26.669f, 22.689f, 26.538f, 23.318f, 26.283f)
                curveTo(23.583f, 26.176f, 23.869f, 26.128f, 24.15f, 26.175f)
                lineTo(24.939f, 26.259f)
                curveTo(25.699f, 26.34f, 26.34f, 25.699f, 26.259f, 24.939f)
                lineTo(26.175f, 24.15f)
                curveTo(26.128f, 23.869f, 26.176f, 23.583f, 26.283f, 23.318f)
                curveTo(26.538f, 22.689f, 26.668f, 22.015f, 26.667f, 21.333f)
                curveTo(26.666f, 20.343f, 26.39f, 19.372f, 25.869f, 18.53f)
                curveTo(25.348f, 17.688f, 24.603f, 17.007f, 23.718f, 16.565f)
                curveTo(23.302f, 16.355f, 22.861f, 16.202f, 22.408f, 16.108f)
                moveTo(16.108f, 22.406f)
                curveTo(15.422f, 22.578f, 14.714f, 22.666f, 14.0f, 22.667f)
                curveTo(12.892f, 22.67f, 11.797f, 22.457f, 10.774f, 22.044f)
                curveTo(10.345f, 21.87f, 9.879f, 21.791f, 9.423f, 21.868f)
                lineTo(7.545f, 22.068f)
                curveTo(6.616f, 22.167f, 5.832f, 21.383f, 5.932f, 20.454f)
                lineTo(6.132f, 18.577f)
                curveTo(6.209f, 18.121f, 6.13f, 17.655f, 5.957f, 17.226f)
                curveTo(5.543f, 16.203f, 5.33f, 15.108f, 5.333f, 14.0f)
                curveTo(5.334f, 12.391f, 5.783f, 10.814f, 6.629f, 9.445f)
                curveTo(7.475f, 8.076f, 8.686f, 6.97f, 10.125f, 6.251f)
                curveTo(11.327f, 5.644f, 12.654f, 5.33f, 14.0f, 5.333f)
                horizontalLineTo(14.51f)
                curveTo(16.635f, 5.451f, 18.642f, 6.348f, 20.147f, 7.853f)
                curveTo(21.652f, 9.358f, 22.549f, 11.365f, 22.667f, 13.49f)
                verticalLineTo(14.0f)
                curveTo(22.669f, 14.713f, 22.581f, 15.421f, 22.408f, 16.108f)
            }
        }
            .build()
        return _messageChatCircle!!
    }

private var _messageChatCircle: ImageVector? = null

@Preview
@Composable
private fun PreviewMessageChatCircle() {
    Box(modifier = Modifier.padding(12.dp)) {
        Image(imageVector = ChatIcons.MessageChatCircle, contentDescription = "")
    }
}
