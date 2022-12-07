package com.nice.cxonechat.internal.model.network

import com.google.gson.annotations.SerializedName
import com.nice.cxonechat.analytics.ActionMetadata
import com.nice.cxonechat.analytics.ActionMetadataInternal
import java.util.UUID

internal data class ProactiveActionInfo constructor(
    @SerializedName("actionId")
    val actionId: UUID,
    @SerializedName("actionName")
    val actionName: String,
    @SerializedName("actionType")
    val actionType: String,
) {

    constructor(
        metadata: ActionMetadataInternal,
    ) : this(
        metadata.id,
        metadata.name,
        metadata.type.value
    )

    companion object {

        operator fun invoke(metadata: ActionMetadata) = when (metadata) {
            is ActionMetadataInternal -> ProactiveActionInfo(metadata)
        }

    }

}
