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

package com.nice.cxonechat.ui.composable.generic

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Arrangement.Absolute.spacedBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.nice.cxonechat.ui.R.string
import com.nice.cxonechat.ui.composable.icons.ChatIcons
import com.nice.cxonechat.ui.composable.icons.filled.AvatarWaiting
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import com.nice.cxonechat.ui.composable.theme.ChatTheme.chatColors
import com.nice.cxonechat.ui.composable.theme.ChatTheme.chatShapes
import com.nice.cxonechat.ui.composable.theme.ChatTheme.chatTypography
import com.nice.cxonechat.ui.composable.theme.ChatTheme.colorScheme
import com.nice.cxonechat.ui.composable.theme.ChatTheme.space
import com.nice.cxonechat.ui.composable.theme.PopupButton

/**
 * A popup that displays a title, icon, subtitle and content and imitates [androidx.compose.ui.window.Dialog] modal behavior.
 * For details of the layout see [ChatPopupContent].
 * The popup is displayed at the top-center of the screen with a semi-transparent gray background and it is dismissible by back press.
 *
 * @param title The title displayed in the layout under the header, with prominent styling.
 * @param icon The icon displayed in the header, the supplied [Painter] will be tinted.
 * @param modifier The modifier to applied to the [ChatPopupContent] layout.
 * @param iconContentDescription The content description for the [icon].
 * @param subtitle The optional subtitle displayed in the layout under the title, with less prominent styling.
 * @param onDismissRequest The callback that is called when the popup is dismissed.
 * @param content The optional content displayed in the layout under the [subtitle] (or title if no [subtitle] is provided).
 */
@Composable
internal fun ChatPopup(
    title: String,
    icon: Painter,
    modifier: Modifier = Modifier,
    iconContentDescription: String? = null,
    subtitle: String? = null,
    onDismissRequest: (() -> Unit)? = null,
    content: (@Composable () -> Unit)? = null,
) {
    Popup(
        popupPositionProvider = OffsetPositionProvider(),
        properties = PopupProperties(
            clippingEnabled = false,
            focusable = false,
            dismissOnClickOutside = false,
            dismissOnBackPress = true,
        ),
        onDismissRequest = onDismissRequest,
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .background(Color.Gray.copy(alpha = 0.5f))
                .fillMaxSize(),
            contentAlignment = Alignment.TopCenter
        ) {
            ChatPopupContent(
                title = title,
                subtitle = subtitle,
                icon = icon,
                iconContentDescription = iconContentDescription,
                modifier = modifier,
                collapsePopupHeight = this.maxHeight,
                content = content
            )
        }
    }
}

/**
 * A layout commonly used for the [ChatPopup] without the popup behavior.
 * The base of the layout is a [ElevatedCard] with a [Column] which contains a Header with slot for [icon] and
 * body with slots for [title], [subtitle] and [content].
 *
 * +--------------------------------+
 * |                                |
 * |            Icon                |
 * +--------------------------------+
 * |                                |
 * |            Title               |
 * |          *Subtitle*            |
 * |                                |
 * |           content              |
 * +--------------------------------+
 *
 *
 * @param title The title displayed in the layout under the header, with prominent styling.
 * @param icon The icon displayed in the header, the supplied [Painter] will be tinted with the
 * [com.nice.cxonechat.ui.composable.theme.ThemeColorTokens.Brand.onPrimary] color.
 * The icon is placed in prominent position in the header.
 * @param collapsePopupHeight The height of the popup at which the layout will collapse the header in order to be smaller.
 * @param modifier The modifier to applied to the [ElevatedCard] layout.
 * @param iconContentDescription The content description for the icon.
 * @param subtitle The optional subtitle displayed in the layout under the title, with less prominent styling.
 * @param content The optional content displayed in the layout under the [subtitle] (or title if no [subtitle] is provided).
 */
