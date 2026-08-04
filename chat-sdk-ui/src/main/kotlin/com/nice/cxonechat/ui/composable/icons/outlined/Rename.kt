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

internal val ChatIcons.Rename: ImageVector
    get() {
        if (rename != null) {
            return rename!!
        }
        rename = Builder(
            name = "TextField", defaultWidth = 24.0.dp, defaultHeight = 24.0.dp,
            viewportWidth = 24.0f, viewportHeight = 24.0f
        ).apply {
            path(
                fill = SolidColor(Color(0xFF666A76)), stroke = null, strokeLineWidth = 0.0f,
                strokeLineCap = Butt, strokeLineJoin = Miter, strokeLineMiter = 4.0f,
                pathFillType = NonZero
            ) {
                moveTo(2.0f, 21.0f)
                verticalLineTo(19.0f)
                horizontalLineTo(22.0f)
                verticalLineTo(21.0f)
                horizontalLineTo(2.0f)
                close()
                moveTo(19.0f, 17.0f)
                verticalLineTo(3.0f)
                horizontalLineTo(20.5f)
                verticalLineTo(17.0f)
                horizontalLineTo(19.0f)
                close()
                moveTo(4.0f, 17.0f)
                lineTo(9.25f, 3.0f)
                horizontalLineTo(11.75f)
                lineTo(17.0f, 17.0f)
                horizontalLineTo(14.6f)
                lineTo(13.35f, 13.4f)
                horizontalLineTo(7.7f)
                lineTo(6.4f, 17.0f)
                horizontalLineTo(4.0f)
                close()
                moveTo(8.4f, 11.4f)
                horizontalLineTo(12.6f)
                lineTo(10.55f, 5.6f)
                horizontalLineTo(10.45f)
                lineTo(8.4f, 11.4f)
                close()
            }
        }
            .build()
        return rename!!
    }

private var rename: ImageVector? = null

@Preview
@Composable
private fun PreviewRename() {
    Box(modifier = Modifier.padding(12.dp)) {
        Image(imageVector = ChatIcons.Rename, contentDescription = "")
    }
}
