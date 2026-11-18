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

package com.nice.cxonechat.ui.composable

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.style.TextOverflow.Companion.Ellipsis
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.LayoutDirection
import com.nice.cxonechat.message.MessageDirection.ToClient
import com.nice.cxonechat.ui.R.string
import com.nice.cxonechat.ui.composable.conversation.ArchiveThreadMenu
import com.nice.cxonechat.ui.composable.conversation.ChangeThreadNameMenu
import com.nice.cxonechat.ui.composable.generic.MessageAvatar
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import com.nice.cxonechat.ui.composable.theme.ChatTheme.chatColors
import com.nice.cxonechat.ui.composable.theme.ChatTheme.chatShapes
import com.nice.cxonechat.ui.composable.theme.ChatTheme.chatTypography
import com.nice.cxonechat.ui.composable.theme.ChatTheme.colorScheme
import com.nice.cxonechat.ui.composable.theme.ChatTheme.space
import com.nice.cxonechat.ui.domain.model.Thread
import com.nice.cxonechat.ui.util.toShortTimeString

/**
 * Renders a single thread list cell.
 *
 * @param thread The thread to render.
 * @param onThreadSelected Invoked when the cell is tapped.
 * @param modifier [Modifier] applied to the cell.
 * @param onArchiveThread When non-null, a long-press context menu offers an "Archive" action for this thread.
 * Passing null suppresses the action (e.g. for already-archived threads).
 * @param onRenameThread When non-null, a long-press context menu offers a "Rename" action for this thread.
 * Passing null suppresses the action.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun ChatThreadView(
    thread: Thread,
    onThreadSelected: (Thread) -> Unit,
    modifier: Modifier = Modifier,
    onArchiveThread: ((Thread) -> Unit)? = null,
    onRenameThread: ((Thread) -> Unit)? = null,
) {
    val isUnread = thread.isUnread
    val accessibilityDescription = threadAccessibilityDescription(thread, isUnread)

    // Archive is only meaningful for threads that still accept messages.
    val canArchive = onArchiveThread != null && thread.chatThread.canAddMoreMessages
    val hasContextMenu = onRenameThread != null || canArchive
    // The pointer position and cell bounds are needed only to anchor the context menu, and only once
    // it opens. Keep them in a plain (non-snapshot) holder that the pointer/layout callbacks write on
    // every scroll frame WITHOUT invalidating composition, and snapshot them into state only on
    // long-press. Writing snapshot state from onGloballyPositioned/pointer events would recompose
    // every visible cell on every scroll frame — this is the thread list's primary scroll path.
    val pendingAnchor = remember { MenuAnchorHolder() }
    var menuAnchor by remember { mutableStateOf<MenuAnchor?>(null) }
    val menuExpanded = menuAnchor != null
    val backgroundColor = if (menuExpanded) {
        chatColors.token.background.surface.variant
    } else {
        chatColors.token.background.default
    }

    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(modifier)
                .then(
                    if (hasContextMenu) {
                        Modifier
                            .trackPointerPosition { pendingAnchor.touchPosition = it }
                            .onGloballyPositioned { pendingAnchor.cellBounds = it.boundsInWindow() }
                    } else {
                        Modifier
                    }
                )
                .combinedClickable(
                    onClick = { onThreadSelected(thread) },
                    onLongClick = if (hasContextMenu) {
                        { menuAnchor = pendingAnchor.snapshot() }
                    } else {
                        null
                    },
                )
                .background(backgroundColor)
                .semantics {
                    contentDescription = accessibilityDescription
                    testTag = "chat_thread_view_${thread.id}"
                },
        ) {
            ChatThreadRowContent(thread, isUnread)
        }

        if (hasContextMenu) {
            ChatThreadContextMenu(
                thread = thread,
                expanded = menuExpanded,
                touchPosition = menuAnchor?.touchPosition ?: Offset.Zero,
                cellBounds = menuAnchor?.cellBounds ?: Rect.Zero,
                onDismiss = { menuAnchor = null },
                onArchiveThread = onArchiveThread.takeIf { canArchive },
                onRenameThread = onRenameThread,
            )
        }
    }
}

/**
 * Mutable, non-snapshot holder for the latest pointer position and cell bounds. Writes here never
 * invalidate composition, so the pointer and layout callbacks can update it freely while scrolling;
 * the values are lifted into snapshot state (via [snapshot]) only when the context menu opens.
 */
private class MenuAnchorHolder {
    var touchPosition: Offset = Offset.Zero
    var cellBounds: Rect = Rect.Zero

