/*
 * Copyright (c) 2021-2025. NICE Ltd. All rights reserved.
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

package com.nice.cxonechat.ui.composable.theme

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons.Outlined
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.nice.cxonechat.ui.R
import com.nice.cxonechat.ui.composable.theme.ChatTheme.chatShapes
import com.nice.cxonechat.ui.composable.theme.ChatTheme.space

@Composable
internal fun ChatTheme.SelectionFrame(
    modifier: Modifier = Modifier,
    framed: Boolean = false,
    selectionCircle: Boolean = framed,
    selected: Boolean = false,
    content: @Composable () -> Unit,
) {
    ShapedFrame(
        modifier = modifier,
        framed = framed,
        content = content,
    ) {
        SelectionFrameOverlay(selectionCircle, selected)
    }
}

@Composable
internal fun ChatTheme.SelectionFrameOverlay(
    showSelectionOverlay: Boolean = false,
    selected: Boolean = false,
) {
    AnimatedVisibility(
        visible = showSelectionOverlay,
        enter = fadeIn() + scaleIn(),
        exit = scaleOut() + fadeOut()
    ) {
        val imageMod = Modifier
            .padding(10.dp)
            .size(16.dp)
            .shadow(1.dp, shape = CircleShape)
        Crossfade(selected) { isSelected ->
            if (isSelected) {
                Image(
                    painter = painterResource(R.drawable.ic_selection_frame_selected),
                    contentDescription = stringResource(R.string.content_description_selection_frame_selected),
                    modifier = imageMod,
                )
            } else {
                Image(
                    painter = painterResource(R.drawable.ic_selection_frame),
                    contentDescription = stringResource(R.string.content_description_selection_frame),
                    modifier = imageMod,
                )
            }
        }
    }
}

@Composable
internal fun ChatTheme.ShapedFrame(
    modifier: Modifier = Modifier,
    framed: Boolean = false,
    shape: Shape = chatShapes.selectionFrame,
    content: @Composable () -> Unit,
    overlayContent: @Composable () -> Unit = {},
) {
    Box(
        contentAlignment = Alignment.TopEnd,
        modifier = modifier,
    ) {
        Box(
            propagateMinConstraints = true,
            modifier = Modifier
                .fillMaxSize()
                .let {
                    if (framed) {
                        it.border(
                            border = BorderStroke(space.framePreviewWidth, chatColors.leadingMessageIconBorder),
                            shape = shape
                        )
                    } else {
                        it
                    }
                }
                .padding(space.framePreviewWidth * 0.9f)
                .clip(shape),
        ) {
            content()
        }
        overlayContent()
    }
}

@Composable
@PreviewLightDark
private fun PreviewSelectionFrame() {
    var selected by remember { mutableStateOf(false) }
    var framed by remember { mutableStateOf(true) }
    var selectionCircle by remember { mutableStateOf(true) }
    ChatTheme {
        Surface {
            Column(
                modifier = Modifier
                    .padding(space.small)
                    .width(IntrinsicSize.Max),
                verticalArrangement = Arrangement.spacedBy(space.small),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                ChatTheme.SelectionFrame(
                    modifier = Modifier
                        .size(100.dp),
                    framed = framed,
                    selected = selected,
                    selectionCircle = selectionCircle,
                ) {
                    Icon(Outlined.Favorite, contentDescription = null, Modifier.background(Color.Green))
                }
                PreviewSwitchRow("Selected", selected, onCheckedChange = { selected = it })
                PreviewSwitchRow("Framed", framed, onCheckedChange = { framed = it })
                PreviewSwitchRow("Selection circle", selectionCircle, onCheckedChange = { selectionCircle = it })
            }
        }
    }
}

@Composable
private fun PreviewSwitchRow(
    text: String,
    selected: Boolean,
    onCheckedChange: (Boolean) -> Unit = {},
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .border(BorderStroke(space.framePreviewWidth, LocalContentColor.current.copy(alpha = 0.5f)), chatShapes.chip)
            .padding(horizontal = space.xSmall)
            .fillMaxWidth()
    ) {
        Text(text, modifier = Modifier.padding(horizontal = space.small))
        Switch(selected, onCheckedChange = onCheckedChange)
    }
}
