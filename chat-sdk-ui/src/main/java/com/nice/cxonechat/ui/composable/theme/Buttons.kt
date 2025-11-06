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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.Icons.AutoMirrored
import androidx.compose.material.icons.Icons.Filled
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.nice.cxonechat.ui.R.string
import com.nice.cxonechat.ui.composable.theme.ChatTheme.chatColors
import com.nice.cxonechat.ui.composable.theme.ChatTheme.chatShapes
import com.nice.cxonechat.ui.composable.theme.ChatTheme.chatTypography
import com.nice.cxonechat.ui.composable.theme.ChatTheme.colorScheme
import com.nice.cxonechat.ui.composable.theme.ChatTheme.space

@Composable
internal fun ChatTheme.buttonColors(isDefault: Boolean): ButtonColors {
    val background = if (isDefault) colorScheme.primary else colorScheme.background
    return ButtonDefaults.buttonColors(
        containerColor = background,
        contentColor = contentColorFor(background)
    )
}

@Composable
internal fun ChatTheme.OutlinedButton(
    text: String,
    modifier: Modifier = Modifier,
    isDefault: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.testTag("outlined_button_$text"),
        enabled = enabled,
        colors = buttonColors(isDefault),
    ) {
        Text(text)
    }
}

@Composable
internal fun ChatTheme.ButtonRow(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = modifier
            .testTag("button_row")
            .fillMaxWidth()
            .padding(top = space.large),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(space = space.medium),
            verticalAlignment = Alignment.CenterVertically,
            content = content,
        )
    }
}

@Composable
internal fun ChatIconButton(
    icon: ImageVector,
    description: String,
    modifier: Modifier = Modifier,
    iconModifier: Modifier = Modifier,
    colors: IconButtonColors = IconButtonDefaults.filledIconButtonColors(),
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    FilledIconButton(
        onClick = onClick,
        shape = chatShapes.actionButtonShape,
        modifier = modifier
            .minimumInteractiveComponentSize()
            .size(44.dp)
            .testTag("chat_icon_button_${description.ifBlank { "icon" }}"),
        colors = colors,
        enabled = enabled,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            modifier = iconModifier,
        )
    }
}

@Composable
internal fun SendButton(
    enabled: Boolean = true,
    onMessageSent: () -> Unit,
) {
    ChatIconButton(
        enabled = enabled,
        icon = AutoMirrored.Default.Send,
        description = stringResource(string.text_send),
        modifier = Modifier.testTag("send_button"),
        iconModifier = Modifier
            .padding(10.dp)
            .rotate(-45f)
            .offset(2.5.dp),
        colors = IconButtonDefaults.filledIconButtonColors(
            contentColor = colorScheme.onPrimary,
            disabledContentColor = chatColors.token.content.secondary
        ),
        onClick = onMessageSent
    )
}

@Composable
internal fun BackButton(onClick: () -> Unit) {
    IconButton(
        onClick,
        modifier = Modifier.testTag("back_button")
    ) {
        Icon(
            AutoMirrored.Default.ArrowBack,
            stringResource(string.content_description_back_button)
        )
    }
}

@Composable
internal fun ShareButton(
    title: String? = null,
    onShare: () -> Unit,
) {
    IconButton(
        onClick = onShare,
        modifier = Modifier.testTag("share_button")
    ) {
        Icon(
            imageVector = Filled.Share,
            contentDescription = stringResource(string.share_attachment, title.orEmpty())
        )
    }
}

@Composable
internal fun PopupButton(
    text: String,
    modifier: Modifier = Modifier,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .testTag("popup_button")
            .fillMaxWidth()
            .sizeIn(minHeight = space.popupButtonMinHeight),
        shape = chatShapes.popupButtonShape,
        colors = colors,
    ) {
        Text(text = text, style = chatTypography.popupButton)
    }
}

@PreviewLightDark
@Composable
internal fun PreviewButtons() {
    ChatTheme {
        Surface(
            modifier = Modifier.semantics {
                testTagsAsResourceId = true
                contentDescription = "PreviewButtons"
            },
        ) {
            Column {
                ChatTheme.ButtonRow {
                    ChatTheme.OutlinedButton("Default", isDefault = true, onClick = { })
                    ChatTheme.OutlinedButton("Normal", onClick = { })
                }
                ChatIconButton(
                    icon = Icons.Default.Add,
                    description = ""
                ) {}
                SendButton(false) {}
                BackButton {}
                ShareButton {}
            }
        }
    }
}
