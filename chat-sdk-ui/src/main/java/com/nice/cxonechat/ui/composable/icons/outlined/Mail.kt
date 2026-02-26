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

internal val ChatIcons.Mail: ImageVector
    get() {
        if (mail != null) {
            return mail!!
        }
        mail = Builder(
            name = "Mail", defaultWidth = 32.0.dp, defaultHeight = 32.0.dp,
            viewportWidth = 32.0f, viewportHeight = 32.0f
        ).apply {
            path(
                fill = SolidColor(Color(0xFF254FE6)), stroke = null, strokeLineWidth = 0.0f,
                strokeLineCap = Butt, strokeLineJoin = Miter, strokeLineMiter = 4.0f,
                pathFillType = NonZero
            ) {
                moveTo(7.835f, 24.165f)
                curveTo(7.274f, 24.165f, 6.793f, 23.965f, 6.393f, 23.565f)
                curveTo(5.994f, 23.166f, 5.794f, 22.685f, 5.794f, 22.124f)
                verticalLineTo(9.876f)
                curveTo(5.794f, 9.315f, 5.994f, 8.834f, 6.393f, 8.435f)
                curveTo(6.793f, 8.035f, 7.274f, 7.835f, 7.835f, 7.835f)
                horizontalLineTo(24.165f)
                curveTo(24.726f, 7.835f, 25.207f, 8.035f, 25.607f, 8.435f)
                curveTo(26.006f, 8.834f, 26.206f, 9.315f, 26.206f, 9.876f)
                verticalLineTo(22.124f)
                curveTo(26.206f, 22.685f, 26.006f, 23.166f, 25.607f, 23.565f)
                curveTo(25.207f, 23.965f, 24.726f, 24.165f, 24.165f, 24.165f)
                horizontalLineTo(7.835f)
                close()
                moveTo(16.0f, 17.021f)
                lineTo(7.835f, 11.917f)
                verticalLineTo(22.124f)
                horizontalLineTo(24.165f)
                verticalLineTo(11.917f)
                lineTo(16.0f, 17.021f)
                close()
                moveTo(16.0f, 14.979f)
                lineTo(24.165f, 9.876f)
                horizontalLineTo(7.835f)
                lineTo(16.0f, 14.979f)
                close()
                moveTo(7.835f, 11.917f)
                verticalLineTo(9.876f)
                verticalLineTo(22.124f)
                verticalLineTo(11.917f)
                close()
            }
        }
            .build()
        return mail!!
    }

private var mail: ImageVector? = null

@Preview
@Composable
private fun PreviewMail() {
    Box(modifier = Modifier.padding(12.dp)) {
        Image(imageVector = ChatIcons.Mail, contentDescription = "")
    }
}
