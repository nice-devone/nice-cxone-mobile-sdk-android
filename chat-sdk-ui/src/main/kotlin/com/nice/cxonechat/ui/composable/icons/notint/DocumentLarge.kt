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
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nice.cxonechat.ui.composable.icons.ChatIcons

/**
 * Icon representing a document, used in the fallback thumbnail.
 * This version is intended for views with enough space to display this larger icon.
 * The icon is not intended for tinting (while it is technically a two-tone).
 */
internal val ChatIcons.DocumentLarge: ImageVector
    get() {
        if (_documentLarge != null) {
            return _documentLarge!!
        }
        _documentLarge = Builder(
            name = "DocumentLarge",
            defaultWidth = 126.0.dp,
            defaultHeight = 165.0.dp,
            viewportWidth = 126.0f,
            viewportHeight = 165.0f
        ).apply {
            group {
                path(
                    fill = SolidColor(Color(0xFFD4D5D8)),
                    stroke = null,
                    strokeLineWidth = 0.0f,
                    strokeLineCap = Butt,
                    strokeLineJoin = Miter,
                    strokeLineMiter = 4.0f,
                    pathFillType = NonZero
                ) {
                    moveTo(6.96f, 0.25f)
                    curveTo(7.29f, 0.25f, 7.62f, 0.25f, 7.95f, 0.25f)
                    curveTo(8.86f, 0.25f, 9.76f, 0.25f, 10.67f, 0.26f)
                    curveTo(11.64f, 0.26f, 12.62f, 0.26f, 13.59f, 0.26f)
                    curveTo(15.29f, 0.26f, 16.98f, 0.26f, 18.67f, 0.26f)
                    curveTo(21.11f, 0.27f, 23.56f, 0.27f, 26.0f, 0.27f)
                    curveTo(29.97f, 0.27f, 33.94f, 0.28f, 37.9f, 0.28f)
                    curveTo(41.76f, 0.29f, 45.61f, 0.29f, 49.46f, 0.3f)
                    curveTo(49.7f, 0.3f, 49.94f, 0.3f, 50.18f, 0.3f)
                    curveTo(51.37f, 0.3f, 52.56f, 0.3f, 53.76f, 0.3f)
                    curveTo(63.64f, 0.31f, 73.53f, 0.32f, 83.41f, 0.33f)
                    curveTo(83.41f, 0.49f, 83.41f, 0.64f, 83.41f, 0.8f)
                    curveTo(83.43f, 4.59f, 83.44f, 8.38f, 83.46f, 12.17f)
                    curveTo(83.47f, 14.0f, 83.48f, 15.84f, 83.49f, 17.67f)
                    curveTo(83.49f, 19.27f, 83.5f, 20.87f, 83.51f, 22.47f)
                    curveTo(83.52f, 23.31f, 83.52f, 24.16f, 83.52f, 25.0f)
                    curveTo(83.52f, 25.8f, 83.53f, 26.6f, 83.53f, 27.4f)
                    curveTo(83.54f, 27.82f, 83.54f, 28.25f, 83.54f, 28.68f)
                    curveTo(83.56f, 31.26f, 83.87f, 33.94f, 85.09f, 36.24f)
                    curveTo(85.17f, 36.39f, 85.25f, 36.54f, 85.33f, 36.69f)
                    curveTo(86.69f, 39.2f, 88.83f, 40.82f, 91.56f, 41.63f)
                    curveTo(92.92f, 41.96f, 94.23f, 42.01f, 95.63f, 42.03f)
                    curveTo(95.87f, 42.03f, 96.11f, 42.04f, 96.35f, 42.04f)
                    curveTo(102.98f, 42.12f, 109.61f, 42.2f, 116.25f, 42.28f)
                    curveTo(117.35f, 42.29f, 118.46f, 42.3f, 119.56f, 42.32f)
                    curveTo(121.71f, 42.34f, 123.85f, 42.37f, 126.0f, 42.39f)
                    curveTo(126.01f, 53.63f, 126.03f, 64.87f, 126.04f, 76.11f)
                    curveTo(126.04f, 76.36f, 126.04f, 76.36f, 126.04f, 76.61f)
                    curveTo(126.04f, 80.23f, 126.04f, 83.84f, 126.04f, 87.46f)
                    curveTo(126.05f, 89.23f, 126.05f, 91.0f, 126.05f, 92.78f)
                    curveTo(126.05f, 92.95f, 126.05f, 93.13f, 126.05f, 93.31f)
                    curveTo(126.05f, 99.03f, 126.06f, 104.75f, 126.07f, 110.47f)
                    curveTo(126.08f, 116.34f, 126.08f, 122.22f, 126.08f, 128.09f)
                    curveTo(126.08f, 128.92f, 126.08f, 129.75f, 126.08f, 130.58f)
                    curveTo(126.08f, 130.83f, 126.08f, 130.83f, 126.08f, 131.08f)
                    curveTo(126.08f, 133.71f, 126.09f, 136.34f, 126.09f, 138.97f)
                    curveTo(126.1f, 141.61f, 126.1f, 144.26f, 126.1f, 146.9f)
                    curveTo(126.1f, 148.33f, 126.1f, 149.76f, 126.11f, 151.2f)
                    curveTo(126.11f, 152.51f, 126.11f, 153.82f, 126.11f, 155.13f)
                    curveTo(126.11f, 155.6f, 126.11f, 156.08f, 126.11f, 156.55f)
                    curveTo(126.11f, 157.2f, 126.11f, 157.84f, 126.11f, 158.49f)
                    curveTo(126.11f, 158.76f, 126.11f, 158.76f, 126.12f, 159.05f)
                    curveTo(126.1f, 160.54f, 125.66f, 161.68f, 124.7f, 162.83f)
                    curveTo(124.56f, 162.95f, 124.42f, 163.07f, 124.28f, 163.19f)
                    curveTo(124.14f, 163.32f, 124.0f, 163.44f, 123.86f, 163.57f)
                    curveTo(122.58f, 164.38f, 121.24f, 164.4f, 119.79f, 164.39f)
                    curveTo(119.59f, 164.39f, 119.39f, 164.39f, 119.18f, 164.39f)
                    curveTo(118.51f, 164.39f, 117.84f, 164.39f, 117.17f, 164.39f)
                    curveTo(116.68f, 164.39f, 116.2f, 164.39f, 115.71f, 164.39f)
                    curveTo(114.38f, 164.4f, 113.04f, 164.4f, 111.71f, 164.4f)
                    curveTo(110.42f, 164.39f, 109.13f, 164.4f, 107.84f, 164.4f)
                    curveTo(104.87f, 164.4f, 101.91f, 164.4f, 98.94f, 164.4f)
                    curveTo(96.65f, 164.4f, 94.36f, 164.4f, 92.07f, 164.4f)
                    curveTo(91.74f, 164.4f, 91.41f, 164.4f, 91.08f, 164.4f)
                    curveTo(90.59f, 164.4f, 90.09f, 164.4f, 89.6f, 164.4f)
                    curveTo(84.99f, 164.4f, 80.38f, 164.4f, 75.77f, 164.4f)
                    curveTo(75.51f, 164.4f, 75.51f, 164.4f, 75.24f, 164.4f)
                    curveTo(74.36f, 164.4f, 73.48f, 164.4f, 72.6f, 164.4f)
                    curveTo(72.42f, 164.4f, 72.25f, 164.4f, 72.07f, 164.4f)
                    curveTo(71.71f, 164.4f, 71.36f, 164.4f, 71.01f, 164.4f)
                    curveTo(65.5f, 164.4f, 60.0f, 164.4f, 54.49f, 164.41f)
                    curveTo(48.31f, 164.41f, 42.12f, 164.41f, 35.94f, 164.41f)
                    curveTo(35.28f, 164.41f, 34.62f, 164.41f, 33.96f, 164.41f)
                    curveTo(33.8f, 164.41f, 33.64f, 164.41f, 33.47f, 164.41f)
                    curveTo(31.02f, 164.41f, 28.57f, 164.41f, 26.11f, 164.42f)
                    curveTo(23.33f, 164.42f, 20.54f, 164.42f, 17.75f, 164.42f)
                    curveTo(16.33f, 164.42f, 14.91f, 164.42f, 13.48f, 164.42f)
                    curveTo(12.18f, 164.42f, 10.88f, 164.42f, 9.58f, 164.42f)
                    curveTo(9.11f, 164.42f, 8.63f, 164.42f, 8.16f, 164.42f)
                    curveTo(7.52f, 164.42f, 6.88f, 164.42f, 6.24f, 164.42f)
                    curveTo(6.06f, 164.42f, 5.88f, 164.42f, 5.69f, 164.42f)
                    curveTo(4.1f, 164.41f, 2.7f, 164.03f, 1.48f, 162.94f)
                    curveTo(1.27f, 162.76f, 1.27f, 162.76f, 1.05f, 162.58f)
                    curveTo(-0.13f, 161.15f, -0.09f, 159.69f, -0.08f, 157.92f)
                    curveTo(-0.09f, 157.52f, -0.09f, 157.52f, -0.09f, 157.11f)
                    curveTo(-0.09f, 156.37f, -0.09f, 155.64f, -0.09f, 154.9f)
                    curveTo(-0.08f, 154.1f, -0.09f, 153.29f, -0.09f, 152.49f)
                    curveTo(-0.09f, 151.08f, -0.09f, 149.68f, -0.09f, 148.27f)
                    curveTo(-0.09f, 146.18f, -0.09f, 144.08f, -0.09f, 141.99f)
                    curveTo(-0.1f, 138.25f, -0.1f, 134.51f, -0.1f, 130.78f)
                    curveTo(-0.1f, 127.51f, -0.1f, 124.24f, -0.1f, 120.97f)
                    curveTo(-0.1f, 120.53f, -0.1f, 120.09f, -0.1f, 119.65f)
                    curveTo(-0.1f, 118.99f, -0.1f, 118.33f, -0.1f, 117.67f)
                    curveTo(-0.1f, 111.53f, -0.1f, 105.38f, -0.1f, 99.24f)
                    curveTo(-0.1f, 99.01f, -0.1f, 98.77f, -0.1f, 98.54f)
                    curveTo(-0.1f, 96.65f, -0.1f, 94.77f, -0.1f, 92.89f)
                    curveTo(-0.1f, 85.55f, -0.1f, 78.21f, -0.1f, 70.87f)
                    curveTo(-0.11f, 62.63f, -0.11f, 54.39f, -0.11f, 46.14f)
                    curveTo(-0.11f, 45.27f, -0.11f, 44.39f, -0.11f, 43.51f)
                    curveTo(-0.11f, 43.29f, -0.11f, 43.08f, -0.11f, 42.85f)
                    curveTo(-0.11f, 39.58f, -0.11f, 36.32f, -0.11f, 33.05f)
                    curveTo(-0.12f, 29.33f, -0.12f, 25.61f, -0.11f, 21.9f)
                    curveTo(-0.11f, 19.82f, -0.11f, 17.74f, -0.12f, 15.66f)
                    curveTo(-0.12f, 14.27f, -0.12f, 12.88f, -0.11f, 11.49f)
                    curveTo(-0.11f, 10.7f, -0.11f, 9.9f, -0.11f, 9.11f)
                    curveTo(-0.12f, 8.26f, -0.11f, 7.41f, -0.11f, 6.55f)
                    curveTo(-0.11f, 6.31f, -0.11f, 6.06f, -0.12f, 5.81f)
                    curveTo(-0.1f, 4.0f, 0.22f, 2.78f, 1.5f, 1.49f)
                    curveTo(3.31f, 0.24f, 4.81f, 0.24f, 6.96f, 0.25f)
                    close()
                }
                path(
                    fill = SolidColor(Color(0xFFD4D5D8)),
                    stroke = null,
                    strokeLineWidth = 0.0f,
                    strokeLineCap = Butt,
                    strokeLineJoin = Miter,
                    strokeLineMiter = 4.0f,
                    pathFillType = NonZero
                ) {
                    moveTo(86.72f, 4.12f)
                    curveTo(87.35f, 4.42f, 87.81f, 4.71f, 88.3f, 5.22f)
                    curveTo(88.44f, 5.35f, 88.57f, 5.49f, 88.7f, 5.62f)
                    curveTo(88.84f, 5.77f, 88.98f, 5.91f, 89.12f, 6.06f)
                    curveTo(89.27f, 6.21f, 89.42f, 6.36f, 89.57f, 6.51f)
                    curveTo(90.04f, 6.99f, 90.5f, 7.46f, 90.97f, 7.94f)
                    curveTo(91.43f, 8.41f, 91.9f, 8.89f, 92.36f, 9.36f)
                    curveTo(92.65f, 9.65f, 92.94f, 9.95f, 93.22f, 10.24f)
                    curveTo(93.9f, 10.93f, 94.59f, 11.59f, 95.32f, 12.23f)
                    curveTo(96.26f, 13.05f, 97.14f, 13.93f, 98.01f, 14.82f)
                    curveTo(98.17f, 14.99f, 98.34f, 15.15f, 98.51f, 15.32f)
                    curveTo(99.21f, 16.03f, 99.9f, 16.74f, 100.6f, 17.45f)
                    curveTo(101.11f, 17.97f, 101.62f, 18.49f, 102.13f, 19.0f)
                    curveTo(102.29f, 19.16f, 102.44f, 19.32f, 102.6f, 19.48f)
                    curveTo(103.32f, 20.21f, 104.04f, 20.91f, 104.81f, 21.58f)
                    curveTo(105.77f, 22.41f, 106.66f, 23.31f, 107.55f, 24.21f)
                    curveTo(107.8f, 24.47f, 107.8f, 24.47f, 108.06f, 24.73f)
                    curveTo(108.78f, 25.46f, 109.5f, 26.19f, 110.21f, 26.92f)
                    curveTo(110.74f, 27.45f, 111.27f, 27.99f, 111.79f, 28.52f)
                    curveTo(111.95f, 28.69f, 112.11f, 28.85f, 112.28f, 29.02f)
                    curveTo(113.15f, 29.9f, 114.04f, 30.73f, 114.98f, 31.55f)
                    curveTo(115.71f, 32.2f, 116.39f, 32.89f, 117.08f, 33.58f)
                    curveTo(117.23f, 33.74f, 117.38f, 33.89f, 117.53f, 34.04f)
                    curveTo(117.84f, 34.35f, 118.15f, 34.67f, 118.46f, 34.98f)
                    curveTo(118.94f, 35.47f, 119.42f, 35.95f, 119.9f, 36.43f)
                    curveTo(120.21f, 36.74f, 120.51f, 37.05f, 120.81f, 37.35f)
                    curveTo(120.96f, 37.5f, 121.1f, 37.64f, 121.25f, 37.79f)
                    curveTo(122.25f, 38.8f, 122.25f, 38.8f, 122.25f, 39.05f)
                    curveTo(118.63f, 39.07f, 115.02f, 39.08f, 111.4f, 39.09f)
                    curveTo(109.72f, 39.09f, 108.04f, 39.1f, 106.36f, 39.1f)
                    curveTo(104.9f, 39.11f, 103.44f, 39.11f, 101.97f, 39.12f)
                    curveTo(101.2f, 39.12f, 100.42f, 39.12f, 99.65f, 39.12f)
                    curveTo(98.78f, 39.13f, 97.91f, 39.13f, 97.05f, 39.13f)
                    curveTo(96.79f, 39.13f, 96.54f, 39.13f, 96.28f, 39.14f)
                    curveTo(93.61f, 39.13f, 91.33f, 38.42f, 89.32f, 36.6f)
                    curveTo(87.75f, 34.94f, 86.69f, 32.87f, 86.69f, 30.55f)
                    curveTo(86.69f, 30.32f, 86.69f, 30.09f, 86.69f, 29.85f)
                    curveTo(86.69f, 29.59f, 86.7f, 29.34f, 86.7f, 29.08f)
                    curveTo(86.7f, 28.81f, 86.7f, 28.54f, 86.7f, 28.26f)
                    curveTo(86.7f, 27.52f, 86.7f, 26.78f, 86.7f, 26.05f)
                    curveTo(86.7f, 25.28f, 86.7f, 24.51f, 86.7f, 23.73f)
                    curveTo(86.7f, 22.27f, 86.7f, 20.82f, 86.71f, 19.36f)
                    curveTo(86.71f, 17.69f, 86.71f, 16.03f, 86.71f, 14.37f)
                    curveTo(86.71f, 10.95f, 86.72f, 7.53f, 86.72f, 4.12f)
                    close()
                }
            }
        }.build()
        return _documentLarge!!
    }

private var _documentLarge: ImageVector? = null

@Preview
@Composable
private fun PreviewDocumentLarge() {
    Box(modifier = Modifier.padding(12.dp)) {
        Image(imageVector = ChatIcons.DocumentLarge, contentDescription = "")
    }
}
