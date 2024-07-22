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

package com.nice.cxonechat.ui.composable.generic

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import com.nice.cxonechat.ui.util.findActivity

/**
 * Set temporary title to parent [Activity] via [DisposableEffect].
 * When the [Composable] is disposed, the original title will be set back.
 *
 * Avoid using multiple parallel [TemporaryActivityTitle] since,
 * there is no guarantee that disposing will happen in reverse order
 * as the original application of effects.
 *
 * @param title A temporary title which will be set.
 */
@Composable
internal fun TemporaryActivityTitle(title: String) {
    val context = LocalContext.current
    DisposableEffect(key1 = title) {
        val activity: Activity = context.findActivity() ?: return@DisposableEffect onDispose {}
        val originalTitle = activity.title
        activity.title = title
        onDispose { activity.title = originalTitle }
    }
}
