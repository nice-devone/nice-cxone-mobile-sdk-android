package com.nice.cxonechat.internal.model

import com.google.gson.annotations.SerializedName

internal data class PreContactCustomFieldDefinitionModel(
    @SerializedName("isRequired")
    val isRequired: Boolean,
    @SerializedName("definition")
    val definition: CustomFieldPolyType,
)
