/*
 * Copyright (c) 2021-2026. NICE Ltd. All rights reserved.
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

package com.nice.cxonechat.ui.composable.generic

import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import com.nice.cxonechat.ui.composable.theme.ChatTheme.colorScheme
import com.nice.cxonechat.ui.composable.theme.LocalChatTypography
import com.nice.cxonechat.ui.domain.model.TreeFieldItem

/** Semantics key used in tests to record the ARGB color of the label. Tests can read this
 *  ARGB integer value from the semantics tree to assert that the label is tinted with the
 *  theme error color when validation fails.
 */
@VisibleForTesting
internal val TreeFieldLabelColorKey = SemanticsPropertyKey<Int>("TreeFieldLabel")

/**
 * Container for localized accessibility text used in tree field descriptions.
 *
 * @param selected Localized text for "selected" state
 * @param expanded Localized text for "expanded" state
 * @param collapsed Localized text for "collapsed" state
 */
internal data class AccessibilityText(
    val selected: String,
    val expanded: String,
    val collapsed: String,
)

/**
 * Builds the accessibility description for a tree node based on its state.
 *
 * @param node The tree node to build the description for
 * @param isLeaf Whether the node is a leaf node
 * @param expanded Whether the node is expanded
 * @param isNodeSelected Whether the node is selected
 * @param text Localized accessibility text for different states
 */
internal fun buildAccessibilityDescription(
    node: TreeFieldItem<*>,
    isLeaf: Boolean,
    expanded: Boolean,
    isNodeSelected: Boolean,
    text: AccessibilityText,
): String = buildString {
    append(node.label)
    when {
        isLeaf ->
            if (isNodeSelected) {
                append(", ")
                append(text.selected)
            }

        else -> {
            append(", ")
            append(if (expanded) text.expanded else text.collapsed)
        }
    }
}

/**
 * Handles the click event for a tree node, determining whether to select, expand, or collapse it.
 */
internal fun <ValueType> handleNodeClick(
    node: TreeFieldItem<ValueType>,
    isLeaf: Boolean,
    expanded: Boolean,
    onNodeClicked: (TreeFieldItem<ValueType>) -> Unit,
    onExpandClicked: (TreeFieldItem<ValueType>) -> Unit,
) {
    when {
        isLeaf -> onNodeClicked(node)
        expanded -> onNodeClicked(node)
        else -> onExpandClicked(node)
    }
}

/**
 * Renders the trailing icon for a tree node.
 */
@Composable
internal fun TreeNodeTrailingIcon(isLeaf: Boolean, isNodeSelected: Boolean, expanded: Boolean, nodeLabel: String) {
    when {
        isLeaf -> Box(contentAlignment = Alignment.Center) {
            if (isNodeSelected) {
                SelectedIcon(nodeLabel)
            }
        }

        else -> TrailingIcon(expanded = expanded)
    }
}

@Composable
internal fun TreeFieldLabel(label: String, error: String?) {
    val argb = colorScheme.error.toArgb()
    val labelModifier = Modifier
        .semantics {
            testTag = "tree_field_label"
            error?.let {
                this[TreeFieldLabelColorKey] = argb
            }
        }

    Text(
        text = label,
        style = LocalChatTypography.current.surveyLabel,
        color = if (error != null) colorScheme.error else Color.Unspecified,
        modifier = labelModifier,
    )
}
