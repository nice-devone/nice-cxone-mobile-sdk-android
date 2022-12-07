package com.nice.cxonechat.internal.model.network

import com.google.gson.annotations.SerializedName

/**
 * Data to be sent on a page view visitor event.
 */
internal data class PageViewData constructor(
    /** A title for the page that was viewed. */
    @SerializedName("title")
    val title: String,

    /** The unique URL or URI for the page that was viewed. Doesn't need to be a valid URL. */
    @SerializedName("url")
    val url: String, // This can be any identifier for the page; doesn't need to be URL
)
