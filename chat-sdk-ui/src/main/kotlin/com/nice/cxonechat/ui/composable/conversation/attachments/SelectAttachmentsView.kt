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

package com.nice.cxonechat.ui.composable.conversation.attachments

import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.dismiss
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.nice.cxonechat.message.Attachment
import com.nice.cxonechat.ui.R
import com.nice.cxonechat.ui.composable.conversation.AttachmentProvider
import com.nice.cxonechat.ui.composable.conversation.PreviewAttachments
import com.nice.cxonechat.ui.composable.generic.scrollbar.LocalScrollbarConfig
import com.nice.cxonechat.ui.composable.generic.scrollbar.rememberScrollbarState
import com.nice.cxonechat.ui.composable.generic.scrollbar.reservedThickness
import com.nice.cxonechat.ui.composable.generic.scrollbar.scrollbar
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import com.nice.cxonechat.ui.composable.theme.ChatTheme.chatColors
import com.nice.cxonechat.ui.composable.theme.ChatTheme.chatTypography
import com.nice.cxonechat.ui.composable.theme.ChatTheme.space
import com.nice.cxonechat.ui.composable.theme.SheetAnchors
import com.nice.cxonechat.ui.composable.theme.WideScreenModalBottomSheet
import com.nice.cxonechat.ui.composable.theme.isLandscapeWindow
import com.nice.cxonechat.ui.composable.theme.wideScreenHorizontalPadding
import com.nice.cxonechat.ui.util.hideFromAccessibility
import com.nice.cxonechat.ui.util.typeAndName
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Columns the grid uses in a window that is taller than it is wide.
 *
 * Held apart from the spacing tokens because a column count is a structural choice, not a measurement.
 */
private const val GRID_COLUMNS_TALL = 2

/** Columns the grid uses in a window that is wider than it is tall. */
private const val GRID_COLUMNS_WIDE = 4

/**
 * Present a view to select and display or share from a list of [Attachment].
 * Current implementation utilizes the SDK's shared wide-screen modal bottom sheet to present the
 * view, with enabled sheet gestures and disabled partial expansion.
 *
 * @param attachments Available attachments.
 * @param onAttachmentTapped Direct action on an attachment, typically preview a single attachment.
 * @param onShare Share was selected for one or more attachments.  Selected attachments are passed
 * as the sole parameter.
 * @param onCancel The selection user intent was cancelled via back button or a tap outside the view.
 * @param modifier Modifier for the underlying bottom sheet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectAttachmentsView(
    attachments: List<Attachment>,
    onAttachmentTapped: (Attachment) -> Unit,
    onShare: (Collection<Attachment>) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel = remember { SelectionViewModel(attachments, onAttachmentTapped, onShare) }
    ChatTheme.WideScreenModalBottomSheet(
        onDismissRequest = onCancel,
        sheetState = rememberBottomSheetState(SheetValue.Hidden, SheetAnchors.FullHeightOnly),
        // The band behind the drag handle, title and action bar; the grid paints its own.
        containerColor = chatColors.token.background.default,
        dragHandle = { BottomSheetDefaults.DragHandle(Modifier.minimumInteractiveComponentSize()) },
        modifier = modifier
            .testTag("select_attachments_view")
            .systemBarsPadding()
            .semantics {
                dismiss {
                    onCancel()
                    true
                }
            },
    ) {
        AttachmentsView(viewModel)
    }
}

/**
 * The sheet's content, separated from [SelectAttachmentsView] so a screenshot test can compose it inside
 * [WideScreenModalBottomSheet] with a [SelectionViewModel] of its own: the view remembers one internally,
 * which leaves selecting mode unreachable from a test.
 */
@VisibleForTesting
@Composable
internal fun ColumnScope.AttachmentsView(viewModel: SelectionViewModel) {
    val selection by viewModel.selection.collectAsState()
    val selecting = viewModel.selecting.collectAsState().value
    SelectAttachmentsTopBar(selecting, viewModel::toggleSelecting)
    HorizontalDivider(color = chatColors.token.border.default)
    Surface(
        color = chatColors.token.background.surface.subtle,
        modifier = Modifier.weight(1f),
    ) {
        // The wide inset replaces the compact one rather than adding to it.
        val horizontalInset = maxOf(space.xl, wideScreenHorizontalPadding())
        Column(
            modifier = Modifier.padding(start = horizontalInset, end = horizontalInset, top = space.xl),
        ) {
            GridView(viewModel)
        }
    }
    SelectAttachmentsBottomBar(
        selecting = selecting,
        selection = selection,
        onSelectAll = remember { viewModel::selectAll },
        onSelectNone = remember { viewModel::selectNone },
        onShare = remember { viewModel.onShare },
        onShareAll = remember { { viewModel.onShare(viewModel.attachments) } },
    )
}

/**
 * The grid of selectable attachments, with a scrollbar in a strip reserved along its trailing edge.
 *
 * The column count follows the window's own proportions rather than its width alone: a window taller than
 * it is wide keeps two columns however wide it is, and a window wider than it is tall takes four. Read from
 * the window, so it follows a resize or a split-screen change rather than the device's orientation.
 *
 * Order in the modifier chain is load-bearing, for the reason spelled out on
 * `Modifier.scrollbarBelowRow`: `scrollbar` reserves no space of its own, so the padding has to grow the
 * node from inside it, leaving the bar drawn in the strip the cells were inset out of.
 *
 * `scrollbar` publishes its own test tag on this node and the outermost tag wins, so a tag added to the
 * grid after it is discarded; tag a wrapper instead.
 */
