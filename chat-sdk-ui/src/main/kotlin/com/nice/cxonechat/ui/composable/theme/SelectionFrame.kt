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
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons.Outlined
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nice.cxonechat.ui.R
import com.nice.cxonechat.ui.composable.icons.ChatIcons
import com.nice.cxonechat.ui.composable.icons.filled.SelectionFrameEmpty
import com.nice.cxonechat.ui.composable.icons.notint.SelectionFrame
import com.nice.cxonechat.ui.composable.icons.notint.SelectionFrameDark

@Composable
internal fun ChatTheme.SelectionFrame(
    modifier: Modifier = Modifier,
    framed: Boolean = false,
    selectionCircle: Boolean = framed,
    selected: Boolean = false,
    color: Color = chatColors.token.border.default,
    content: @Composable () -> Unit,
) {
    ShapedFrame(
        modifier = modifier,
        framed = framed,
        color = color,
        content = content,
    ) {
        SelectionFrameOverlay(selectionCircle, selected, color)
    }
}

@Composable
internal fun ChatTheme.SelectionFrameOverlay(
    showSelectionOverlay: Boolean = false,
    selected: Boolean = false,
    color: Color = chatColors.token.border.default,
) {
    AnimatedVisibility(
        visible = showSelectionOverlay,
        enter = fadeIn() + scaleIn(),
        exit = scaleOut() + fadeOut()
    ) {
        val imageMod = Modifier
            .padding(10.dp)
            .size(24.dp)
        Crossfade(selected) { isSelected ->
            if (isSelected) {
                Image(
                    imageVector = if (isSystemInDarkTheme()) ChatIcons.SelectionFrameDark else ChatIcons.SelectionFrame,
                    contentDescription = stringResource(R.string.content_description_selection_frame),
                    modifier = imageMod,
                )
            } else {
                Icon(
                    imageVector = ChatIcons.SelectionFrameEmpty,
                    contentDescription = stringResource(R.string.content_description_selection_frame),
                    modifier = imageMod,
                    tint = color
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
    color: Color = chatColors.token.border.default,
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
                            border = BorderStroke(space.framePreviewWidth, color),
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
private fun PreviewSelectionFrame(
    @PreviewParameter(SelectionFrameParamsProvider::class) params: SelectionFrameParams,
) {
    ChatTheme {
        Surface {
            Column(
                modifier = Modifier
                    .padding(ChatTheme.space.small)
                    .width(IntrinsicSize.Max),
                verticalArrangement = Arrangement.spacedBy(ChatTheme.space.small),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("$params", fontSize = 10.sp)
                ChatTheme.SelectionFrame(
                    modifier = Modifier
                        .size(100.dp),
                    framed = params.framed,
                    selected = params.selected,
                    selectionCircle = params.selectionCircle,
                ) {
                    Icon(
                        Outlined.Favorite,
                        contentDescription = Outlined.Favorite.name,
                        Modifier.background(ChatTheme.chatColors.token.brand.primaryContainer.copy(alpha = 0.5f))
                    )
                }
            }
        }
    }
}

private data class SelectionFrameParams(
    val selected: Boolean,
    val framed: Boolean,
    val selectionCircle: Boolean,
)

private class SelectionFrameParamsProvider : PreviewParameterProvider<SelectionFrameParams> {
    override val values: Sequence<SelectionFrameParams>
        get() = sequence {
            for (selected in listOf(true, false)) {
                for (framed in listOf(true, false)) {
                    for (selectionCircle in listOf(true, false)) {
                        yield(SelectionFrameParams(selected, framed, selectionCircle))
                    }
                }
            }
        }
}