    fun snapshot(): MenuAnchor = MenuAnchor(touchPosition, cellBounds)
}

/** Immutable anchor captured at the moment the context menu opens. */
private data class MenuAnchor(val touchPosition: Offset, val cellBounds: Rect)

/**
 * The avatar and text content of a thread list cell.
 */
@Composable
private fun RowScope.ChatThreadRowContent(thread: Thread, isUnread: Boolean) {
    MessageAvatar(
        agent = thread.agent,
        modifier = Modifier.padding(start = space.large, top = space.semiLarge),
        size = space.agentImageSize,
    )
    Spacer(modifier = Modifier.width(space.large))
    Column(
        modifier = Modifier
            .weight(1f)
            .padding(vertical = space.semiLarge)
    ) {
        ChatThreadHeader(thread, isUnread)
        ChatThreadLastMessage(thread, isUnread)
    }
}

/**
 * Long-press context menu for a thread list cell, offering the same actions as the swipe gestures.
 *
 * @param thread The thread the menu acts on.
 * @param expanded Whether the menu is currently shown.
 * @param touchPosition Pointer position within the cell; the menu is anchored near its horizontal
 * coordinate.
 * @param cellBounds The cell's bounds in the window, used to decide whether the menu opens below or
 * above the cell so the gap can be placed on the side facing the cell, and to measure the touch from
 * the start edge in RTL.
 * @param onDismiss Invoked when the menu is dismissed.
 * @param onArchiveThread When non-null, an "Archive" action is shown.
 * @param onRenameThread When non-null, a "Rename" action is shown.
 */
@Composable
private fun ChatThreadContextMenu(
    thread: Thread,
    expanded: Boolean,
    touchPosition: Offset,
    cellBounds: Rect,
    onDismiss: () -> Unit,
    onArchiveThread: ((Thread) -> Unit)?,
    onRenameThread: ((Thread) -> Unit)?,
) {
    val itemCount = (if (onRenameThread != null) 1 else 0) + if (onArchiveThread != null) 1 else 0
    val windowHeightPx = LocalWindowInfo.current.containerSize.height
    val layoutDirection = LocalLayoutDirection.current
    val offset = with(LocalDensity.current) {
        // DropdownMenu applies our y-offset with the same sign whether it opens below (top of menu
        // to bottom of anchor) or above (bottom of menu to top of anchor). A positive gap therefore
        // pushes the menu away from the cell when below, but *into* the cell when above. Estimate
        // the menu height and mirror the provider's fit test to pick the direction, then flip the
        // gap's sign so the gap always sits between the cell and the menu.
        val estimatedMenuHeight = space.contextMenuItemHeight.roundToPx() * itemCount +
            space.contextMenuVerticalPadding.roundToPx() * 2
        // Mirror DropdownMenuPositionProvider's fit test, which keeps the menu within a vertical
        // margin from the window edges.
        val fitsBelow = cellBounds.bottom + estimatedMenuHeight <= windowHeightPx - space.contextMenuWindowMargin.roundToPx()
        val gap = if (fitsBelow) space.small else -space.small
        // DropdownMenu measures the x-offset from the anchor's start edge, which is the right edge in
        // RTL, but the touch position is measured from the left edge.
        val touchFromStart = if (layoutDirection == LayoutDirection.Rtl) {
            cellBounds.width - touchPosition.x
        } else {
            touchPosition.x
        }
        DpOffset(x = touchFromStart.toDp(), y = gap)
    }
    DropdownMenu(
        expanded = expanded,
        containerColor = chatColors.token.background.surface.subtle,
        onDismissRequest = onDismiss,
        shape = chatShapes.menuActionsBoxShape,
        offset = offset,
        modifier = Modifier
            .width(space.contextMenuWidth)
            .testTag("chat_thread_context_menu_${thread.id}"),
    ) {
        onArchiveThread?.let { archive ->
            ArchiveThreadMenu {
                onDismiss()
                archive(thread)
            }
        }
        onRenameThread?.let { rename ->
            ChangeThreadNameMenu(text = string.rename_thread_menu_item) {
                onDismiss()
                rename(thread)
            }
        }
    }
}

/**
 * Reports the pointer position of each press-down within this element via [onPositionChanged].
 *
 * Runs on the [PointerEventPass.Initial] pass so the position is captured before a subsequent
 * `combinedClickable` consumes the gesture; it only observes and never consumes the event, so it
 * does not interfere with click/long-click handling or the ripple. Only the initial down is
 * reported (not subsequent move/scroll events), which is all that's needed to anchor the context
 * menu and avoids doing work on every pointer sample.
 */
