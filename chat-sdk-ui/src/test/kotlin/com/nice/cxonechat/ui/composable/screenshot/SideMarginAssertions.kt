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

package com.nice.cxonechat.ui.composable.screenshot

import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.unit.Dp
import org.junit.Assert.assertEquals

/**
 * Asserts that [content] sits [expected] in from both the left and the right edge of [container].
 *
 * Both are measured in the window, so the nodes may live in a sheet's own dialog window. A half-pixel
 * tolerance absorbs rounding of dp values that are not whole pixels.
 */
internal fun ComposeTestRule.assertSideMargins(
    container: SemanticsNodeInteraction,
    content: SemanticsNodeInteraction,
    expected: Dp,
) {
    val containerBounds = container.fetchSemanticsNode().boundsInWindow
    val contentBounds = content.fetchSemanticsNode().boundsInWindow
    val expectedPx = with(density) { expected.toPx() }
    assertEquals("left margin", expectedPx, contentBounds.left - containerBounds.left, 0.5f)
    assertEquals("right margin", expectedPx, containerBounds.right - contentBounds.right, 0.5f)
}
