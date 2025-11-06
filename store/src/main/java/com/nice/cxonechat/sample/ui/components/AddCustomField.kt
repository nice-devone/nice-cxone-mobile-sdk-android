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

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.nice.cxonechat.sample.R
import com.nice.cxonechat.sample.ui.theme.AppTheme

@Composable
internal fun AddCustomField(onSet: (String, String) -> Unit) {
    val newKey = rememberTextFieldState()
    val newValue = rememberTextFieldState()
    HorizontalDivider(modifier = Modifier.padding(vertical = AppTheme.space.medium))
    Text(
        text = stringResource(id = R.string.add_custom_field),
        modifier = Modifier.padding(AppTheme.space.medium),
        style = AppTheme.typography.labelMedium
    )
    Box(
        Modifier.Companion
            .border(1.dp, AppTheme.colorScheme.onSurface.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
            .padding(AppTheme.space.medium)
    ) {
        Column {
            OutlinedTextField(
                state = newKey,
                label = { Text(stringResource(R.string.new_custom_key)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("add_custom_field_key"),
            )
            OutlinedTextField(
                state = newValue,
                label = { Text(stringResource(R.string.new_custom_value)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("add_custom_field_value"),
            )
            Button(
                onClick = {
                    onSet(newKey.text.toString(), newValue.text.toString())
                    newKey.clearText()
                    newValue.clearText()
                },
                shape = AppTheme.shapes.medium,
                enabled = newKey.text.isNotEmpty() && newValue.text.isNotEmpty(),
                contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
                modifier = Modifier
                    .align(Alignment.Companion.End)
                    .padding(top = AppTheme.space.medium)
                    .testTag("add_custom_field_button")
            ) {
                Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = stringResource(R.string.add_custom_field),
                    modifier = Modifier.size(ButtonDefaults.IconSize)
                )
                Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                Text(stringResource(R.string.add_custom_field))
            }
        }
    }
}
