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

package com.nice.cxonechat.ui.composable.conversation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.LocalContentColor
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.Icons.Outlined
import androidx.compose.material.icons.filled.Deselect
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.outlined.Share
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.nice.cxonechat.message.Attachment
import com.nice.cxonechat.ui.R.string
import com.nice.cxonechat.ui.composable.generic.forwardingPainter
import com.nice.cxonechat.ui.composable.theme.BottomBar
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import com.nice.cxonechat.ui.composable.theme.ChatTheme.space
import com.nice.cxonechat.ui.composable.theme.TopBar
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Present a dialog to select and display or share from a list of [Attachment].
 *
 * @param attachments Available attachments.
 * @param title Title to display on the dialog.
 * @param onAttachmentTapped Direct action on an attachment, typically preview a single attachment.
 * @param onShare Share was selected for one or more attachments.  Selected attachments are passed
 * as the sole parameter.
 * @param onCancel The dialog was cancelled via back button or a tap outside the dialog.
 */
@Composable
fun SelectAttachmentsDialog(
    attachments: List<Attachment>,
    title: String,
    onAttachmentTapped: (Attachment) -> Unit,
    onShare: (Collection<Attachment>) -> Unit,
    onCancel: () -> Unit,
) {
    val viewModel = remember { ViewModel(attachments, onAttachmentTapped, onShare) }

    Dialog(
        onDismissRequest = onCancel,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
        )
    ) {
        Surface {
            Column {
                val selecting = viewModel.selecting.collectAsState().value

                TopBar(
                    title.ifBlank { stringResource(string.attachments_title) },
                    selecting,
                    viewModel::toggleSelecting
                )

                GridView(viewModel)

                if (selecting) {
                    BottomBar(
                        viewModel.selection.collectAsState().value,
                        onSelectAll = viewModel::selectAll,
                        onSelectNone = viewModel::selectNone,
                        onShare = viewModel.onShare
                    )
                }
            }
        }
    }
}

@Composable
private fun GridView(
    viewModel: ViewModel,
) {
    LazyVerticalGrid(
        columns = GridCells.FixedSize(space.largeAttachmentSize),
        contentPadding = PaddingValues(space.medium)
    ) {
        items(
            viewModel.attachments.toList(),
            key = { it.url }
        ) { attachment ->
            AttachmentIcon(
                attachment = attachment,
                modifier = Modifier
                    .size(space.largeAttachmentSize)
                    .padding(space.largeAttachmentPadding),
                selected = viewModel.isSelected(attachment),
                onClick = viewModel::onClick,
                onLongClick = viewModel::onLongClick,
            )
        }
    }
}

@Composable
private fun TopBar(
    title: String,
    selecting: Boolean,
    toggleSelecting: () -> Unit
) {
    val select = rememberVectorPainter(
        if (selecting) Icons.Default.Deselect else Icons.Default.SelectAll
    )

    ChatTheme.TopBar(
        title = title
    ) {
        IconButton(onClick = toggleSelecting) {
            Icon(
                painter = forwardingPainter(
                    select,
                    colorFilter = ColorFilter.tint(LocalContentColor.current)
                ),
                contentDescription = null
            )
        }
    }
}

@Composable
private fun BottomBar(
    selection: Collection<Attachment>,
    onSelectAll: () -> Unit,
    onSelectNone: () -> Unit,
    onShare: (Collection<Attachment>) -> Unit,
) {
    ChatTheme.BottomBar {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row {
                TextButton(onClick = onSelectAll) {
                    Text(stringResource(string.select_all), color = ChatTheme.colors.onPrimary)
                }

                TextButton(onClick = onSelectNone) {
                    Text(stringResource(string.select_none), color = ChatTheme.colors.onPrimary)
                }
            }

            if (selection.isEmpty()) {
                Text(stringResource(string.select_items))
            } else {
                Text("${selection.count()} items selected")
                IconButton({ onShare(selection) }) {
                    Icon(
                        painter = forwardingPainter(
                            rememberVectorPainter(image = Outlined.Share),
                            colorFilter = ColorFilter.tint(LocalContentColor.current)
                        ),
                        contentDescription = stringResource(string.share_content_description),
                    )
                }
            }
        }
    }
}

private class ViewModel(
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

    fun isSelected(attachment: Attachment) = selection.value.contains(attachment)

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

@Preview
@Composable
private fun TopBarPreview() {
    ChatTheme {
        TopBar(title = "title", selecting = false, toggleSelecting = {})
    }
}

@Preview
@Composable
private fun BottomBarPreviewNone() {
    ChatTheme {
        BottomBar(selection = setOf(), onSelectAll = {}, onSelectNone = {}, onShare = {})
    }
}

@Preview
@Composable
private fun BottomBarPreviewMore() {
    ChatTheme {
        BottomBar(
            selection = AttachmentProvider().values.take(4).toSet(),
            onSelectAll = {},
            onSelectNone = {},
            onShare = {}
        )
    }
}

@Preview
@Composable
private fun GridViewPreview() {
    val attachments = remember {
        AttachmentProvider().values.take(5).distinctBy(Attachment::url).toList()
    }

    ChatTheme {
        GridView(
            ViewModel(
                attachments = attachments,
                onAttachmentTapped = {},
                onShare = {},
            ).apply {
                toggleSelected(attachments.take(1).first())
            }
        )
    }
}

@Preview
@Composable
private fun SelectAttachmentsPreview() {
    val attachments = remember {
        AttachmentProvider().values.take(5).distinctBy(Attachment::url).toList()
    }

    ChatTheme {
        SelectAttachmentsDialog(
            attachments = attachments,
            title = "Title",
            onAttachmentTapped = {},
            onShare = {},
            onCancel = {},
        )
    }
}
