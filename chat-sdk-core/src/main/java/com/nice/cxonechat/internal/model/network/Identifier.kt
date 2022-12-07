package com.nice.cxonechat.internal.model.network

import com.google.gson.annotations.SerializedName
import java.util.UUID

internal data class Identifier(
    @SerializedName("id")
    val id: String,
) {

    constructor(id: UUID) : this(id.toString())

}
