package com.nice.cxonechat.internal.model

import com.nice.cxonechat.thread.CustomField

internal data class CustomFieldInternal(
    override val id: String,
    override val value: String,
) : CustomField() {

    constructor(
        entry: Map.Entry<String, String>
    ) : this(
        id = entry.key,
        value = entry.value
    )

    override fun toString() = buildString {
        append("CustomField(id='")
        append(id)
        append("', value='")
        append(value)
        append("')")
    }

}
