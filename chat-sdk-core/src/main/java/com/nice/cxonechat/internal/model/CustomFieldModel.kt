package com.nice.cxonechat.internal.model

import com.google.gson.annotations.SerializedName
import com.nice.cxonechat.thread.CustomField

internal data class CustomFieldModel(
    @SerializedName("ident")
    val id: String,
    @SerializedName("value")
    val value: String,
) {

    constructor(
        entry: Map.Entry<String, String>,
    ) : this(
        entry.key,
        entry.value
    )

    constructor(
        field: CustomField,
    ) : this(
        field.id,
        field.value
    )

    fun toCustomField() = CustomFieldInternal(
        id = id,
        value = value
    )

}