@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
internal fun ChatPopupContent(
    title: String,
    icon: Painter,
    collapsePopupHeight: Dp,
    modifier: Modifier = Modifier,
    iconContentDescription: String? = null,
    subtitle: String? = null,
    content: (@Composable () -> Unit)? = null,
) {
    ElevatedCard(
        shape = chatShapes.popupShape,
        modifier = modifier
            .padding(space.popupPaddingValues)
            .wrapContentHeight(align = Alignment.Top)
            .wrapContentWidth(align = Alignment.CenterHorizontally)
            .sizeIn(maxWidth = space.popupMaxWidth),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = space.popupElevation),
    ) {
        Column(modifier = Modifier.background(chatColors.token.brand.primary)) {
            val windowImeInsets = WindowInsets.ime.asPaddingValues()
            val insets = windowImeInsets.calculateBottomPadding() + windowImeInsets.calculateTopPadding()
            val availableHeight = collapsePopupHeight - insets
            var layoutSize by remember { mutableStateOf(0.dp) }

            val popupHeaderHeight = space.popupHeaderHeight
            val showHeader = availableHeight >= layoutSize
            ChatPopupHeader(showHeader, icon, iconContentDescription)
            Surface(
                color = colorScheme.background.copy(alpha = if (isSystemInDarkTheme()) 0.75f else 1f),
                contentColor = colorScheme.onBackground,
                modifier = Modifier.onGloballyPositioned { coordinates ->
                    // Capture the size of the content to adjust the popup height accordingly.
                    // Always add the header height to the content height to ensure that header will fit into bounds.
                    layoutSize = popupHeaderHeight + coordinates.size.height.dp
                }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(space.popupContentPaddingValues),
                    verticalArrangement = spacedBy(space.medium),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = title,
                        style = chatTypography.popupTitle,
                    )
                    if (subtitle != null) {
                        Text(
                            text = subtitle,
                            style = chatTypography.bottomSheetSubtitleText,
                            modifier = Modifier.alpha(0.5f)
                        )
                    }
                    if (content != null) {
                        Spacer(modifier = Modifier.size(Dp.Hairline))
                        content()
                    }
                }
            }
        }
    }
}

@Composable
private fun ColumnScope.ChatPopupHeader(showHeader: Boolean, icon: Painter, iconContentDescription: String?) {
    AnimatedVisibility(visible = showHeader) {
        Box(
            modifier = Modifier
                .height(space.popupHeaderHeight)
                .padding(space.popupHeaderPaddingValues)
                .fillMaxWidth(),
            contentAlignment = Alignment.BottomCenter
        ) {
            Icon(
                painter = icon,
                contentDescription = iconContentDescription,
                tint = chatColors.token.brand.onPrimary
            )
        }
    }
}

/**
 * A popup position provider that only uses offset.
 * The default position provider is not used because it provides incorrect position (half of popup is offscreen) due to the incorrect anchor
 * calculation.
 */
private class OffsetPositionProvider(
    val offset: IntOffset = IntOffset.Zero,
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val resolvedUserOffset = IntOffset(
            offset.x * if (layoutDirection == LayoutDirection.Ltr) 1 else -1,
            offset.y
        )

        return anchorBounds.topLeft + resolvedUserOffset
    }
}

@Preview
@Composable
private fun ChatPopupPreview() {
    ChatTheme {
        Surface {
            Column {
                // Background content
                Text("Background content")
                Spacer(
                    Modifier
                        .fillMaxSize()
                        .background(Color.Transparent)
                )

                ChatPopup(
                    icon = rememberVectorPainter(ChatIcons.AvatarWaiting),
                    title = "You Chatted With",
                    subtitle = "Lukas Sanda",
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(space.semiLarge)
                    ) {
                        PopupButton("Start a New Chat") {}
                        PopupButton("Back to Conversation") {}
                        PopupButton("Close the Chat", colors = ButtonDefaults.buttonColors(containerColor = colorScheme.error)) {}
                    }
                }
                Text("Background content")
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun ChatPopupContentPreview() {
    ChatTheme {
        Surface(Modifier.systemBarsPadding()) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Background content
                Text("Background content", modifier = Modifier.fillMaxWidth())
                BoxWithConstraints {
                    ChatPopupContent(
                        title = stringResource(string.position_in_queue_next),
                        icon = rememberVectorPainter(ChatIcons.AvatarWaiting),
                        subtitle = stringResource(string.position_in_queue_supporting_text),
                        collapsePopupHeight = this.maxHeight
                    )
                }
                Text("Background content", modifier = Modifier.fillMaxWidth())
                TextField(rememberTextFieldState(), placeholder = { Text("Type here to display keyboard") })
            }
        }
    }
}
