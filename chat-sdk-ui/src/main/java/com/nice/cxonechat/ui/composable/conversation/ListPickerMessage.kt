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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import com.nice.cxonechat.ui.R
import com.nice.cxonechat.ui.R.string
import com.nice.cxonechat.ui.composable.conversation.model.Action
import com.nice.cxonechat.ui.composable.conversation.model.Action.ReplyButton
import com.nice.cxonechat.ui.composable.conversation.model.Message.ListPicker
import com.nice.cxonechat.ui.composable.generic.forwardingPainter
import com.nice.cxonechat.ui.composable.theme.ChatColors.ColorPair
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import com.nice.cxonechat.ui.composable.theme.ChatTheme.chatTypography
import com.nice.cxonechat.ui.composable.theme.ChatTheme.colorScheme
import com.nice.cxonechat.ui.composable.theme.ChatTheme.space
import com.nice.cxonechat.ui.composable.theme.ChatTheme.typography
import com.nice.cxonechat.ui.composable.theme.Dialog
import com.nice.cxonechat.ui.util.preview.message.SdkAction
import com.nice.cxonechat.ui.util.preview.message.UiSdkListPicker
import com.nice.cxonechat.ui.util.preview.message.UiSdkReplyButton

@Composable
internal fun ListPickerMessage(
    message: ListPicker,
    textColor: ColorPair,
    modifier: Modifier = Modifier,
) {
    var showDialog by rememberSaveable { mutableStateOf(false) }
    val dismissDialog = { showDialog = false }
    val clickModifier = Modifier.clickable { showDialog = true }
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = clickModifier
                .padding(space.richListPickerTextPadding),
            horizontalAlignment = Alignment.Start,
        ) {
            Text(
                text = message.title,
                color = textColor.foreground,
                style = chatTypography.listPickerTitle,
                modifier = Modifier.padding(top = 1.dp)
            )
            Text(
                text = message.text,
                color = textColor.foreground,
                modifier = Modifier.alpha(0.5f),
                style = chatTypography.listPickerText
            )
            if (showDialog) {
                ListPickerDialog(message, dismissDialog)
            }
        }
        Spacer(modifier = Modifier.size(space.semiLarge))
        Image(
            painter = painterResource(R.drawable.ic_list_picker),
            contentDescription = null,
            modifier = clickModifier
                .size(space.listPickerIconSize)
                .padding(space.medium)
        )
    }
}

@Composable
private fun ListPickerDialog(message: ListPicker, dismissDialog: () -> Unit) {
    var selectedAction: Action? by remember { mutableStateOf(null) }
    ChatTheme.Dialog(
        title = message.title,
        titlePadding = 1.dp,
        onDismiss = dismissDialog,
        confirmButton = {
            TextButton(
                onClick = {
                    when (val action = selectedAction) {
                        is ReplyButton -> action.onClick()
                        else -> {}
                    }
                    dismissDialog()
                },
                enabled = selectedAction != null,
            ) {
                Text(stringResource(string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = dismissDialog) {
                Text(stringResource(string.dismiss))
            }
        },
    ) {
        Text(
            text = message.text,
            style = typography.labelLarge,
            modifier = Modifier.padding(bottom = 10.dp)
        )
        LazyColumn {
            items(message.actions.size) { index ->
                val actionItem = message.actions[index]
                if (actionItem is ReplyButton) {
                    ActionListItem(
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
}

@Composable
private fun ActionListItem(
    action: ReplyButton,
    isSelected: Boolean,
    onSelect: (Action) -> Unit,
) {
    ListItem(
        colors = ListItemDefaults.colors(
            containerColor = if (isSelected) colorScheme.surfaceContainerHighest else colorScheme.surfaceContainer,
        ),
        headlineContent = { Text(action.text) },
        leadingContent = {
            if (action.media != null) {
                val placeholder = forwardingPainter(
                    painter = rememberVectorPainter(image = Icons.Default.Image),
                    colorFilter = ColorFilter.tint(colorScheme.onBackground)
                )
                SubcomposeAsyncImage(
                    model = action.media.url,
                    loading = { Image(painter = placeholder, contentDescription = null) },
                    error = { Image(painter = placeholder, contentDescription = null) },
                    contentDescription = action.description,
                    contentScale = ContentScale.FillBounds,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
            }
        },
        trailingContent = {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = stringResource(string.action_was_selected),
                    tint = colorScheme.primary
                )
            }
        },
        modifier = Modifier.clickable(onClick = { onSelect(action) }),
    )
}

@PreviewLightDark
@Composable
private fun ListPickerMessagePreview() {
    PreviewMessageItemBase(
        message = ListPicker(UiSdkListPicker()) {},
    )
}

@PreviewLightDark
@Composable
private fun ListPickerDialogPreview() {
    val listPicker = ListPicker(
        UiSdkListPicker(
            actions = buildList {
                repeat(7) {
                    add(
                        UiSdkReplyButton(
                            text = "Action $it",
                            mediaUrl = "https://http.cat/" + (400 + it)
                        ) as SdkAction
                    )
                }
            }
        )
    ) {}
    ChatTheme {
        Surface {
            ListPickerDialog(
                message = listPicker,
                dismissDialog = {}
            )
        }
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
