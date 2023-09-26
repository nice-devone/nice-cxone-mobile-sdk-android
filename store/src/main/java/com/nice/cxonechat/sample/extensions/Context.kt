/*
 * Copyright (c) 2021-2023. NICE Ltd. All rights reserved.
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

package com.nice.cxonechat.sample.extensions

import android.content.Context
import android.content.pm.PackageManager.NameNotFoundException
import android.content.pm.PackageManager.PackageInfoFlags
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import androidx.annotation.DrawableRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.core.graphics.drawable.toBitmap

/**
 * Create a Compose [Painter] from a drawable resource.
 *
 * Note: Intended for usage *only* with mipmap resources (like launcher icons).
 * For other resource types use [painterResource](androidx.compose.ui.res.painterResource).
 *
 * @param resId drawable resource id
 * @return a [Painter] resource suitable for usage with [Icon](androidx.compose.material.Icon)
 * and [Image](androidx.compose.foundation.Image).
 */
fun Context.mipmapPainter(@DrawableRes resId: Int): Painter? = AppCompatResources
    .getDrawable(this, resId)
    ?.toBitmap()
    ?.asImageBitmap()
    ?.let {
        return BitmapPainter(it)
    }

/**
 * Fetch the `versionName` value from the manifest file.
 */
val Context.manifestVersionName: String?
    get() {
        return try {
            if (VERSION.SDK_INT >= VERSION_CODES.TIRAMISU) {
                packageManager
                    ?.getPackageInfo(packageName, PackageInfoFlags.of(0))
                    ?.versionName
            } else {
                @Suppress("Deprecation")
                packageManager?.getPackageInfo(packageName, 0)?.versionName
            }
        } catch (_: NameNotFoundException) {
            null
        } catch (_: UnsupportedOperationException) {
            // This is necessary to support usage in preview windows where the
            // context is the Activity, not a real context.
            null
        }
    }