@Composable
private fun ColumnScope.GridView(
    viewModel: SelectionViewModel,
) {
    val items = viewModel.attachments.toList()
    val gridState = rememberLazyGridState()
    val scrollbarState = rememberScrollbarState()
    val landscapeWindow = isLandscapeWindow()
    LazyVerticalGrid(
        columns = GridCells.Fixed(if (landscapeWindow) GRID_COLUMNS_WIDE else GRID_COLUMNS_TALL),
        state = gridState,
        horizontalArrangement = Arrangement.spacedBy(if (landscapeWindow) space.semiLarge else space.xl),
        verticalArrangement = Arrangement.spacedBy(space.xl, Alignment.Top),
        modifier = Modifier
            .weight(1f)
            .scrollbar(gridState, scrollbarState, Orientation.Vertical)
            .padding(end = LocalScrollbarConfig.current.reservedThickness),
    ) {
        val itemsTotal = items.size
        itemsIndexed(
            key = { _: Int, attachment -> attachment.url },
            contentType = { _: Int, attachment -> attachment.mimeType },
            items = items,
        ) { i, attachment ->
            val isSelected = viewModel.selection.collectAsState().value.contains(attachment)
            val selecting = viewModel.selecting.collectAsState().value
            AttachmentGridItem(
                attachment = attachment,
                index = i,
                itemsTotal = itemsTotal,
                isSelected = isSelected,
                selecting = selecting,
                onClick = remember { viewModel::onClick },
                onLongClick = remember { viewModel::onLongClick },
            )
        }
    }
}

@Composable
private fun AttachmentGridItem(
    attachment: Attachment,
    index: Int,
    itemsTotal: Int,
    isSelected: Boolean,
    selecting: Boolean,
    onClick: (Attachment) -> Unit,
    onLongClick: (Attachment) -> Unit,
) {
    Column(
        modifier = Modifier.semantics {
            isTraversalGroup = true
        },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(space.medium),
    ) {
        val description = stringResource(
            R.string.content_description_attachment_item,
            attachment.typeAndName,
            index + 1,
            itemsTotal
        )
        AttachmentFramedPreview(
            attachment = attachment,
            modifier = Modifier
                .focusable(true)
                .minimumInteractiveComponentSize()
                .height(220.dp)
                .testTag("attachment_preview_$index"),
            selected = isSelected,
            selectionFrame = true,
            selectionFrameColor = chatColors.token.border.default,
            selectionCircle = selecting,
            contentDescriptionText = description,
            isGroupAttachment = true, // Selection screen has same corners as group items
            onClick = remember { onClick },
            onClickLabel = stringResource(R.string.content_description_attachment_detail),
            onLongClick = remember { onLongClick },
            onLongClickLabel = stringResource(R.string.content_description_attachments_sharing),
        )
        Text(
            text = attachment.friendlyName,
            maxLines = 1,
            overflow = TextOverflow.MiddleEllipsis,
            style = chatTypography.previewTitle,
            modifier = Modifier
                .padding(horizontal = space.framePreviewWidth)
                .hideFromAccessibility {
                    testTag = "attachment_name_$index"
                },
        )
    }
}

internal class SelectionViewModel(
    val attachments: Collection<Attachment>,
    private val onAttachmentTapped: (Attachment) -> Unit,
    val onShare: (Collection<Attachment>) -> Unit,
) {
    private val _selection = MutableStateFlow(setOf<Attachment>())
    val selection = _selection.asStateFlow()

    private val _selecting = MutableStateFlow(false)
    val selecting = _selecting.asStateFlow()

    fun toggleSelecting(attachment: Attachment? = null) {
        _selecting.value = !_selecting.value

        if (!selecting.value) {
            _selection.value = setOf()
        } else {
            _selection.value = setOfNotNull(attachment)
        }
    }

    fun toggleSelected(attachment: Attachment) {
        _selection.value = if (_selection.value.contains(attachment)) {
            _selection.value - attachment
        } else {
            _selection.value + attachment
        }
    }

    fun onClick(attachment: Attachment) {
        if (selecting.value) {
            toggleSelected(attachment)
        } else {
            onAttachmentTapped(attachment)
        }
    }

    fun onLongClick(attachment: Attachment) {
        toggleSelecting(attachment)
    }

    fun selectAll() {
        _selection.value = attachments.toSet()
    }

    fun selectNone() {
        _selection.value = setOf()
    }
}

@PreviewLightDark
@Composable
private fun GridViewPreview() {
    val attachments = remember {
        AttachmentProvider().values.take(5).distinctBy(Attachment::url).toList()
    }
    ChatTheme {
        Surface {
            Column {
                GridView(
                    SelectionViewModel(
                        attachments = attachments,
                        onAttachmentTapped = {},
                        onShare = {},
                    ).apply {
                        toggleSelected(attachments.take(1).first())
                    }
                )
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun SelectAttachmentsPreview() {
    val attachments = remember {
        PreviewAttachments.with(10).toList()
    }
    val onAttachmentTapped: (Attachment) -> Unit = {}
    val onShare: (Collection<Attachment>) -> Unit = {}
    ChatTheme {
        val viewModel = remember { SelectionViewModel(attachments, onAttachmentTapped, onShare) }
        Column {
            AttachmentsView(viewModel)
        }
    }
}

@PreviewLightDark
@Composable
private fun SelectAttachmentsViewPreview() {
    val attachments = remember { PreviewAttachments.with(6).toList() }
    ChatTheme {
        Surface(modifier = Modifier) {
            SelectAttachmentsView(
                attachments = attachments,
                onAttachmentTapped = {},
                onShare = {},
                onCancel = {},
            )
        }
    }
}
