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

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.nice.cxonechat.Popup
import com.nice.cxonechat.message.Action
import com.nice.cxonechat.ui.R.string
import com.nice.cxonechat.ui.composable.generic.BottomSheetActionRow
import com.nice.cxonechat.ui.composable.generic.BottomSheetTitle
import com.nice.cxonechat.ui.composable.icons.ChatIcons
import com.nice.cxonechat.ui.composable.icons.outlined.ArrowRight
import com.nice.cxonechat.ui.composable.icons.outlined.Hourglass
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import com.nice.cxonechat.ui.composable.theme.ChatTheme.chatColors
import com.nice.cxonechat.ui.composable.theme.ChatTheme.chatTypography
import com.nice.cxonechat.ui.composable.theme.ChatTheme.colorScheme
import com.nice.cxonechat.ui.composable.theme.ChatTheme.space
import com.nice.cxonechat.ui.composable.theme.SubtleModalBottomSheet
import com.nice.cxonechat.ui.util.hideFromAccessibility
import com.nice.cxonechat.ui.util.toTimeStamp
import kotlin.time.Duration

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun InactivityContent(
    duration: State<Duration>,
    popup: Popup.InactivityPopup,
    onClickAction: (Action) -> Unit,
    closeChat: () -> Unit,
) {
    val isNotExpired = duration.value != Duration.ZERO
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true) {
        // Disable swipe to dismiss, user must choose an action
        it === SheetValue.Expanded
    }
    ChatTheme.SubtleModalBottomSheet(
        onDismissRequest = closeChat,
        sheetState = sheetState,
        content = {
            AnimatedContent(isNotExpired) { notExpired ->
                InactivityNonAnimatedContent(notExpired, duration, popup, onClickAction)
            }
        },
    )
}

@Composable
private fun InactivityNonAnimatedContent(
    notExpired: Boolean,
    duration: State<Duration>,
    popup: Popup.InactivityPopup,
    onClickAction: (Action) -> Unit,
) {
    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .fillMaxWidth()
            .padding(top = space.medium, bottom = space.xl)
            .semantics {
                testTag = "inactivity_popup"
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(space.large, Alignment.CenterVertically)
    ) {
        if (notExpired) {
            NonExpiredContent(duration, popup, onClickAction)
        } else {
            ExpiredContent()
        }
    }
}

@Composable
private fun ExpiredContent() {
    val titleText = stringResource(string.inactivity_time_up_title)
    val subtitleText = stringResource(string.inactivity_time_up_subtitle)
    val accessibilityText = "$titleText $subtitleText"
    BottomSheetTitle(
        message = titleText,
        modifier = Modifier.semantics(mergeDescendants = true) {
            contentDescription = accessibilityText
            testTag = "inactivity_expired_title"
        },
        bottomContent = {
            Text(
                text = subtitleText,
                modifier = Modifier.hideFromAccessibility(),
                style = chatTypography.bottomSheetSubtitleText,
                color = chatColors.token.content.tertiary,
            )
        },
        trailingContent = { InactivityIcon() },
    )
}

@Composable
private fun NonExpiredContent(duration: State<Duration>, popup: Popup.InactivityPopup, onClickAction: (Action) -> Unit) {
    val currentDuration by duration
    val locale = Locale.current
    val formatted = remember(currentDuration) { currentDuration.toTimeStamp(locale) }
    val title = popup.title + " " + formatted
    val subtitle = popup.body + " " + popup.callToAction
    val bottomSheetDescription = "$title, $subtitle"
    BottomSheetTitle(
        message = title,
        modifier = Modifier.semantics(mergeDescendants = true) {
            contentDescription = bottomSheetDescription
            testTag = "inactivity_popup_title"
        },
        bottomContent = {
            Text(
                text = subtitle,
                modifier = Modifier.hideFromAccessibility(),
                style = chatTypography.bottomSheetSubtitleText,
                color = chatColors.token.content.tertiary,
            )
        },
        trailingContent = { InactivityIcon() },
    )
    InactivityActionList(popup, onClickAction)
}

@Composable
@NonRestartableComposable
private fun InactivityIcon() {
    Icon(
        imageVector = ChatIcons.Hourglass,
        contentDescription = null,
        modifier = Modifier
            .size(space.agentImageSize)
            .background(color = chatColors.token.status.warningContainer, CircleShape)
            .padding(space.medium),
        tint = chatColors.token.status.onWarningContainer,
    )
}

@Composable
private fun InactivityActionList(
    popup: Popup.InactivityPopup,
    onClickAction: (Action) -> Unit,
) {
    val dividerColor = chatColors.token.border.default
    val iconMod = Modifier.fillMaxSize()
    Column(modifier = Modifier.fillMaxWidth()) {
        val refreshButton = popup.sessionRefresh as? Action.ReplyButton
        if (refreshButton != null) {
            BottomSheetActionRow(
                text = refreshButton.text,
                onClick = { onClickAction(refreshButton) },
                textColor = colorScheme.primary,
                testTag = "inactivity_popup_refresh_button",
                leadingContent = {
                    Icon(
                        painter = rememberVectorPainter(ChatIcons.ArrowRight),
                        contentDescription = null, // The text contains the meaning of the icon, so no need for a content description
                        tint = colorScheme.primary,
                        modifier = iconMod
                    )
                }
            )
            DividerItem(color = dividerColor)
        }
        val expireButton = popup.sessionExpire as? Action.ReplyButton
        if (expireButton != null) {
            BottomSheetActionRow(
                text = expireButton.text,
                onClick = { onClickAction(expireButton) },
                textColor = chatColors.token.content.secondary,
                testTag = "inactivity_popup_expire_button",
                leadingContent = {
                    Icon(
                        painter = rememberVectorPainter(Icons.Default.Close),
                        contentDescription = null, // Decorative icon, content description is provided by the text
                        tint = colorScheme.tertiary,
                        modifier = iconMod
                    )
                }
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun PreviewExpiredContent() {
    val duration = remember { mutableStateOf(Duration.parse("0s")) }
    ChatTheme {
        Surface(Modifier.systemBarsPadding(), color = colorScheme.background) {
            InactivityNonAnimatedContent(false, duration, createPreviewPopup(10)) { }
        }
    }
}

@PreviewLightDark
@Composable
private fun PreviewNonExpiredContent() {
    val duration = remember { mutableStateOf(Duration.parse("10s")) }
    ChatTheme {
        Surface(Modifier.systemBarsPadding(), color = colorScheme.background) {
            InactivityNonAnimatedContent(true, duration, createPreviewPopup(10)) { }
        }
    }
}
