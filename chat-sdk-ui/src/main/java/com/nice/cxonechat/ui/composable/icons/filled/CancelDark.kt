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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.nice.cxonechat.ui.composable.icons.ChatIcons
import com.nice.cxonechat.ui.composable.theme.ChatTheme

internal val ChatIcons.CancelDark: ImageVector
    get() {
        if (_Cancel != null) return _Cancel!!
        _Cancel = ImageVector.Builder(
            name = "CancelDark",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 24.0f,
            viewportHeight = 24.0f
        ).apply {
            path(
                fill = SolidColor(Color(0xFFFF9175)),
                stroke = null,
                strokeLineWidth = 0.0f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Companion.Miter,
                strokeLineMiter = 4.0f,
                pathFillType = PathFillType.Companion.NonZero
            ) {
                moveTo(22.667f, 12.0f)
                curveTo(22.667f, 17.891f, 17.891f, 22.667f, 12.0f, 22.667f)
                curveTo(6.109f, 22.667f, 1.333f, 17.891f, 1.333f, 12.0f)
                curveTo(1.333f, 6.109f, 6.109f, 1.333f, 12.0f, 1.333f)
                curveTo(17.891f, 1.333f, 22.667f, 6.109f, 22.667f, 12.0f)
                close()
            }
            path(
                fill = SolidColor(Color(0x00000000)),
                stroke = SolidColor(Color(0xFFffffff)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Companion.Round,
                strokeLineJoin = StrokeJoin.Companion.Miter,
                strokeLineMiter = 4.0f,
                pathFillType = PathFillType.Companion.NonZero
            ) {
                moveTo(15.017f, 8.983f)
                lineTo(12.0f, 12.0f)
                moveTo(12.0f, 12.0f)
                lineTo(8.983f, 15.017f)
                moveTo(12.0f, 12.0f)
                lineTo(15.017f, 15.017f)
                moveTo(12.0f, 12.0f)
                lineTo(8.983f, 8.983f)
                moveTo(22.667f, 12.0f)
                curveTo(22.667f, 17.891f, 17.891f, 22.667f, 12.0f, 22.667f)
                curveTo(6.109f, 22.667f, 1.333f, 17.891f, 1.333f, 12.0f)
                curveTo(1.333f, 6.109f, 6.109f, 1.333f, 12.0f, 1.333f)
                curveTo(17.891f, 1.333f, 22.667f, 6.109f, 22.667f, 12.0f)
                close()
            }
        }
            .build()
        return _Cancel!!
    }

private var _Cancel: ImageVector? = null

@PreviewLightDark
@Composable
private fun Preview() {
    ChatTheme {
        Surface(modifier = Modifier.padding(12.dp)) {
            Image(imageVector = ChatIcons.Cancel, contentDescription = "", modifier = Modifier.size(32.dp))
        }
    }
}
