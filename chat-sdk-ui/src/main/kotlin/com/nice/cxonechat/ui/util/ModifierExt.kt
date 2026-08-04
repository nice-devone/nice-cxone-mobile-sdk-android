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

package com.nice.cxonechat.ui.util

import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.hideFromAccessibility

/**
 * Hides the composable and all its descendants from accessibility services
 * by clearing any existing semantics and preventing children from contributing
 * their own semantics nodes.
 *
 * This is useful for decorative elements that do not convey meaningful information
 * to users of assistive technologies. Do not apply to composables with interactive
 * children, as their accessibility nodes will also be removed.
 *
 * @param semantics optional lambda to set additional semantics properties (e.g., [testTag][androidx.compose.ui.semantics.testTag])
 * applied after clearing. This allows the node to remain findable in tests via
 * `onNodeWithTag(..., useUnmergedTree = true)` even though it is hidden from accessibility.
 */
internal fun Modifier.hideFromAccessibility(semantics: (SemanticsPropertyReceiver.() -> Unit)? = null) = clearAndSetSemantics {
    hideFromAccessibility()
    semantics?.invoke(this)
}
