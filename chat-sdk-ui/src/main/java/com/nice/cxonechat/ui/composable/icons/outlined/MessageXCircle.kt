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

internal val ChatIcons.MessageXCircle: ImageVector
    get() {
        if (_messageXCircle != null) {
            return _messageXCircle!!
        }
        _messageXCircle = Builder(
            name = "MessageXCircle",
            defaultWidth = 32.0.dp,
            defaultHeight = 32.0.dp,
            viewportWidth = 32.0f,
            viewportHeight = 32.0f
        ).apply {
            path(
                fill = SolidColor(Color(0xFF38000D)),
                stroke = null,
                strokeLineWidth = 0.0f,
                strokeLineCap = Butt,
                strokeLineJoin = Miter,
                strokeLineMiter = 4.0f,
                pathFillType = NonZero
            ) {
                moveTo(19.547f, 13.513f)
                curveTo(19.84f, 13.22f, 19.84f, 12.746f, 19.547f, 12.453f)
                curveTo(19.254f, 12.16f, 18.78f, 12.16f, 18.487f, 12.453f)
                lineTo(19.547f, 13.513f)
                close()
                moveTo(12.453f, 18.487f)
                curveTo(12.16f, 18.78f, 12.16f, 19.254f, 12.453f, 19.547f)
                curveTo(12.746f, 19.84f, 13.22f, 19.84f, 13.513f, 19.547f)
                lineTo(12.453f, 18.487f)
                close()
                moveTo(18.487f, 19.547f)
                curveTo(18.78f, 19.84f, 19.254f, 19.84f, 19.547f, 19.547f)
                curveTo(19.84f, 19.254f, 19.84f, 18.78f, 19.547f, 18.487f)
                lineTo(18.487f, 19.547f)
                close()
                moveTo(13.513f, 12.453f)
                curveTo(13.22f, 12.16f, 12.746f, 12.16f, 12.453f, 12.453f)
                curveTo(12.16f, 12.746f, 12.16f, 13.22f, 12.453f, 13.513f)
                lineTo(13.513f, 12.453f)
                close()
                moveTo(26.667f, 16.0f)
                horizontalLineTo(25.917f)
                lineTo(25.917f, 16.002f)
                lineTo(26.667f, 16.0f)
                close()
                moveTo(25.537f, 20.769f)
                lineTo(24.868f, 20.43f)
                lineTo(24.866f, 20.433f)
                lineTo(25.537f, 20.769f)
                close()
                moveTo(16.0f, 26.667f)
                lineTo(16.0f, 25.917f)
                lineTo(15.998f, 25.917f)
                lineTo(16.0f, 26.667f)
                close()
                moveTo(5.333f, 16.0f)
                lineTo(6.083f, 16.002f)
                lineTo(6.083f, 16.0f)
                lineTo(5.333f, 16.0f)
                close()
                moveTo(11.231f, 6.463f)
                lineTo(11.567f, 7.134f)
                lineTo(11.57f, 7.132f)
                lineTo(11.231f, 6.463f)
                close()
                moveTo(16.0f, 5.333f)
                lineTo(15.998f, 6.083f)
                horizontalLineTo(16.0f)
                verticalLineTo(5.333f)
                close()
                moveTo(16.628f, 5.333f)
                lineTo(16.669f, 4.584f)
                curveTo(16.655f, 4.584f, 16.641f, 4.583f, 16.628f, 4.583f)
                verticalLineTo(5.333f)
                close()
                moveTo(26.667f, 15.373f)
                horizontalLineTo(27.417f)
                curveTo(27.417f, 15.359f, 27.416f, 15.345f, 27.416f, 15.331f)
                lineTo(26.667f, 15.373f)
                close()
                moveTo(6.316f, 21.633f)
                lineTo(5.577f, 21.508f)
                curveTo(5.574f, 21.523f, 5.572f, 21.538f, 5.571f, 21.554f)
                lineTo(6.316f, 21.633f)
                close()
                moveTo(6.1f, 19.97f)
                lineTo(6.796f, 19.689f)
                lineTo(6.1f, 19.97f)
                close()
                moveTo(12.03f, 25.9f)
                lineTo(11.749f, 26.595f)
                lineTo(12.03f, 25.9f)
                close()
                moveTo(10.367f, 25.684f)
                lineTo(10.446f, 26.43f)
                curveTo(10.461f, 26.428f, 10.477f, 26.426f, 10.492f, 26.423f)
                lineTo(10.367f, 25.684f)
                close()
                moveTo(7.639f, 25.974f)
                lineTo(7.56f, 25.228f)
                lineTo(7.639f, 25.974f)
                close()
                moveTo(6.025f, 24.36f)
                lineTo(6.771f, 24.44f)
                lineTo(6.025f, 24.36f)
                close()
                moveTo(19.017f, 12.983f)
                lineTo(18.487f, 12.453f)
                lineTo(15.47f, 15.47f)
                lineTo(16.0f, 16.0f)
                lineTo(16.53f, 16.53f)
                lineTo(19.547f, 13.513f)
                lineTo(19.017f, 12.983f)
                close()
                moveTo(16.0f, 16.0f)
                lineTo(15.47f, 15.47f)
                lineTo(12.453f, 18.487f)
                lineTo(12.983f, 19.017f)
                lineTo(13.513f, 19.547f)
                lineTo(16.53f, 16.53f)
                lineTo(16.0f, 16.0f)
                close()
                moveTo(19.017f, 19.017f)
                lineTo(19.547f, 18.487f)
                lineTo(16.53f, 15.47f)
                lineTo(16.0f, 16.0f)
                lineTo(15.47f, 16.53f)
                lineTo(18.487f, 19.547f)
                lineTo(19.017f, 19.017f)
                close()
                moveTo(16.0f, 16.0f)
                lineTo(16.53f, 15.47f)
                lineTo(13.513f, 12.453f)
                lineTo(12.983f, 12.983f)
                lineTo(12.453f, 13.513f)
                lineTo(15.47f, 16.53f)
                lineTo(16.0f, 16.0f)
                close()
                moveTo(26.667f, 16.0f)
                lineTo(25.917f, 16.002f)
                curveTo(25.921f, 17.54f, 25.561f, 19.058f, 24.868f, 20.431f)
                lineTo(25.537f, 20.769f)
                lineTo(26.207f, 21.107f)
                curveTo(27.007f, 19.523f, 27.421f, 17.772f, 27.417f, 15.998f)
                lineTo(26.667f, 16.0f)
                close()
                moveTo(25.537f, 20.769f)
                lineTo(24.866f, 20.433f)
                curveTo(24.043f, 22.08f, 22.778f, 23.466f, 21.212f, 24.434f)
                lineTo(21.606f, 25.072f)
                lineTo(22.001f, 25.71f)
                curveTo(23.804f, 24.595f, 25.26f, 23.0f, 26.208f, 21.104f)
                lineTo(25.537f, 20.769f)
                close()
                moveTo(21.606f, 25.072f)
                lineTo(21.212f, 24.434f)
                curveTo(19.646f, 25.403f, 17.841f, 25.916f, 16.0f, 25.917f)
                lineTo(16.0f, 26.667f)
                lineTo(16.0f, 27.417f)
                curveTo(18.12f, 27.416f, 20.198f, 26.825f, 22.001f, 25.71f)
                lineTo(21.606f, 25.072f)
                close()
                moveTo(16.0f, 26.667f)
                lineTo(15.998f, 25.917f)
                curveTo(14.732f, 25.92f, 13.48f, 25.677f, 12.311f, 25.204f)
                lineTo(12.03f, 25.9f)
                lineTo(11.749f, 26.595f)
                curveTo(13.097f, 27.14f, 14.542f, 27.42f, 16.002f, 27.417f)
                lineTo(16.0f, 26.667f)
                close()
                moveTo(10.367f, 25.684f)
                lineTo(10.287f, 24.938f)
                lineTo(7.56f, 25.228f)
                lineTo(7.639f, 25.974f)
                lineTo(7.718f, 26.72f)
                lineTo(10.446f, 26.43f)
                lineTo(10.367f, 25.684f)
                close()
                moveTo(6.025f, 24.36f)
                lineTo(6.771f, 24.44f)
                lineTo(7.062f, 21.713f)
                lineTo(6.316f, 21.633f)
                lineTo(5.571f, 21.554f)
                lineTo(5.28f, 24.281f)
                lineTo(6.025f, 24.36f)
                close()
                moveTo(6.1f, 19.97f)
                lineTo(6.796f, 19.689f)
                curveTo(6.323f, 18.52f, 6.08f, 17.268f, 6.083f, 16.002f)
                lineTo(5.333f, 16.0f)
                lineTo(4.583f, 15.998f)
                curveTo(4.58f, 17.458f, 4.86f, 18.903f, 5.405f, 20.251f)
                lineTo(6.1f, 19.97f)
                close()
                moveTo(5.333f, 16.0f)
                lineTo(6.083f, 16.0f)
                curveTo(6.084f, 14.159f, 6.597f, 12.354f, 7.566f, 10.788f)
                lineTo(6.928f, 10.394f)
                lineTo(6.29f, 9.999f)
                curveTo(5.175f, 11.802f, 4.584f, 13.88f, 4.583f, 16.0f)
                lineTo(5.333f, 16.0f)
                close()
                moveTo(6.928f, 10.394f)
                lineTo(7.566f, 10.788f)
                curveTo(8.534f, 9.222f, 9.92f, 7.957f, 11.567f, 7.134f)
                lineTo(11.231f, 6.463f)
                lineTo(10.896f, 5.792f)
                curveTo(9.0f, 6.74f, 7.405f, 8.196f, 6.29f, 9.999f)
                lineTo(6.928f, 10.394f)
                close()
                moveTo(11.231f, 6.463f)
                lineTo(11.57f, 7.132f)
                curveTo(12.943f, 6.439f, 14.46f, 6.079f, 15.998f, 6.083f)
                lineTo(16.0f, 5.333f)
                lineTo(16.002f, 4.583f)
                curveTo(14.228f, 4.579f, 12.477f, 4.993f, 10.893f, 5.793f)
                lineTo(11.231f, 6.463f)
                close()
                moveTo(16.0f, 5.333f)
                verticalLineTo(6.083f)
                horizontalLineTo(16.628f)
                verticalLineTo(5.333f)
                verticalLineTo(4.583f)
                horizontalLineTo(16.0f)
                verticalLineTo(5.333f)
                close()
                moveTo(16.628f, 5.333f)
                lineTo(16.586f, 6.082f)
                curveTo(19.017f, 6.216f, 21.314f, 7.243f, 23.036f, 8.964f)
                lineTo(23.566f, 8.434f)
                lineTo(24.096f, 7.904f)
                curveTo(22.113f, 5.921f, 19.469f, 4.739f, 16.669f, 4.584f)
                lineTo(16.628f, 5.333f)
                close()
                moveTo(23.566f, 8.434f)
                lineTo(23.036f, 8.964f)
                curveTo(24.757f, 10.686f, 25.784f, 12.983f, 25.918f, 15.414f)
                lineTo(26.667f, 15.373f)
                lineTo(27.416f, 15.331f)
                curveTo(27.261f, 12.531f, 26.079f, 9.887f, 24.096f, 7.904f)
                lineTo(23.566f, 8.434f)
                close()
                moveTo(26.667f, 15.373f)
                horizontalLineTo(25.917f)
                verticalLineTo(16.0f)
                horizontalLineTo(26.667f)
                horizontalLineTo(27.417f)
                verticalLineTo(15.373f)
                horizontalLineTo(26.667f)
                close()
                moveTo(6.316f, 21.633f)
                lineTo(7.056f, 21.759f)
                curveTo(7.179f, 21.03f, 7.049f, 20.315f, 6.796f, 19.689f)
                lineTo(6.1f, 19.97f)
                lineTo(5.405f, 20.251f)
                curveTo(5.579f, 20.682f, 5.644f, 21.113f, 5.577f, 21.508f)
                lineTo(6.316f, 21.633f)
                close()
                moveTo(12.03f, 25.9f)
                lineTo(12.311f, 25.204f)
                curveTo(11.686f, 24.951f, 10.97f, 24.821f, 10.241f, 24.944f)
                lineTo(10.367f, 25.684f)
                lineTo(10.492f, 26.423f)
                curveTo(10.887f, 26.356f, 11.318f, 26.421f, 11.749f, 26.595f)
                lineTo(12.03f, 25.9f)
                close()
                moveTo(7.639f, 25.974f)
                lineTo(7.56f, 25.228f)
                curveTo(7.105f, 25.277f, 6.723f, 24.894f, 6.771f, 24.44f)
                lineTo(6.025f, 24.36f)
                lineTo(5.28f, 24.281f)
                curveTo(5.13f, 25.685f, 6.314f, 26.87f, 7.718f, 26.72f)
                lineTo(7.639f, 25.974f)
                close()
            }
        }
            .build()
        return _messageXCircle!!
    }

private var _messageXCircle: ImageVector? = null

@Preview
@Composable
private fun PreviewMessageXCircle() {
    Box(modifier = Modifier.padding(12.dp)) {
        Image(imageVector = ChatIcons.MessageXCircle, contentDescription = "")
    }
}
