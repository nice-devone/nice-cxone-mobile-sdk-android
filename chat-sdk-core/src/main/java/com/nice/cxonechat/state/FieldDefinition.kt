package com.nice.cxonechat.state

import com.nice.cxonechat.Public
import com.nice.cxonechat.exceptions.InvalidCustomFieldValue
import kotlin.jvm.Throws

/** Definition of a custom Field that is stored on the server. */
@Public
interface FieldDefinition {
    /** Identifier for the field. */
    val fieldId: String

    /** Human-readable label for the field. */
    val label: String

    /** True iff the value is required.  This is only valid for fields within a survey */
    val isRequired: Boolean

    /**
     * Check if [value] is valid for this field definition.
     *
     * Exact semantics depend on the field type:
     * * [Text] fields will be checked for a proper email format if non-blank and isEMail is set
     * * [Selector] and [Hierarchy] fields will be checked to verify that the value matches an
     * included node [nodeId].
     * * [Hierarchy] fields will verify the selected node is a leaf node.
     *
     * @throws InvalidCustomFieldValue if the field fails validation.
     */
    @Throws(
        InvalidCustomFieldValue::class
    )
    fun validate(value: String)

    /** Details of a text (or email) field. */
    @Public
    interface Text: FieldDefinition {
        /**
         * Is the field intended to be an email address?
         *
         * EMail fields should be displayed with an EMail keyboard if available and
         * will be validated as having an appropriate form for an address.
         */
        val isEMail: Boolean
    }

    /**
     * Details of a list field.
     *
     * Typically, a list field will be displayed as a dropdown or popup menu.
     * Each item in the menu will take its label from the [label] property of the corresponding
     * [SelectorNode] in values.  The value of the field can only be set to one of
     * the [SelectorNode.nodeId] values from the list of [SelectorNode].
     */
    @Public
    interface Selector: FieldDefinition {
        /**
         * The list of possible values which the chosen [Selector] may hold.
         *
         * The field value *must* be the [SelectorNode.nodeId] of one of these nodes.
         */
        val values: Sequence<SelectorNode>
    }

    /**
     * Details of a hierarchical or tree field.
     *
     * Usually displayed as a collapsing tree, with each node displaying text corresponding
     * to the [label] field of the corresponding [HierarchyNode].  The value of the field
     * can only be set to one of the [HierarchyNode.nodeId] values from the hierarchy of nodes, where the
     * corresponding [HierarchyNode.isLeaf] property is `true`.
     */
    @Public
    interface Hierarchy: FieldDefinition {
        /** Top level options. */
        val values: Sequence<HierarchyNode<String>>
    }
}
