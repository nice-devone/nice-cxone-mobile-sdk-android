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

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.nice.cxonechat.message.Attachment
import com.nice.cxonechat.ui.R
import com.nice.cxonechat.ui.composable.conversation.AttachmentProvider
import com.nice.cxonechat.ui.composable.conversation.ShareIcon
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import com.nice.cxonechat.ui.composable.theme.ChatTheme.chatTypography
import com.nice.cxonechat.ui.composable.theme.ChatTheme.colorScheme

@Composable
internal fun SelectAttachmentsBottomBar(
    selecting: Boolean,
    selection: Collection<Attachment>,
    onSelectAll: () -> Unit,
    onSelectNone: () -> Unit,
    onShare: (Collection<Attachment>) -> Unit,
    onShareAll: () -> Unit,
) {
    val selectionNotEmpty = selection.isNotEmpty()
    val onClick = { onShare(selection) }
    Column(
        modifier = Modifier.testTag("select_attachments_bottom_bar")
    ) {
        HorizontalDivider()
        Crossfade(selecting) { isSelecting ->
            if (isSelecting) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row {
                        TextButton(
                            onClick = onSelectAll,
                            modifier = Modifier.testTag("select_all_button")
                        ) {
                            Text(
                                text = stringResource(id = R.string.select_all),
                                style = chatTypography.selectAttachmentBottomBarText
                            )
                        }
                        TextButton(
                            onClick = onSelectNone,
                            enabled = selectionNotEmpty,
                            modifier = Modifier.testTag("select_none_button")
                        ) {
                            Text(
                                text = stringResource(id = R.string.select_none),
                                style = chatTypography.selectAttachmentBottomBarText
                            )
                        }
                    }

                    if (selectionNotEmpty) {
                        Text(
                            text = stringResource(R.string.items_selected, selection.count()),
                            style = chatTypography.selectAttachmentBottomBarText
                        )
                    } else {
                        Text(
                            text = stringResource(id = R.string.select_items),
                            style = chatTypography.selectAttachmentBottomBarText
                        )
                    }
                    ShareButton(
                        contentDescription = stringResource(R.string.share_attachment_selected),
                        enabled = selectionNotEmpty,
                        onClick = onClick,
                        modifier = Modifier.testTag("share_selected_button"),
                    )
                }
            } else {
                NotSelecting(onShareAll)
            }
        }
    }
}

@Composable
private fun NotSelecting(onShareAll: () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        ShareButton(
            contentDescription = stringResource(R.string.content_description_share_all),
            enabled = true,
            onClick = onShareAll,
            modifier = Modifier.testTag("share_all_button"),
        )
    }
}

@Composable
private fun ShareButton(
    enabled: Boolean,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    onClick: () -> Unit,
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        colors = IconButtonDefaults.iconButtonColors(contentColor = colorScheme.primary),
    ) {
        ShareIcon(contentDescription)
    }
}

@Preview
@Composable
private fun BottomBarNotSelecting() {
    val selection = remember { setOf<Attachment>() }
    ChatTheme {
        Surface {
            SelectAttachmentsBottomBar(selecting = false, selection = selection, {}, {}, { _ -> }, {})
        }
    }
}

@PreviewLightDark
@Composable
private fun BottomBarPreviewNone() {
    ChatTheme {
        Surface {
            SelectAttachmentsBottomBar(
                selection = setOf(),
                onSelectAll = {},
                onSelectNone = {},
                onShare = {},
                selecting = true,
                onShareAll = {}
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun BottomBarPreviewMore() {
    ChatTheme {
        Surface {
            SelectAttachmentsBottomBar(
                selection = AttachmentProvider().values.take(4).toSet(),
                onSelectAll = {},
                onSelectNone = {},
                onShare = {},
                selecting = true,
                onShareAll = {},
            )
        }
    }
}
