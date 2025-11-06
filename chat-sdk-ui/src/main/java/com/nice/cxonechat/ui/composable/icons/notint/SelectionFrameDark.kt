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
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nice.cxonechat.ui.composable.icons.ChatIcons

internal val ChatIcons.SelectionFrameDark: ImageVector
    get() {
        if (_selectionFrameDark != null) {
            return _selectionFrameDark!!
        }
        _selectionFrameDark = Builder(
            name = "Selection frame dark",
            defaultWidth = 32.0.dp,
            defaultHeight = 31.0.dp,
            viewportWidth = 32.0f,
            viewportHeight = 31.0f
        ).apply {
            path(
                fill = SolidColor(Color(0x00000000)),
                stroke = SolidColor(Color(0xFF333848)),
                strokeLineWidth = 3.0f,
                strokeLineCap = Butt,
                strokeLineJoin = Miter,
                strokeLineMiter = 4.0f,
                pathFillType = NonZero
            ) {
                moveTo(16.224f, 1.684f)
                curveTo(23.89f, 1.684f, 30.146f, 7.804f, 30.146f, 15.404f)
                curveTo(30.146f, 23.005f, 23.89f, 29.124f, 16.224f, 29.124f)
                curveTo(8.557f, 29.124f, 2.3f, 23.005f, 2.3f, 15.404f)
                curveTo(2.3f, 7.804f, 8.557f, 1.684f, 16.224f, 1.684f)
                close()
            }
            path(
                fill = SolidColor(Color(0xFF6680FF)),
                stroke = null,
                strokeLineWidth = 0.0f,
                strokeLineCap = Butt,
                strokeLineJoin = Miter,
                strokeLineMiter = 4.0f,
                pathFillType = NonZero
            ) {
                moveTo(3.8f, 15.404f)
                arcToRelative(12.424f, 12.22f, 0.0f, true, false, 24.847f, 0.0f)
                arcToRelative(12.424f, 12.22f, 0.0f, true, false, -24.847f, 0.0f)
                close()
            }
            path(
                fill = SolidColor(Color(0x00000000)),
                stroke = SolidColor(Color(0xFF030712)),
                strokeLineWidth = 2.1f,
                strokeLineCap = Butt,
                strokeLineJoin = Miter,
                strokeLineMiter = 4.0f,
                pathFillType = NonZero
            ) {
                moveTo(10.012f, 14.946f)
                lineTo(14.67f, 19.528f)
                lineTo(22.435f, 11.891f)
            }
        }.build()
        return _selectionFrameDark!!
    }

private var _selectionFrameDark: ImageVector? = null

@Preview
@Composable
private fun PreviewSelectionFrameDark() {
    Box(modifier = Modifier.padding(12.dp)) {
        Image(imageVector = ChatIcons.SelectionFrameDark, contentDescription = "")
    }
}
