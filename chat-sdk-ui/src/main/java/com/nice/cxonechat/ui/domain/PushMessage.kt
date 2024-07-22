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

package com.nice.cxonechat.ui.domain

import androidx.annotation.DrawableRes

internal data class PushMessage(
    val imageUrl: String?,
    val imageIconUrl: String?,
    val imageSmallIconUrl: String?,
    val isSilent: Boolean,
    @DrawableRes val iconResId: Int,
    val title: String?,
    val message: String?,
    val colorString: String?,
    val url: String?,
    val deepLink: String?,
    val openApp: String?,
)
