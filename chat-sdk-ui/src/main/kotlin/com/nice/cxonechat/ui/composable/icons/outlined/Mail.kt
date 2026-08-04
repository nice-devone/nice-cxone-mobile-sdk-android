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
            name = "Mail", defaultWidth = 24.0.dp, defaultHeight = 24.0.dp,
            viewportWidth = 24.0f, viewportHeight = 24.0f
        ).apply {
            path(
                fill = SolidColor(Color(0xFF666A76)), stroke = null, strokeLineWidth = 0.0f,
                strokeLineCap = Butt, strokeLineJoin = Miter, strokeLineMiter = 4.0f,
                pathFillType = NonZero
            ) {
                moveTo(4.0f, 20.0f)
                curveTo(3.45f, 20.0f, 2.979f, 19.804f, 2.588f, 19.413f)
                curveTo(2.196f, 19.021f, 2.0f, 18.55f, 2.0f, 18.0f)
                verticalLineTo(6.0f)
                curveTo(2.0f, 5.45f, 2.196f, 4.979f, 2.588f, 4.588f)
                curveTo(2.979f, 4.196f, 3.45f, 4.0f, 4.0f, 4.0f)
                horizontalLineTo(20.0f)
                curveTo(20.55f, 4.0f, 21.021f, 4.196f, 21.413f, 4.588f)
                curveTo(21.804f, 4.979f, 22.0f, 5.45f, 22.0f, 6.0f)
                verticalLineTo(18.0f)
                curveTo(22.0f, 18.55f, 21.804f, 19.021f, 21.413f, 19.413f)
                curveTo(21.021f, 19.804f, 20.55f, 20.0f, 20.0f, 20.0f)
                horizontalLineTo(4.0f)
                close()
                moveTo(12.0f, 13.0f)
                lineTo(4.0f, 8.0f)
                verticalLineTo(18.0f)
                horizontalLineTo(20.0f)
                verticalLineTo(8.0f)
                lineTo(12.0f, 13.0f)
                close()
                moveTo(12.0f, 11.0f)
                lineTo(20.0f, 6.0f)
                horizontalLineTo(4.0f)
                lineTo(12.0f, 11.0f)
                close()
                moveTo(4.0f, 8.0f)
                verticalLineTo(6.0f)
                verticalLineTo(18.0f)
                verticalLineTo(8.0f)
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
