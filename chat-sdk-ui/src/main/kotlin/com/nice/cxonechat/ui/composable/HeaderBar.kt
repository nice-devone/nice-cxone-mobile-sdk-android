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

package com.nice.cxonechat.ui.composable

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.nice.cxonechat.ui.R
import com.nice.cxonechat.ui.composable.icons.ChatIcons
import com.nice.cxonechat.ui.composable.icons.filled.Offline
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import com.nice.cxonechat.ui.composable.theme.ChatTheme.chatColors
import com.nice.cxonechat.ui.composable.theme.ChatTheme.chatShapes
import com.nice.cxonechat.ui.composable.theme.ChatTheme.chatTypography
import com.nice.cxonechat.ui.composable.theme.ChatTheme.space

/**
 * Content padding that keeps a header bar on the design.
 *
 * The current list item overload derives its geometry differently from the deprecated one it replaced, in
 * three ways that all moved these bars off the design: it computes a smaller minimum height, insets content
 * by 10dp (12dp for precision pointers) rather than the design's 8dp, and top aligns above a height
 * breakpoint these bars exceed. So height, alignment and the vertical inset are stated at the call sites
 * rather than inherited from defaults that have moved once already and are still flag gated in the alpha.
 *
 * Only the vertical inset is pinned, because only that one is in the design. The horizontal insets stay
 * inherited so the bars keep tracking Material's list item inset if it changes.
 */
@Composable
internal fun headerBarContentPadding(): PaddingValues = PaddingValues(
    start = ListItemDefaults.ContentPadding.calculateStartPadding(LocalLayoutDirection.current),
    end = ListItemDefaults.ContentPadding.calculateEndPadding(LocalLayoutDirection.current),
    top = space.headerBarContentVerticalPadding,
    bottom = space.headerBarContentVerticalPadding,
)

/**
 * Extra gap after the leading icon.
 *
 * The current overload spaces slots 12dp apart where the deprecated one used 16dp, which pulled the text 4dp
 * left of the design. This restores the design's gap without depending on either default.
 */
@Composable
internal fun headerBarLeadingGap(): Modifier = Modifier.padding(end = space.headerBarLeadingGap)

@Composable
internal fun HeaderBar(
    containerColor: Color,
    titleText: String,
    messageText: String,
    modifier: Modifier = Modifier,
    titleModifier: Modifier = Modifier,
    leadingContent: @Composable (() -> Unit),
) {
    // The bar floats inside a horizontal margin and is capped at componentMaxWidth, so it never touches the
    // window edges and all four of its corners stay visible - hence the inset shape rather than the edge-to-edge one.
    // The bottom inset is pinned to the design's 10dp rather than the larger margin used on the other sides.
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = space.large, top = space.large, end = space.large, bottom = space.headerBarBottomPadding)
    ) {
        Surface(
            color = Color.Unspecified,
            shape = chatShapes.headerBarInsetShape,
            modifier = Modifier.widthIn(max = space.componentMaxWidth)
        ) {
            ListItem(
                modifier = modifier
                    .testTag("header_bar")
                    .heightIn(min = space.headerBarMinHeight),
                verticalAlignment = Alignment.CenterVertically,
                contentPadding = headerBarContentPadding(),
                colors = ListItemDefaults.colors(
                    containerColor = containerColor,
                ),
                leadingContent = {
                    Box(headerBarLeadingGap()) { leadingContent() }
                }
            ) {
                Column(verticalArrangement = Arrangement.Center) {
                    Text(
                        text = titleText,
                        color = chatColors.token.content.primary,
                        style = chatTypography.headerBarTitle,
                        modifier = titleModifier
                    )
                    Text(
                        text = messageText,
                        color = chatColors.token.content.secondary,
                        modifier = Modifier.padding(top = space.small),
                        style = chatTypography.headerBarMessage
                    )
                }
            }
        }
    }
}

/* Sample Icon for the leadingContent slot Preview */
@Composable
internal fun LeadingContentPreviewContent() {
    Box(
        modifier = Modifier
            .padding(vertical = space.large, horizontal = space.medium)
            .size(space.positionInQueueIconSize)
            .background(color = chatColors.token.brand.primary, shape = CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = ChatIcons.Offline,
            contentDescription = stringResource(R.string.offline),
            tint = chatColors.token.status.onError,
            modifier = Modifier
                .fillMaxSize()
                .padding(space.headerBarLeadingIconPadding)
        )
    }
}

@PreviewLightDark
@Composable
private fun PreviewHeaderBar() {
    ChatTheme {
        HeaderBar(
            containerColor = chatColors.token.background.surface.emphasis,
            titleText = stringResource(id = R.string.position_in_queue_next),
            messageText = stringResource(id = R.string.position_in_queue_supporting_text),
            leadingContent = { LeadingContentPreviewContent() }
        )
    }
}
