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

package com.nice.cxonechat.ui.composable.generic

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize.Max
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowRight
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.nice.cxonechat.ui.AttachmentType
import com.nice.cxonechat.ui.R.string
import com.nice.cxonechat.ui.composable.icons.ChatIcons
import com.nice.cxonechat.ui.composable.icons.outlined.Camera
import com.nice.cxonechat.ui.composable.icons.outlined.Folder
import com.nice.cxonechat.ui.composable.icons.outlined.Image
import com.nice.cxonechat.ui.composable.icons.outlined.VideoAdd
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import com.nice.cxonechat.ui.composable.theme.ChatTheme.chatColors
import com.nice.cxonechat.ui.composable.theme.ChatTheme.colorScheme
import com.nice.cxonechat.ui.composable.theme.ChatTheme.space
import com.nice.cxonechat.ui.data.source.AllowedFileType
import com.nice.cxonechat.ui.data.source.AllowedFileTypeSource
import com.nice.cxonechat.ui.domain.AttachmentOption.CameraPhoto
import com.nice.cxonechat.ui.domain.AttachmentOption.CameraVideo
import com.nice.cxonechat.ui.domain.AttachmentOption.File
import com.nice.cxonechat.ui.domain.AttachmentOption.ImageAndVideo
import com.nice.cxonechat.ui.domain.GetAllowedAttachmentGroups
import com.nice.cxonechat.ui.domain.MimeTypeGroup
import org.koin.compose.getKoin
import java.util.EnumMap

/**
 * Displays a view for selecting an attachment type.
 *
 * @param onDismiss Callback invoked when the view is dismissed.
 * @param getContent Callback invoked with the selected attachment type.
 * @param allowedFileTypeSource Source of allowed file types for attachments.
 */
@Composable
internal fun AttachmentPickerDialog(
    onDismiss: () -> Unit,
    getContent: (attachmentType: AttachmentType) -> Unit,
    allowedFileTypeSource: AllowedFileTypeSource = getKoin().get(),
) {
    val allowedFileTypes = allowedFileTypeSource.allowedMimeTypes
    AttachmentPickerBottomSheet(
        onDismiss = onDismiss,
        content = {
            BottomSheetTitle(stringResource(string.title_attachment_picker))
            AttachmentPickerDialogContent(
                allowedFileTypes = allowedFileTypes,
                onOptionSelected = {
                    getContent(it)
                    onDismiss()
                }
            )
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AttachmentPickerBottomSheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (ColumnScope.() -> Unit),
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(),
        dragHandle = { BottomSheetDefaults.DragHandle(color = chatColors.token.content.tertiary) },
        containerColor = chatColors.token.background.surface.subtle,
        contentColor = chatColors.token.content.primary,
        modifier = modifier
            .testTag("attachment_picker_bottom_sheet")
            .systemBarsPadding(),
        content = content,
    )
}

/**
 * Content of the attachment picker dialog.
 *
 * @param allowedFileTypes List of allowed file types for attachments.
 * @param onOptionSelected Callback invoked when an attachment type is selected.
 */
@Composable
private fun AttachmentPickerDialogContent(
    allowedFileTypes: List<AllowedFileType>,
    onOptionSelected: (attachmentType: AttachmentType) -> Unit,
) {
    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .height(Max)
    ) {
        val attachmentOptions = EnumMap(
            mapOf(
                CameraPhoto to stringResource(string.attachment_type_camera_image),
                CameraVideo to stringResource(string.attachment_type_camera_video),
                ImageAndVideo to stringResource(string.attachment_type_media),
                File to stringResource(string.attachment_type_file),
            )
        )
        val allowedAttachmentGroups = remember {
            GetAllowedAttachmentGroups.allowedAttachmentGroups(
                attachmentOptions,
                allowedFileTypes,
            )
        }
        val icons = remember { allowedAttachmentGroups.prepareIcons() }
        allowedAttachmentGroups.forEachIndexed { i, (label, option) ->
            if (i > 0) {
                HorizontalDivider(color = chatColors.token.border.default)
            }
            BottomSheetActionRow(
                text = label,
                onClick = { onOptionSelected(option) },
                testTag = "option_$i",
                leadingContent = { LeadingIcon(remember { icons[option] }, label) },
                trailingContent = { TrailingArrow() }
            )
        }
    }
}

@Composable
private fun LeadingIcon(icon: ImageVector?, label: String) {
    if (icon != null) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size(space.bottomSheetActionItemSize)
        )
    }
}

@Composable
private fun TrailingArrow() {
    Icon(
        imageVector = Icons.AutoMirrored.Default.ArrowRight,
        contentDescription = stringResource(string.select),
        tint = chatColors.token.content.primary,
        modifier = Modifier.size(space.xl)
    )
}

private fun List<MimeTypeGroup>.prepareIcons() = associate {
    when (val option = it.options) {
        AttachmentType.CameraPhoto -> option to ChatIcons.Camera
        AttachmentType.CameraVideo -> option to ChatIcons.VideoAdd
        is AttachmentType.File -> option to ChatIcons.Folder
        else -> option to ChatIcons.Image
    }
}

@PreviewLightDark
@Composable
private fun DialogPreview() {
    ChatTheme {
        Surface(modifier = Modifier.systemBarsPadding(), color = colorScheme.background) {
            AttachmentPickerDialog(
                onDismiss = {},
                getContent = {},
                allowedFileTypeSource = object : AllowedFileTypeSource {
                    override val allowedMimeTypes: List<AllowedFileType> = listOf(
                        AllowedFileType(
                            mimeType = "image/*",
                            description = stringResource(string.attachment_type_camera_image)
                        ),
                        AllowedFileType(
                            mimeType = "video/*",
                            description = stringResource(string.attachment_type_camera_video)
                        )
                    )
                }
            )
        }
    }
}
