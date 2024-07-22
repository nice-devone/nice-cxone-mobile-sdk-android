/*
 * Copyright (c) 2021-2024. NICE Ltd. All rights reserved.
 *
 * Licensed under the NICE License;
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    https://github.com/nice-devone/nice-cxone-mobile-sdk-android/blob/main/LICENSE
 *
 * TO THE EXTENT PERMITTED BY APPLICABLE LAW, THE CXONE MOBILE SDK IS PROVIDED ON
 * AN “AS IS” BASIS. NICE HEREBY DISCLAIMS ALL WARRANTIES AND CONDITIONS, EXPRESS
 * OR IMPLIED, INCLUDING (WITHOUT LIMITATION) WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE, NON-INFRINGEMENT, AND TITLE.
 */

package com.nice.cxonechat.state

import com.nice.cxonechat.internal.model.NodeModel

/**
 * Internal representation of [HierarchyNode] for N-Ary tree reconstruction from a list of [NodeModel]s.
 */
internal class HierarchyNodeInternal<T>(
    override val nodeId: T,
    private var originalLabel: String?,
    val nodes: MutableList<HierarchyNodeInternal<T>> = mutableListOf(),

    /**
     * Reference to parent node.
     * This field is variable since we are fetching/creating an instance of the parent after this node is created & inserted.
     */
    var parent: HierarchyNodeInternal<T>? = null,
) : HierarchyNode<T> {

    override val label: String
        get() = checkNotNull(originalLabel) { "label only valid after full construction" }

    override val isLeaf: Boolean
        get() = nodes.isEmpty()

    override val children: Sequence<HierarchyNode<T>>
        get() = nodes.asSequence()

    override fun toString(): String =
        "HierarchyNode(nodeId=$nodeId, label=$label, children=${nodes.joinToString(limit = 100)})"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as HierarchyNodeInternal<*>

        if (nodeId != other.nodeId) return false
        if (originalLabel != other.originalLabel) return false
        if (nodes != other.nodes) return false

        return true
    }

    override fun hashCode(): Int {
        var result = nodeId.hashCode()
        result = 31 * result + originalLabel.hashCode()
        result = 31 * result + nodes.hashCode()
        return result
    }

    companion object {
        /**
         * Reconstructs a node hierarchy from a flat list of [NodeModel]s to tree structure of [HierarchyNode]s.
         * The resulting [Iterable] can contain multiple root [HierarchyNode]s.
         *
         * [HierarchyNodeInternal] is used for reconstruction of the hierarchy since it allows reassignment
         * of the properties as they will be resolved during multiple iterations.
         *
         * Node reconstruction doesn't assume any order of the node models in the receiver sequence, because
         * of that reconstruction first checks if the node instance already exists and updates it, if it is needed.
         *
         * After that method checks if the node has a parent and if that node already exists and creates it if
         * it is needed (without value).
         * If the node model doesn't have a parent, then the node instance is a root node for the final set of nodes
         * which will be returned.
         *
         * As final check, the conversion discards any root nodes which were created because they were referenced by
         * child nodes, but the node model with matching id was not present in the receiver sequence (thus also
         * discarding child nodes with invalid parent reference).
         *
         * @receiver Iterable sequence of [NodeModel] which should be converted to an iterable sequence of [HierarchyNode].
         * @return Iterable sequence of [HierarchyNode] which consists of root nodes from the original iterable.
         */
        internal fun Iterable<NodeModel>.toNodeIterable(): Sequence<HierarchyNode<String>> {
            val nodes = mutableMapOf<String, HierarchyNodeInternal<String>>()
            val nodesWithoutParent = mutableSetOf<HierarchyNodeInternal<String>>()
            for ((name, label, parentId) in this) {
                /*
                 * Create node instance if it doesn't yet exist.
                 * Instance will already exist in case of parent node which was created, by its children which were processed earlier.
                 */
                val existingNode = nodes.getOrPut(name) { HierarchyNodeInternal(name, label) }
                if (existingNode.originalLabel == null) {
                    // Update the node if it was just a referential parent node without value.
                    existingNode.originalLabel = label
                }
                if (parentId != null) {
                    // Get or create parent node. Created parent node should be updated later with internal value.
                    val parent = nodes.getOrPut(parentId) { HierarchyNodeInternal(parentId, originalLabel = null) }
                    existingNode.parent = parent
                    // Add self-reference to parent in order to restore hierarchy.
                    parent.nodes.add(existingNode)
                } else {
                    // This node doesn't have a parent, so it is a root node which needs to be in the final result set.
                    nodesWithoutParent.add(existingNode)
                }
            }
            // Remove theoretically possible invalid root nodes, which were created by children but never updated, from the returned set.
            return nodesWithoutParent.filterNot { it.originalLabel == null }.asSequence()
        }
    }
}
