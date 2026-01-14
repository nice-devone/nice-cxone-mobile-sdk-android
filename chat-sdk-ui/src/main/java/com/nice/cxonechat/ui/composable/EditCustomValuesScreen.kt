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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewParameter
import com.nice.cxonechat.ui.R
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import com.nice.cxonechat.ui.composable.theme.ChatTheme.chatColors
import com.nice.cxonechat.ui.composable.theme.ChatTheme.space
import com.nice.cxonechat.ui.composable.theme.ModalBottomSheet
import com.nice.cxonechat.ui.domain.model.CustomValueItemList
import com.nice.cxonechat.ui.util.preview.FieldModelListProvider

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EditCustomValuesScreen(
    title: String,
    fields: CustomValueItemList,
    sheetState: SheetState,
    modifier: Modifier = Modifier,
    canSubmit: Boolean = true,
    onUpdated: (CustomValueItemList) -> Unit = {},
    onCancel: () -> Unit,
    onConfirm: (CustomValueItemList) -> Unit,
) {
    val listState = remember(fields) { mutableStateOf(fields) }
    val validatedSubmit = remember(listState.value, canSubmit) {
        derivedStateOf {
            canSubmit && listState.value.none { item ->
                runCatching {
                    item.stringValue()?.let { item.definition.validate(it) }
                }.isFailure
            }
        }
    }.value
    ChatTheme.ModalBottomSheet(
        title = title,
        subtitle = stringResource(R.string.edit_custom_field_subtitle),
        onDismiss = onCancel,
        modifier = modifier.testTag("edit_custom_values_screen"),
        onSubmit = { onConfirm(listState.value) },
        sheetState = sheetState,
        canSubmit = validatedSubmit,
    ) {
        CVFieldList(
            fields = listState.value,
            modifier = Modifier.fillMaxSize(),
            onUpdated = { newList: CustomValueItemList ->
                listState.value = newList
                onUpdated(newList)
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@PreviewLightDark
@Composable
private fun EditCustomValuesScreenPreview(
    @PreviewParameter(FieldModelListProvider::class) fields: CustomValueItemList,
) {
    ChatTheme {
        Surface(
            color = chatColors.token.background.surface.subtle,
        ) {
            Box(Modifier.padding(space.large)) {
                EditCustomValuesScreen(
                    title = "Pre-Contact Survey",
                    fields = fields,
                    sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                    onCancel = {},
                    onConfirm = {},
                )
            }
        }
    }
}
