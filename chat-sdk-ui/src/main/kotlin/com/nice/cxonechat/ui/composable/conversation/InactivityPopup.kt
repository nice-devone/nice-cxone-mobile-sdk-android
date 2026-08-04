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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.nice.cxonechat.Popup
import com.nice.cxonechat.message.Action
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import kotlinx.coroutines.delay
import org.jetbrains.annotations.VisibleForTesting
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.Instant
import kotlin.time.toDuration

@Composable
internal fun InactivityPopup(popup: Popup.InactivityPopup, onClickAction: (Action) -> Unit, closeChat: () -> Unit) {
    val end: Instant = remember {
        popup.countdown.startedAt + popup.countdown.timeoutSeconds.toDuration(DurationUnit.SECONDS)
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
@VisibleForTesting
internal fun Duration.setMinDuration(): Duration = if (isInfinite() || !isPositive()) {
    // If the duration is not valid just set it to zero
    Duration.ZERO
} else {
    this
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
