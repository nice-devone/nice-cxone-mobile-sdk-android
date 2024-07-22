/*
 * Copyright (c) 2021-2024. NICE Ltd. All rights reserved.
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

package com.nice.cxonechat.ui.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.util.TypedValue

/**
 * Convert DP to Pixels in the receiving context.
 *
 * @receiver Context for display metrics
 * @param dp DP to convert to pixels
 * @return converted pixels
 */
internal fun Context.dpToPixels(dp: Float) =
    TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, resources.displayMetrics)

/**
 * Ask android to open a URL if possible.
 *
 * @param url URL to open
 * @param mimeType mime type of [url] if known
 * @return true iff android could open the URL
 */
internal fun Context.openWithAndroid(url: String, mimeType: String?): Boolean {
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(Uri.parse(url), mimeType)
    }

    return if (intent.resolveActivity(packageManager) == null) {
        false
    } else {
        startActivity(intent)
        true
    }
}

/**
 * Look up parent activity recursively.
 *
 * @return Parent [Activity] or `null`.
 */
internal tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    !is ContextWrapper -> null
    else -> baseContext.findActivity()
}
