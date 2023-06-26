package com.nice.cxonechat.state

internal data class SelectorNodeImpl(
    override val nodeId: String,
    override val label: String,
) : SelectorNode {
    override fun toString() = "SelectorNode(nodeId='$nodeId', label='$label')"
}
