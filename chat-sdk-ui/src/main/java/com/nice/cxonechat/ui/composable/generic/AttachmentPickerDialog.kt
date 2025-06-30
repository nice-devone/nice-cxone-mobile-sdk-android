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

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize.Max
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.LocalTonalElevationEnabled
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import com.nice.cxonechat.ui.AttachmentType
import com.nice.cxonechat.ui.R.string
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import com.nice.cxonechat.ui.composable.theme.ChatTheme.colorScheme
import com.nice.cxonechat.ui.composable.theme.ChatTheme.space
import com.nice.cxonechat.ui.composable.theme.ChatTheme.typography
import com.nice.cxonechat.ui.data.source.AllowedFileType
import com.nice.cxonechat.ui.data.source.AllowedFileTypeSource
import com.nice.cxonechat.ui.domain.AttachmentOption.CameraPhoto
import com.nice.cxonechat.ui.domain.AttachmentOption.CameraVideo
import com.nice.cxonechat.ui.domain.AttachmentOption.File
import com.nice.cxonechat.ui.domain.AttachmentOption.ImageAndVideo
import com.nice.cxonechat.ui.domain.GetAllowedAttachmentGroups
import org.koin.compose.getKoin
import java.util.EnumMap

/**
 * Displays a dialog for selecting an attachment type.
 *
 * @param onDismissed Callback invoked when the dialog is dismissed.
 * @param getContent Callback invoked with the selected attachment type.
 * @param allowedFileTypeSource Source of allowed file types for attachments.
 */
@Composable
internal fun AttachmentPickerDialog(
    onDismissed: () -> Unit,
    getContent: (attachmentType: AttachmentType) -> Unit,
    allowedFileTypeSource: AllowedFileTypeSource = getKoin().get(),
) {
    val (selectedOption, onOptionSelected) = remember { mutableStateOf<AttachmentType?>(null) }
    val allowedFileTypes = allowedFileTypeSource.allowedMimeTypes
    CompositionLocalProvider(
        LocalTonalElevationEnabled provides false
    ) {
        ChatAlertDialog(
            onDismissRequest = onDismissed,
            containerColor = colorScheme.surface,
            dismissButton = {
                TextButton(onClick = {
                    onDismissed()
                }) {
                    Text(text = stringResource(string.cancel))
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        selectedOption?.let(getContent)
                        onDismissed()
                    },
                    enabled = selectedOption != null
                ) {
                    Text(text = stringResource(string.ok))
                }
            },
            title = {
                Text(text = stringResource(string.title_attachment_picker), style = typography.headlineSmall)
            },
            text = {
                AttachmentPickerDialogContent(allowedFileTypes, selectedOption, onOptionSelected)
            }
        )
    }
}

/**
 * Content of the attachment picker dialog.
 *
 * @param allowedFileTypes List of allowed file types for attachments.
 * @param selectedOption Currently selected attachment type.
 * @param onOptionSelected Callback invoked when an attachment type is selected.
 */
@Composable
private fun AttachmentPickerDialogContent(
    allowedFileTypes: List<AllowedFileType>,
    selectedOption: AttachmentType?,
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
        Column(
            Modifier
                .selectableGroup()
                .padding(space.medium)
        ) {
            allowedAttachmentGroups.forEach { (label, option) ->
                val isSelected = remember(selectedOption, option) {
                    selectedOption != null && selectedOption == option
                }
                PickerItem(
                    label = label,
                    isSelected = isSelected,
                ) {
                    onOptionSelected(option)
                }
            }
        }
    }
}

/**
 * A single item in the attachment picker.
 *
 * @param label The label for the picker item.
 * @param isSelected Whether the item is currently selected.
 * @param onOptionSelected Callback invoked when the item is selected.
 */
@Composable
private fun ColumnScope.PickerItem(
    label: String,
    isSelected: Boolean,
    onOptionSelected: () -> Unit,
) {
    Row(
        Modifier
            .align(Alignment.Start)
            .fillMaxWidth()
            .selectable(
                selected = isSelected,
                onClick = { onOptionSelected() },
                role = Role.RadioButton
            )
            .padding(
                top = space.medium,
                end = space.medium,
                bottom = space.medium
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            modifier = Modifier
                .padding(space.medium)
                .alpha(if (isSystemInDarkTheme()) 0.5f else 1f),
            selected = isSelected,
            onClick = null
        )
        Text(
            text = label,
            style = typography.bodyLarge.merge(),
            modifier = Modifier.padding(horizontal = space.medium)
        )
    }
}

@Preview
@Composable
private fun PickerItemPreview() {
    var isSelected by remember { mutableStateOf(false) }
    ChatTheme {
        Column(Modifier.selectableGroup()) {
            PickerItem("Image", isSelected = isSelected) { isSelected = !isSelected }
        }
    }
}

@Preview
@Composable
private fun DialogPreview() {
    ChatTheme {
        Column {
            val (selection, onSelected) = remember { mutableStateOf<AttachmentType?>(null) }
            if (selection == null) {
                AttachmentPickerDialog(
                    onDismissed = {},
                    getContent = { onSelected(it) },
                    allowedFileTypeSource = object : AllowedFileTypeSource {
                        override val allowedMimeTypes: List<AllowedFileType> = listOf(
                            AllowedFileType(
                                mimeType = "image/*",
                                description = stringResource(string.attachment_type_camera_image)
                            )
                        )
                    }
                )
            }
            Text(text = selection?.toString() ?: "Nothing")
            Button(onClick = { onSelected(null) }) {
                Text(text = "Reset dialog")
            }
        }
    }
}
