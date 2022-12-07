package com.nice.cxonechat.internal.model

import com.google.gson.annotations.SerializedName

// BrandView

/**
 * Represents all info about the brand.
 */
internal data class Brand constructor(
    /** The id of the brand. */
    @SerializedName("id")
    val id: Int,
)
