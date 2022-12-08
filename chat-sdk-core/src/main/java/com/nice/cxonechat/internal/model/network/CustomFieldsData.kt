package com.nice.cxonechat.internal.model.network

import com.google.gson.annotations.SerializedName
import com.nice.cxonechat.internal.model.CustomFieldModel

internal data class CustomFieldsData(
    @SerializedName("customFields")
    val customFields: List<CustomFieldModel>,
)
