package com.nice.cxonechat.model

import com.nice.cxonechat.internal.model.NodeModel
import com.nice.cxonechat.tool.nextString
import java.util.UUID

internal fun nextNode(rootId: String? = null): NodeModel {
    return NodeModel(UUID.randomUUID().toString(), nextString(), rootId)
}
