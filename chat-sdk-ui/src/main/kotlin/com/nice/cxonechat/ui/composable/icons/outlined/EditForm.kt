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

internal val ChatIcons.EditForm: ImageVector
    get() {
        if (edit != null) {
            return edit!!
        }
        edit = Builder(
            name = "Edit", defaultWidth = 24.0.dp, defaultHeight = 24.0.dp,
            viewportWidth = 24.0f, viewportHeight = 24.0f
        ).apply {
            path(
                fill = SolidColor(Color(0xFF666A76)), stroke = null, strokeLineWidth = 0.0f,
                strokeLineCap = Butt, strokeLineJoin = Miter, strokeLineMiter = 4.0f,
                pathFillType = NonZero
            ) {
                moveTo(14.0f, 22.0f)
                verticalLineTo(18.925f)
                lineTo(19.525f, 13.425f)
                curveTo(19.675f, 13.275f, 19.842f, 13.167f, 20.025f, 13.1f)
                curveTo(20.208f, 13.033f, 20.392f, 13.0f, 20.575f, 13.0f)
                curveTo(20.775f, 13.0f, 20.967f, 13.038f, 21.15f, 13.113f)
                curveTo(21.333f, 13.188f, 21.5f, 13.3f, 21.65f, 13.45f)
                lineTo(22.575f, 14.375f)
                curveTo(22.708f, 14.525f, 22.813f, 14.692f, 22.888f, 14.875f)
                curveTo(22.962f, 15.058f, 23.0f, 15.242f, 23.0f, 15.425f)
                curveTo(23.0f, 15.608f, 22.967f, 15.796f, 22.9f, 15.988f)
                curveTo(22.833f, 16.179f, 22.725f, 16.35f, 22.575f, 16.5f)
                lineTo(17.075f, 22.0f)
                horizontalLineTo(14.0f)
                close()
                moveTo(15.5f, 20.5f)
                horizontalLineTo(16.45f)
                lineTo(19.475f, 17.45f)
                lineTo(19.025f, 16.975f)
                lineTo(18.55f, 16.525f)
                lineTo(15.5f, 19.55f)
                verticalLineTo(20.5f)
                close()
                moveTo(6.0f, 22.0f)
                curveTo(5.45f, 22.0f, 4.979f, 21.804f, 4.588f, 21.413f)
                curveTo(4.196f, 21.021f, 4.0f, 20.55f, 4.0f, 20.0f)
                verticalLineTo(4.0f)
                curveTo(4.0f, 3.45f, 4.196f, 2.979f, 4.588f, 2.588f)
                curveTo(4.979f, 2.196f, 5.45f, 2.0f, 6.0f, 2.0f)
                horizontalLineTo(14.0f)
                lineTo(20.0f, 8.0f)
                verticalLineTo(11.0f)
                horizontalLineTo(18.0f)
                verticalLineTo(9.0f)
                horizontalLineTo(13.0f)
                verticalLineTo(4.0f)
                horizontalLineTo(6.0f)
                verticalLineTo(20.0f)
                horizontalLineTo(12.0f)
                verticalLineTo(22.0f)
                horizontalLineTo(6.0f)
                close()
                moveTo(19.025f, 16.975f)
                lineTo(18.55f, 16.525f)
                lineTo(19.475f, 17.45f)
                lineTo(19.025f, 16.975f)
                close()
            }
        }
            .build()
        return edit!!
    }

private var edit: ImageVector? = null

@Preview
@Composable
private fun PreviewEditForm() {
    Box(modifier = Modifier.padding(12.dp)) {
        Image(imageVector = ChatIcons.EditForm, contentDescription = "")
    }
}
