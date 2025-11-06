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

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.nice.cxonechat.Popup
import com.nice.cxonechat.Popup.InactivityPopup.Countdown
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
import com.nice.cxonechat.ui.util.preview.message.UiSdkReplyButton
import kotlinx.coroutines.delay
import java.util.Date
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlin.time.toDuration

@OptIn(ExperimentalTime::class)
@Composable
internal fun InactivityPopup(popup: Popup.InactivityPopup, onClickAction: (Action) -> Unit, closeChat: () -> Unit) {
    val end: Instant = remember {
        Instant.fromEpochMilliseconds(popup.countdown.startedAt.time) + popup.countdown.timeoutSeconds.toDuration(DurationUnit.SECONDS)
    }
    val now: Instant = Clock.System.now()
    val duration: MutableState<Duration> = remember(end) { mutableStateOf((end - now).setMinDuration()) }
    LaunchedEffect(duration) {
        while (duration.value.isFinite() && duration.value.isPositive() && duration.value != Duration.ZERO) {
            delay(1_000)
            duration.value = (end - Clock.System.now()).setMinDuration()
        }
    }

    InactivityContent(duration, popup, onClickAction, closeChat)
}

@Stable
private fun Duration.setMinDuration(): Duration = if (isInfinite() || !isPositive()) {
    // If the duration is not valid just set it to zero
    Duration.ZERO
} else {
    this
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InactivityContent(
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
                Column(
                    modifier = Modifier
                        .testTag("inactivity_popup")
                        .verticalScroll(rememberScrollState())
                        .fillMaxWidth()
                        .padding(top = space.medium, bottom = space.xl),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(space.large, Alignment.CenterVertically)
                ) {
                    if (notExpired) {
                        NonExpiredContent(duration, popup, onClickAction)
                    } else {
                        BottomSheetTitle(
                            message = stringResource(string.inactivity_time_up_title),
                            modifier = Modifier.testTag("inactivity_expired_title"),
                            bottomContent = {
                                Text(
                                    text = stringResource(string.inactivity_time_up_subtitle),
                                    style = chatTypography.bottomSheetSubtitleText,
                                    color = chatColors.token.content.tertiary,
                                )
                            },
                            trailingContent = { InactivityIcon() },
                        )
                    }
                }
            }
        },
    )
}

@Composable
private fun NonExpiredContent(duration: State<Duration>, popup: Popup.InactivityPopup, onClickAction: (Action) -> Unit) {
    val currentDuration = duration.value
    val minutes = currentDuration.inWholeMinutes
    val seconds = (currentDuration.inWholeSeconds % 60).toInt()
    val formatted = remember(minutes, seconds) { "%02d:%02d".format(minutes, seconds) }
    val title = popup.title + " " + formatted
    val subtitle = popup.body + " " + popup.callToAction
    BottomSheetTitle(
        message = title,
        modifier = Modifier.testTag("inactivity_popup_title"),
        bottomContent = {
            Text(
                text = subtitle,
                style = chatTypography.bottomSheetSubtitleText,
                color = chatColors.token.content.tertiary,
            )
        },
        trailingContent = {
            InactivityIcon()
        }
    )
    InactivityActionList(popup, onClickAction)
}

@Composable
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
                        contentDescription = stringResource(string.close_chat),
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
                        contentDescription = stringResource(string.close_chat),
                        tint = colorScheme.tertiary,
                        modifier = iconMod
                    )
                }
            )
        }
    }
}

@Composable
@PreviewLightDark
private fun PreviewInactivityPopup() {
    ChatTheme {
        Surface(Modifier.systemBarsPadding()) {
            InactivityPopup(
                popup = createPreviewPopup(),
                onClickAction = {},
                closeChat = {},
            )
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                Text("This is a preview of the Inactivity Popup")
            }
        }
    }
}

@Composable
@PreviewLightDark
private fun PreviewInactivityPopupExpired() {
    ChatTheme {
        Surface(Modifier.systemBarsPadding()) {
            InactivityPopup(
                popup = createPreviewPopup(0),
                onClickAction = {},
                closeChat = {},
            )
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                Text("This is a preview of the Inactivity Popup")
            }
        }
    }
}

@Stable
private fun createPreviewPopup(timeoutSeconds: Long = 20): Popup.InactivityPopup = object : Popup.InactivityPopup {
    override val title: String = "Your chat will expire in"
    override val body: String = "When the time expires, the conversation is terminated. Would you like to continue?"
    override val countdown: Countdown = object : Countdown {
        override val timeoutSeconds: Long = timeoutSeconds
        override val startedAt: Date = Date()
    }
    override val callToAction: String = "Please respond to continue."
    override val sessionExpire: Action = UiSdkReplyButton("Close Chat")
    override val sessionRefresh: Action = UiSdkReplyButton("Continue")
}
