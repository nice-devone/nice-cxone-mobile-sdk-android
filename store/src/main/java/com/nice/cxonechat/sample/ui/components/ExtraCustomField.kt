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

package com.nice.cxonechat.sample.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.nice.cxonechat.sample.R.string
import com.nice.cxonechat.sample.ui.theme.AppTheme
import com.nice.cxonechat.sample.ui.theme.AppTheme.space

/**
 * A composable function that renders a custom field as an [OutlinedTextField] with a remove trailing icon button.
 *
 * @param customFieldEntry A map entry containing the key (label) and value (content) of the custom field.
 * @param modifier A [Modifier] to be applied to the root composable. Defaults to [Modifier].
 * @param onValueSet A callback invoked when the value of the custom field changes.
 *                   Takes the key and the new value as parameters.
 * @param onRemove A callback invoked when the remove button is clicked.
 *                 Takes the key of the custom field to be removed as a parameter.
 */
@Composable
internal fun ExtraCustomField(
    customFieldEntry: Map.Entry<String, String>,
    modifier: Modifier = Modifier,
    onValueSet: (String, String) -> Unit,
    onRemove: (String) -> Unit,
) {
    val key = customFieldEntry.key
    OutlinedTextField(
        value = customFieldEntry.value,
        label = { Text(key) },
        onValueChange = { onValueSet(key, it) },
        trailingIcon = {
            IconButton(
                onClick = { onRemove(key) },
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("remove_custom_field_$key"),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Cancel,
                    contentDescription = stringResource(string.remove_custom_field, key),
                )
            }
        },
        modifier = modifier
    )
}

internal fun LazyListScope.extraCustomFields(
    @StringRes label: Int,
    customFields: Map<String, String>,
    onSet: (key: String, value: String) -> Unit,
    onRemove: (key: String) -> Unit,
) {
    item {
        HorizontalDivider()
        Text(
            text = stringResource(label),
            modifier = Modifier.padding(space.medium),
            style = AppTheme.typography.labelLarge,
        )
    }
    items(count = customFields.size, key = customFields.keys::elementAt) { index ->
        val key = customFields.keys.elementAt(index)
        ExtraCustomField(
            customFieldEntry = customFields.entries.elementAt(index),
            modifier = Modifier
                .fillParentMaxWidth()
                .testTag("custom_field_$key"),
            onValueSet = onSet,
            onRemove = onRemove
        )
    }
    item {
        AddCustomField(onSet)
    }
}

@Preview
@Composable
private fun PreviewExtraCustomFields() {
    val state by remember { mutableStateOf(mapOf("Sample_key" to "some_value")) }
    AppTheme {
        Surface {
            LazyColumn {
                extraCustomFields(
                    label = string.extra_customer_fields,
                    customFields = state,
                    onSet = { key, value -> },
                    onRemove = { key -> }
                )
            }
        }
    }
}
