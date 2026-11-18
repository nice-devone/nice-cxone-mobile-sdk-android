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
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewLightDark
import coil3.compose.SubcomposeAsyncImage
import com.nice.cxonechat.ui.composable.conversation.model.Action
import com.nice.cxonechat.ui.composable.conversation.model.Action.ReplyButton
import com.nice.cxonechat.ui.composable.conversation.model.Message.ListPicker
import com.nice.cxonechat.ui.composable.generic.forwardingPainter
import com.nice.cxonechat.ui.composable.generic.scrollbar.rememberScrollbarState
import com.nice.cxonechat.ui.composable.generic.scrollbar.scrollbar
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import com.nice.cxonechat.ui.composable.theme.ChatTheme.chatColors
import com.nice.cxonechat.ui.composable.theme.ChatTheme.chatTypography
import com.nice.cxonechat.ui.composable.theme.ChatTheme.colorScheme
import com.nice.cxonechat.ui.composable.theme.ChatTheme.space
import com.nice.cxonechat.ui.composable.theme.ModalBottomSheet
import com.nice.cxonechat.ui.util.preview.message.UiSdkListPicker

/**
 * Displays the list picker as the shared [ChatTheme.ModalBottomSheet], rather than building its own
 * header/footer chrome: the title becomes the sheet's non-scrolling title, and the subtitle scrolls
 * with the options, matching the design where "Please choose the best option below." sits inside the
 * scrollable menu list rather than the fixed header. Cancel and Submit are the only way to close the
 * sheet by pointer interaction, unlike the shared sheet's other users: its own design gives no way to
 * dismiss it by dragging or by tapping outside it, so both a swipe and a scrim tap - including, on a
 * wide screen, the strip either side of the width-capped sheet - are disabled. A back press still
 * dismisses it, as it always has.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ListPickerBottomSheet(message: ListPicker, onDismiss: () -> Unit, onDone: () -> Unit) {
    var selectedAction: Action? by remember { mutableStateOf(null) }
    ChatTheme.ModalBottomSheet(
        title = message.title,
        onDismiss = onDismiss,
        onSubmit = {
            (selectedAction as? ReplyButton)?.onClick?.let { it() }
            onDone()
        },
        canSubmit = selectedAction != null,
        dismissOnClickOutside = false,
        modifier = Modifier.testTag("list_picker_bottom_sheet"),
    ) { contentPadding ->
        ListPickerOptions(
            message = message,
            contentPadding = contentPadding,
            selectedAction = selectedAction,
            onSelect = { action -> selectedAction = if (selectedAction == action) null else action },
        )
    }
}

/** The subtitle and the selectable options, scrolling as one list with its own scrollbar. */
@Composable
private fun ListPickerOptions(
    message: ListPicker,
    contentPadding: PaddingValues,
    selectedAction: Action?,
    onSelect: (Action) -> Unit,
) {
    val listState = rememberLazyListState()
    val scrollbarState = rememberScrollbarState()
    LazyColumn(
        state = listState,
        contentPadding = contentPadding,
        modifier = Modifier
            .fillMaxSize()
            .scrollbar(listState, scrollbarState, Orientation.Vertical)
            .selectableGroup(),
    ) {
        item {
            Text(
                text = message.text,
                color = chatColors.token.content.secondary,
                style = chatTypography.listPickerBottomSheetSubtitleText,
                modifier = Modifier
                    .padding(bottom = space.large)
                    .testTag("list_picker_subtitle")
            )
        }
        items(message.actions.size) { index ->
            val actionItem = message.actions[index]
            if (actionItem is ReplyButton) {
                AlignedListItem(
                    action = actionItem,
                    isSelected = selectedAction == actionItem,
                    onSelect = onSelect,
                )
            }
        }
    }
}

@Composable
internal fun BottomSheetButtonText(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit,
    testTag: String,
) {
    TextButton(
        modifier = Modifier
            .padding(horizontal = space.large)
            .testTag(testTag),
        onClick = onClick,
        enabled = enabled,
        colors = ButtonDefaults.textButtonColors(
            contentColor = chatColors.token.brand.primary,
            disabledContentColor = chatColors.token.content.tertiary
        )
    ) {
        Text(text = text)
    }
}

@Composable
internal fun AlignedListItem(
    action: ReplyButton,
    isSelected: Boolean,
    onSelect: (Action) -> Unit,
) {
    val containerColor by animateColorAsState(
        targetValue = if (isSelected) {
            chatColors.token.background.surface.emphasis
        } else {
            chatColors.token.background.surface.subtle
        },
        label = "AlignedListItem containerColor"
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = space.clickableSize)
            .background(containerColor)
            .testTag("list_picker_item")
            .selectable(selected = isSelected, role = Role.RadioButton) { onSelect(action) }
            .padding(horizontal = space.semiLarge, vertical = space.medium)
            .semantics(mergeDescendants = true) {
                contentDescription = buildString {
                    append(action.text)
                    action.description?.let {
                        append(", $it")
                    }
                }
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        LeadingIcon(action.media?.url)

        Column(
            modifier = Modifier
                .weight(1f)
                .semantics(true) {
                    hideFromAccessibility() // Text and description are read as part of the content description of the item
                },
        ) {
            Text(
                text = action.text,
                color = chatColors.token.content.primary,
                style = chatTypography.bottomSheetActionRowText,
            )
            if (action.description != null) {
                Text(
                    text = action.description,
                    color = chatColors.token.content.secondary,
                    style = chatTypography.listPickerText,
                )
            }
        }
        TrailingIcon(isSelected)
    }
}

@Composable
private fun TrailingIcon(isSelected: Boolean) {
    Box(
        Modifier
            .padding(start = space.semiLarge)
            .size(space.xl)
            .testTag("list_picker_item_check")
    ) {
        AnimatedVisibility(isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null, // Decorative icon, the selected state is already conveyed through semantics
                tint = chatColors.token.brand.primary
            )
        }
    }
}

@Composable
private fun LeadingIcon(model: Any?) {
    model?.let {
        val placeholder = forwardingPainter(
            painter = rememberVectorPainter(image = Icons.Default.Image),
            colorFilter = ColorFilter.tint(colorScheme.onBackground)
        )
        SubcomposeAsyncImage(
            model = model,
            loading = { Image(painter = placeholder, contentDescription = null) },
            error = { Image(painter = placeholder, contentDescription = null) },
            contentDescription = null, // Decorative image, API doesn't provide meaningful content description - yet
            contentScale = ContentScale.FillBounds,
            modifier = Modifier
                .size(space.listPickerItemImageSize)
                .clip(RoundedCornerShape(space.medium))
        )

        Spacer(modifier = Modifier.width(space.semiLarge))
    }
}

@PreviewLightDark
@Composable
private fun PreviewListPickerBottomSheet() {
    val listPicker = ListPicker(UiSdkListPicker()) {}
    ChatTheme {
        Surface(
            modifier = Modifier
                .systemBarsPadding()
        ) {
            ListPickerBottomSheet(
                message = listPicker,
                onDismiss = {},
                onDone = {}
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun AlignedListItemPreview() {
    val listPicker = ListPicker(UiSdkListPicker()) {}
    var selectedAction: Action? by remember { mutableStateOf(listPicker.actions.first()) }
    ChatTheme {
        Surface(Modifier.systemBarsPadding()) {
            Column(Modifier.selectableGroup()) {
                for (action: Action in listPicker.actions) {
                    AlignedListItem(
                        action = action as ReplyButton,
                        isSelected = selectedAction == action,
                        onSelect = { selectedAction = if (selectedAction == action) null else action }
                    )
                }
            }
        }
    }
}
