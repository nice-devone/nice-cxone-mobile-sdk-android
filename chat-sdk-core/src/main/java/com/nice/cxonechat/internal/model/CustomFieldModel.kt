package com.nice.cxonechat.internal.model

import com.google.gson.annotations.SerializedName
import com.nice.cxonechat.thread.CustomField
import java.util.Date

internal data class CustomFieldModel(
    @SerializedName("ident")
    val id: String,
    @SerializedName("value")
    val value: String,
    @SerializedName("updatedAt")
    val updatedAt: Date,
) {

    constructor(
        entry: Map.Entry<String, String>,
    ) : this(
        id = entry.key,
        value = entry.value,
        updatedAt = Date(),
    )

    constructor(
        field: CustomField,
    ) : this(
        id = field.id,
        value = field.value,
        updatedAt = field.updatedAt
    )

    fun toCustomField() = CustomFieldInternal(
        id = id,
        value = value,
        updatedAt = updatedAt
    )
}
