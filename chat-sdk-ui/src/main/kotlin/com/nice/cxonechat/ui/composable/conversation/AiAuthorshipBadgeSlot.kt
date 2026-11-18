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

package com.nice.cxonechat.ui.composable.conversation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.semantics
import com.nice.cxonechat.ui.R.string
import com.nice.cxonechat.ui.composable.theme.ChatTheme.space

/**
 * AI authorship disclosure shown below the conversation title, in both the two-row and the single-row
 * top app bar.
 *
 * @param hasAiMessages Whether the conversation contains messages authored by AI.
 * @param singleRow Whether the hosting app bar is the fixed-height single-row variant. That bar cannot
 * grow, so the badge fades in and out instead of expanding the bar vertically.
 * @param modifier Modifier for the badge.
 */
@Composable
internal fun AiAuthorshipBadgeSlot(
    hasAiMessages: Boolean,
    singleRow: Boolean,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = hasAiMessages,
        enter = if (singleRow) fadeIn() else fadeIn() + expandVertically(expandFrom = Alignment.Top),
        exit = if (singleRow) fadeOut() else fadeOut() + shrinkVertically(shrinkTowards = Alignment.Top),
    ) {
        // Only the two-row bar needs the badge padded off its bottom edge.
        AiAuthorshipBadge(modifier = if (singleRow) modifier else modifier.padding(bottom = space.large))
    }
}

@Composable
private fun AiAuthorshipBadge(modifier: Modifier = Modifier) {
    val label = stringResource(string.ai_assistant_label)
    Box(
        modifier = modifier
            .testTag("chat_thread_top_bar_ai_assistant_badge")
            .background(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(space.small),
            )
            .padding(horizontal = space.small)
            .semantics(mergeDescendants = true) { contentDescription = label },
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.semantics { hideFromAccessibility() },
        )
    }
}
