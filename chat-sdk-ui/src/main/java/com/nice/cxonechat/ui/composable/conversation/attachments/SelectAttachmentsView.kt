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

package com.nice.cxonechat.ui.composable.conversation.attachments

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.nice.cxonechat.message.Attachment
import com.nice.cxonechat.ui.composable.conversation.AttachmentProvider
import com.nice.cxonechat.ui.composable.conversation.PreviewAttachments
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import com.nice.cxonechat.ui.composable.theme.ChatTheme.chatColors
import com.nice.cxonechat.ui.composable.theme.ChatTheme.chatTypography
import com.nice.cxonechat.ui.composable.theme.ChatTheme.space
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Present a view to select and display or share from a list of [Attachment].
 * Current implementation utilizes [ModalBottomSheet] to present the view,
 * with enabled sheet gestures and disabled partial expansion.
 *
 * @param attachments Available attachments.
 * @param onAttachmentTapped Direct action on an attachment, typically preview a single attachment.
 * @param onShare Share was selected for one or more attachments.  Selected attachments are passed
 * as the sole parameter.
 * @param onCancel The selection user intent was cancelled via back button or a tap outside the view.
 * @param modifier Modifier for the [ModalBottomSheet].
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
    ModalBottomSheet(
        onDismissRequest = onCancel,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = chatColors.token.background.surface.subtle,
        contentColor = chatColors.token.content.primary,
        modifier = modifier
            .testTag("select_attachments_view")
            .systemBarsPadding(),
    ) {
        AttachmentsView(viewModel)
    }
}

@Composable
private fun ColumnScope.AttachmentsView(viewModel: SelectionViewModel) {
    val selection by viewModel.selection.collectAsState()
    val selecting = viewModel.selecting.collectAsState().value
    Column(
        modifier = Modifier
            .padding(horizontal = space.xl)
            .weight(1f),
    ) {
        SelectAttachmentsTopBar(selecting, viewModel::toggleSelecting)
        GridView(viewModel)
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

@Composable
private fun ColumnScope.GridView(
    viewModel: SelectionViewModel,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        horizontalArrangement = Arrangement.spacedBy(23.dp),
        verticalArrangement = Arrangement.spacedBy(space.xl, Alignment.Top),
        modifier = Modifier
            .weight(1f)
            .testTag("attachments_grid_preview"),
    ) {
        itemsIndexed(
            key = { _: Int, attachment -> attachment.url },
            contentType = { _: Int, attachment -> attachment.mimeType },
            items = viewModel.attachments.toList(),
        ) { i, attachment ->
            val isSelected = viewModel.selection.collectAsState().value.contains(attachment)
            val selecting = viewModel.selecting.collectAsState().value
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(space.medium),
            ) {
                AttachmentFramedPreview(
                    attachment = attachment,
                    modifier = Modifier
                        .height(220.dp)
                        .testTag("attachment_preview_$i"),
                    selected = isSelected,
                    selectionFrame = true,
                    selectionFrameColor = chatColors.token.border.default,
                    selectionCircle = selecting,
                    onClick = remember { viewModel::onClick },
                    onLongClick = remember { viewModel::onLongClick },
                )
                Text(
                    text = attachment.friendlyName,
                    maxLines = 1,
                    overflow = TextOverflow.MiddleEllipsis,
                    style = chatTypography.previewTitle,
                    modifier = Modifier.padding(horizontal = space.framePreviewWidth),
                )
            }
        }
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