private fun Modifier.trackPointerPosition(onPositionChanged: (Offset) -> Unit): Modifier =
    pointerInput(Unit) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                event.changes.firstOrNull { it.pressed && !it.previousPressed }
                    ?.let { onPositionChanged(it.position) }
            }
        }
    }

/**
 * True when the thread has an unread inbound message and can still accept messages.
 */
private val Thread.isUnread: Boolean
    get() {
        val firstMessage = messages.firstOrNull()
        return firstMessage?.metadata?.seenByCustomerAt == null &&
                firstMessage?.direction == ToClient &&
                chatThread.canAddMoreMessages
    }

/**
 * Builds the spoken content description for a thread list cell, covering the name, last message,
 * time, and unread status.
 */
@Composable
private fun threadAccessibilityDescription(thread: Thread, isUnread: Boolean): String {
    val resources = LocalResources.current
    val threadName = thread.name?.ifBlank { null }
        ?: when {
            thread.agent != null -> stringResource(string.agent_default_name)
            else -> stringResource(string.no_agent_assigned)
        }
    val timeText = thread.lastMessageTime?.let { resources.toShortTimeString(it) }.orEmpty()
    val lastMessage = thread.lastMessage
    return buildString {
        append(threadName)
        if (lastMessage.isNotBlank()) {
            append(stringResource(string.accessibility_thread_last_message, lastMessage))
        }
        if (timeText.isNotBlank()) {
            append(stringResource(string.accessibility_thread_time, timeText))
        }
        if (isUnread) {
            append(", ")
            append(stringResource(string.unread_status_suffix))
        }
    }
}

@Composable
private fun ChatThreadHeader(thread: Thread, isUnread: Boolean) {
    Row(verticalAlignment = CenterVertically) {
        if (isUnread) {
            UnreadIndicator()
            Spacer(modifier = Modifier.width(space.small))
        }
        Text(
            text = thread.name?.ifBlank { null }
                ?: when {
                    thread.agent != null -> stringResource(string.agent_default_name)
                    else -> stringResource(string.no_agent_assigned)
                },
            modifier = Modifier
                .semantics {
                    hideFromAccessibility()
                    testTag = "conversation_name"
                },
            color = ChatTheme.chatColors.token.content.primary,
            style = if (isUnread) chatTypography.threadListNameUnread else chatTypography.threadListName,
        )
        Spacer(modifier = Modifier.weight(1f))
        val resources = LocalResources.current
        val lastMessageTime = remember(thread, resources) {
            thread.lastMessageTime?.let { resources.toShortTimeString(it) }.orEmpty()
        }
        Text(
            text = lastMessageTime,
            modifier = Modifier
                .semantics {
                    hideFromAccessibility()
                    testTag = "last_message_time"
                },
            style = if (isUnread) chatTypography.threadListLastMessageTimeUnread else chatTypography.threadListLastMessageTime,
            color = colorScheme.tertiary
        )
        DetailsChevron()
    }
}

@Composable
private fun UnreadIndicator() {
    Box(
        modifier = Modifier
            .size(space.medium)
            .testTag("unread_indicator")
            .background(color = colorScheme.primary, shape = CircleShape)
    )
}

@Composable
private fun ChatThreadLastMessage(thread: Thread, isUnread: Boolean) {
    Text(
        text = thread.lastMessage,
        modifier = Modifier
            .padding(end = space.xxl)
            .semantics {
                hideFromAccessibility()
            },
        style = if (isUnread) chatTypography.threadListLastMessageUnread else chatTypography.threadListLastMessage,
        maxLines = 2,
        overflow = Ellipsis,
        color = colorScheme.tertiary,
    )
}

@Composable
@Preview
@NonRestartableComposable
private fun DetailsChevron(modifier: Modifier = Modifier) {
    Image(
        painter = rememberVectorPainter(image = Icons.Default.ChevronRight),
        contentDescription = null,
        modifier = modifier
            .padding(end = space.medium)
            .semantics {
                hideFromAccessibility()
            },
        colorFilter = ColorFilter.tint(colorScheme.primary)
    )
}

@Composable
@Preview
private fun ChatThreadViewPreview() {
    ChatTheme {
        ChatThreadView(thread = PreviewThread.nextThread(), onThreadSelected = { })
    }
}
