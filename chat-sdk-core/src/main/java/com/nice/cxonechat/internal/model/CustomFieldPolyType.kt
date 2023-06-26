package com.nice.cxonechat.internal.model

import com.google.gson.annotations.SerializedName

internal sealed interface CustomFieldPolyType {

    data class Text(
        @SerializedName("ident")
        val fieldId: String,
        @SerializedName("label")
        val label: String,
    ) : CustomFieldPolyType

    data class Email(
        @SerializedName("ident")
        val fieldId: String,
        @SerializedName("label")
        val label: String,
    ) : CustomFieldPolyType

    data class Selector(
        @SerializedName("ident")
        val fieldId: String,
        @SerializedName("label")
        val label: String,
        @SerializedName("values")
        val values: List<SelectorModel>,
    ) : CustomFieldPolyType

    data class Hierarchy(
        @SerializedName("ident")
        val fieldId: String,
        @SerializedName("label")
        val label: String,
        @SerializedName("values")
        val values: List<NodeModel>,
    ) : CustomFieldPolyType

    object Noop : CustomFieldPolyType
}
