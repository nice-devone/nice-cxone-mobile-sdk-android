package com.nice.cxonechat.analytics

import com.nice.cxonechat.enums.ActionType
import java.util.UUID

internal data class ActionMetadataInternal(
    val id: UUID,
    val name: String,
    val type: ActionType,
) : ActionMetadata
