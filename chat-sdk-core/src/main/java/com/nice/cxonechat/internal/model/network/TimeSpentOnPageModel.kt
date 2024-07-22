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

package com.nice.cxonechat.internal.model.network

import com.google.gson.annotations.SerializedName

/**
 * Data to be sent on a page view visitor event.
 */
internal data class TimeSpentOnPageModel(
    /** A title for the page that was viewed. */
    @SerializedName("title")
    val title: String,

    /** The unique URL or URI for the page that was viewed. Doesn't need to be a valid URL. */
    @SerializedName("url")
    val url: String, // This can be any identifier for the page; doesn't need to be URL

    /** Time spent on the page. */
    @SerializedName("timeSpentOnPage")
    val timeSpentOnPage: Long,
)
