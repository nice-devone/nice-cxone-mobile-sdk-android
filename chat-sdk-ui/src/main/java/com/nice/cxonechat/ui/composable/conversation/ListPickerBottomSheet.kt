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

package com.nice.cxonechat.ui.composable.conversation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue.Expanded
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import com.nice.cxonechat.ui.R.string
import com.nice.cxonechat.ui.composable.conversation.model.Action
import com.nice.cxonechat.ui.composable.conversation.model.Action.ReplyButton
import com.nice.cxonechat.ui.composable.conversation.model.Message.ListPicker
import com.nice.cxonechat.ui.composable.generic.forwardingPainter
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import com.nice.cxonechat.ui.composable.theme.ChatTheme.chatColors
import com.nice.cxonechat.ui.composable.theme.ChatTheme.chatTypography
import com.nice.cxonechat.ui.composable.theme.ChatTheme.colorScheme
import com.nice.cxonechat.ui.composable.theme.ChatTheme.space
import com.nice.cxonechat.ui.util.preview.message.UiSdkListPicker

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ListPickerBottomSheet(message: ListPicker, onDismiss: () -> Unit, onDone: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true) {
        it === Expanded
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = chatColors.token.background.default,
        contentColor = chatColors.token.content.primary,
        dragHandle = {
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(chatColors.token.background.surface.subtle)
            ) {
                Surface(
                    modifier =
                        Modifier
                            .padding(vertical = space.large)
                            .semantics {
                                contentDescription = "DragHandle"
                            }
                            .align(Alignment.Center),
                    color = chatColors.token.content.tertiary,
                    shape = MaterialTheme.shapes.extraLarge,
                ) {
                    Box(Modifier.size(width = 32.dp, height = 4.dp))
                }
            }
        },
        modifier = Modifier
            .testTag("list_picker_bottom_sheet")
    ) {
        ListPickerBottomSheetContent(message = message, onDismiss = onDismiss, onDone = onDone)
    }
}

@Suppress("LongMethod")
@Composable
internal fun ListPickerBottomSheetContent(
    message: ListPicker,
    onDismiss: () -> Unit,
    onDone: () -> Unit,
) {
    var selectedAction: Action? by remember { mutableStateOf(null) }
    Column(Modifier.background(chatColors.token.background.surface.subtle)) {
        Column(
            modifier = Modifier
                .padding(start = space.large, top = space.large, end = space.large)
        ) {
            Text(
                text = message.title,
                style = chatTypography.bottomSheetTitleText,
                modifier = Modifier
                    .testTag("list_picker_title")
                    .padding(start = space.large),
                color = chatColors.token.content.primary
            )
            Text(
                text = message.text,
                color = chatColors.token.content.secondary,
                style = chatTypography.listPickerBottomSheetSubtitleText,
                modifier = Modifier
                    .padding(bottom = space.large, start = space.large)
                    .testTag("list_picker_subtitle")
            )
            LazyColumn {
                items(message.actions.size) { index ->
                    val actionItem = message.actions[index]
                    if (actionItem is ReplyButton) {
                        AlignedListItem(
                            action = actionItem,
                            isSelected = selectedAction == actionItem,
                            onSelect = { action: Action ->
                                selectedAction = if (selectedAction == action) null else action
                            }
                        )
                    }
                }
            }
        }
        HorizontalDivider(color = chatColors.token.border.default)
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .background(chatColors.token.background.default)
        ) {
            BottomSheetButtonText(
                text = stringResource(string.cancel),
                enable = true,
                onClick = { onDismiss() }
            )
            BottomSheetButtonText(
                text = stringResource(string.done),
                enable = selectedAction != null,
                onClick = {
                    (selectedAction as? ReplyButton)?.onClick?.let { it() }
                    onDone()
                }
            )
        }
    }
}

@Composable
private fun BottomSheetButtonText(
    text: String,
    enable: Boolean,
    onClick: () -> Unit,
) {
    TextButton(
        modifier = Modifier.padding(horizontal = space.large),
        onClick = onClick,
        enabled = enable
    ) {
        Text(text)
    }
}

@Composable
internal fun AlignedListItem(
    action: ReplyButton,
    isSelected: Boolean,
    onSelect: (Action) -> Unit,
) {
    val containerColor = if (isSelected) chatColors.token.background.surface.emphasis else chatColors.token.background.surface.subtle
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 55.dp)
            .background(containerColor)
            .testTag("list_picker_item")
            .clickable { onSelect(action) }
            .padding(horizontal = space.large, vertical = space.medium),
        verticalAlignment = Alignment.CenterVertically
    ) {
        action.media?.let {
            val placeholder = forwardingPainter(
                painter = rememberVectorPainter(image = Icons.Default.Image),
                colorFilter = ColorFilter.tint(colorScheme.onBackground)
            )
            SubcomposeAsyncImage(
                model = it.url,
                loading = { Image(painter = placeholder, contentDescription = null) },
                error = { Image(painter = placeholder, contentDescription = null) },
                contentDescription = action.description,
                contentScale = ContentScale.FillBounds,
                modifier = Modifier
                    .size(space.listPickerItemImageSize)
                    .clip(RoundedCornerShape(space.medium))
            )

            Spacer(modifier = Modifier.width(space.semiLarge))
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = action.text,
                color = chatColors.token.content.primary,
                style = chatTypography.bottomSheetActionRowText
            )
            if (action.description != null) {
                Text(
                    text = action.description,
                    color = chatColors.token.content.secondary,
                    style = chatTypography.listPickerText,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }

        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = stringResource(string.action_was_selected),
                tint = chatColors.token.brand.primary
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun PreviewListPickerBottomSheet() {
    val listPicker = ListPicker(UiSdkListPicker()) {}
    ChatTheme {
        ListPickerBottomSheetContent(
            message = listPicker,
            onDismiss = {},
            onDone = {}
        )
    }
}

@PreviewLightDark
@Composable
private fun ActionListItemPreview() {
    val listPicker = ListPicker(UiSdkListPicker()) {}
    var selectedAction: Action? by remember { mutableStateOf(listPicker.actions.first()) }
    ChatTheme {
        Surface {
            Column {
                for (action: Action in listPicker.actions) {
                    ActionListItem(
                        action = action as ReplyButton,
                        isSelected = selectedAction == action,
                        onSelect = { selectedAction = if (selectedAction == action) null else action }
                    )
                }
            }
        }
    }
}
