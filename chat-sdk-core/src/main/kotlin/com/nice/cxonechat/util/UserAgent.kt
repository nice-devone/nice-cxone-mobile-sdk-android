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

package com.nice.cxonechat.util

import androidx.annotation.VisibleForTesting
import com.nice.cxonechat.core.BuildConfig
import com.nice.cxonechat.util.UserAgent.NetworkUserAgent
import okhttp3.OkHttp
import okhttp3.OkHttpClient
import io.github.ackeecz.useragent.UserAgent as AckeeUserAgent

internal object UserAgent {

    /** HTTP header name for the User-Agent string. */
    @VisibleForTesting
    internal const val USER_AGENT_HEADER_NAME = "User-Agent"

    /**
     * SDK/OkHttp suffix appended to the app/device user agent string.
     * Used as the trailing part of the full User-Agent header built by
     * [AckeeUserAgent.getUserAgentString].
     */
    @VisibleForTesting
    internal val NetworkUserAgent =
        "okhttp/${OkHttp.VERSION} ${BuildConfig.IDENTIFIER}/${BuildConfig.VERSION_NAME}"

    /**
     * Adds an interceptor that sets the `User-Agent` header on every request
     * with app name, version, device info, and SDK version.
     *
     * @param userAgent the [AckeeUserAgent] instance used to build the full
     * User-Agent string from app/device metadata and [NetworkUserAgent] suffix.
     */
    internal fun OkHttpClient.Builder.addUserAgentInterceptor(
        userAgent: AckeeUserAgent,
    ): OkHttpClient.Builder = addInterceptor { chain ->
        chain.proceed(
            chain.request()
                .newBuilder()
                .header(USER_AGENT_HEADER_NAME, userAgent.getUserAgentString(NetworkUserAgent))
                .build()
        )
    }
}
