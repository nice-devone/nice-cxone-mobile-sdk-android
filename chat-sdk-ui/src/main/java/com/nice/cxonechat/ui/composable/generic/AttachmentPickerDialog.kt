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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize.Max
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.nice.cxonechat.ui.R
import com.nice.cxonechat.ui.R.string
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import com.nice.cxonechat.ui.data.AllowedFileType
import com.nice.cxonechat.ui.data.AllowedFileTypeSource
import com.nice.cxonechat.ui.domain.GetAllowedAttachmentGroups
import org.koin.compose.getKoin

@Composable
internal fun AttachmentPickerDialog(
    onDismissed: () -> Unit,
    getContent: (mimeType: Collection<String>) -> Unit,
    allowedFileTypeSource: AllowedFileTypeSource = getKoin().get(),
) {
    val (selectedOption, onOptionSelected) = remember { mutableStateOf<Collection<String>?>(null) }
    val allowedFileTypes = allowedFileTypeSource.allowedMimeTypes
    AlertDialog(
        onDismissRequest = onDismissed,
        dismissButton = {
            Button(onClick = {
                onDismissed()
            }) {
                Text(text = stringResource(string.cancel))
            }
        },
        confirmButton = {
            Button(onClick = {
                selectedOption?.let(getContent)
                onDismissed()
            }) {
                Text(text = stringResource(string.ok))
            }
        },
        title = {
            Text(text = stringResource(string.title_attachment_picker))
        },
        text = {
            AttachmentPickerDialogContent(allowedFileTypes, selectedOption, onOptionSelected)
        }
    )
}

@Composable
private fun AttachmentPickerDialogContent(
    allowedFileTypes: List<AllowedFileType>,
    selectedOption: Collection<String>?,
    onOptionSelected: (Collection<String>?) -> Unit,
) {
    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .height(Max)
    ) {
        val labels = stringArrayResource(R.array.attachment_type_labels)
        val options = stringArrayResource(R.array.attachment_type_mimetypes)
        val allowedAttachmentGroups = remember {
            GetAllowedAttachmentGroups.allowedAttachmentGroups(
                labels.asIterable(),
                options.asIterable(),
                allowedFileTypes,
            )
        }
        allowedAttachmentGroups.forEach { (label, option) ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = selectedOption != null && option.containsAll(selectedOption),
                        onClick = { onOptionSelected(option) }
                    )
                    .padding(horizontal = ChatTheme.space.large),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = selectedOption != null && option.containsAll(selectedOption),
                    onClick = { onOptionSelected(option) }
                )
                Text(
                    text = label,
                    style = ChatTheme.typography.bodyLarge.merge(),
                    modifier = Modifier.padding(start = ChatTheme.space.large)
                )
            }
        }
    }
}

@Preview
@Composable
private fun DialogPreview() {
    ChatTheme {
        Column {
            val (selection, onSelected) = remember { mutableStateOf<Collection<String>?>(null) }
            val labels = stringArrayResource(R.array.attachment_type_labels)
            val options = stringArrayResource(R.array.attachment_type_mimetypes)
            if (selection == null) {
                AttachmentPickerDialog(
                    onDismissed = {},
                    getContent = { onSelected(it) },
                    allowedFileTypeSource = object : AllowedFileTypeSource {
                        override val allowedMimeTypes: List<AllowedFileType> = labels
                            .zip(options)
                            .map(::AllowedFileType)
                    }
                )
            }
            Text(text = selection?.joinToString() ?: "Nothing")
            Button(onClick = { onSelected(null) }) {
                Text(text = "Reset dialog")
            }
        }
    }
}
