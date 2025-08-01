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

package com.nice.cxonechat.ui.util

import android.net.Uri
import java.util.UUID

internal fun Uri.parseThreadDeeplink(): Result<UUID> = runCatching {
    val threadIdString = getQueryParameter(PARAM_DEEPLINK)
    require(!threadIdString.isNullOrEmpty()) { "Invalid threadId in $this" }
    UUID.fromString(threadIdString)
}

internal fun Uri.addThreadDeeplink(uid: UUID): Uri = buildUpon()
    .appendQueryParameter(PARAM_DEEPLINK, uid.toString())
    .build()

private const val PARAM_DEEPLINK = "idOnExternalPlatform"
