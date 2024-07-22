/*
 * Copyright (c) 2021-2024. NICE Ltd. All rights reserved.
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

package com.nice.cxonechat.ui.composable.generic

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.VectorPainter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat.Type
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import com.nice.cxonechat.ui.util.findActivity

/**
 * View which will fill out all available space and it will activity title to [title] if it is supplied, until
 * the composable is disposed.
 */
@Composable
internal fun FullscreenView(
    title: String?,
    onExitFullScreen: () -> Unit,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxSize()
    ) {
        BackHandler(onBack = onExitFullScreen)
        HideSystemUi()
        if (title != null) {
            TemporaryActivityTitle(title)
        }
        content()
    }
}

/**
 * Wraps [content] in a [Box] with [FullscreenButton] displayed over [content] with [Alignment.BottomEnd].
 */
@Composable
internal fun FullscreenButtonWrapper(
    isFullScreen: Boolean,
    onTriggerFullScreen: (Boolean) -> Unit,
    content: @Composable BoxScope.() -> Unit,
) {
    Box {
        content()
        Box(modifier = Modifier.align(Alignment.BottomEnd)) {
            AnimatedContent(
                targetState = isFullScreen,
                label = "button_is_fullscreen"
            ) { targetState ->
                if (targetState) {
                    FullscreenExitButton(onTriggerFullScreen)
                } else {
                    FullscreenButton(onTriggerFullScreen)
                }
            }
        }
    }
}

@Composable
private fun FullscreenButton(onClick: (Boolean) -> Unit) = FullscreenButton(
    icon = rememberVectorPainter(image = Icons.Default.FullscreenExit),
    iconOnPressed = rememberVectorPainter(image = Icons.Default.Fullscreen),
    isFullScreenDefault = false,
    onClick = onClick,
)

@Composable
private fun FullscreenExitButton(onClick: (Boolean) -> Unit) = FullscreenButton(
    icon = rememberVectorPainter(image = Icons.Default.Fullscreen),
    iconOnPressed = rememberVectorPainter(image = Icons.Default.FullscreenExit),
    isFullScreenDefault = true,
    onClick = onClick
)

/**
 * Turn-on immersive mode until the composable is disposed.
 */
@Composable
private fun HideSystemUi() {
    val context = LocalContext.current
    val view = LocalView.current
    DisposableEffect(context, view) {
        val activity = context.findActivity() ?: return@DisposableEffect onDispose { }
        val windowInsetsController =
            WindowCompat.getInsetsController(activity.window, view)
        val types = Type.systemBars()
        windowInsetsController.hide(types)
        onDispose {
            windowInsetsController.show(types)
        }
    }
}

@Composable
private fun FullscreenButton(
    icon: VectorPainter,
    iconOnPressed: VectorPainter,
    modifier: Modifier = Modifier,
    isFullScreenDefault: Boolean,
    onClick: (Boolean) -> Unit,
) {
    val interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
    IconButton(
        modifier = modifier
            .background(
                shape = CircleShape,
                color = ChatTheme.colors.surface.copy(alpha = 0.6f)
            ),
        onClick = { onClick(!isFullScreenDefault) },
        interactionSource = interactionSource,
    ) {
        val isPressed by interactionSource.collectIsPressedAsState()
        AnimatedContent(targetState = isPressed, label = "isPressed") { pressed ->
            if (pressed) {
                Icon(painter = iconOnPressed, contentDescription = null)
            } else {
                Icon(painter = icon, contentDescription = null)
            }
        }
    }
}

@Composable
@Preview
private fun PreviewButton() {
    ChatTheme {
        Surface {
            var isFullscreen by remember { mutableStateOf(false) }
            FullscreenButtonWrapper(
                isFullscreen,
                onTriggerFullScreen = { isFullscreen = it }
            ) {
                Text("FullScreen: $isFullscreen")
            }
        }
    }
}
