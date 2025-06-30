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

package com.nice.cxonechat.ui.composable

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import com.nice.cxonechat.ui.composable.theme.ModalBottomSheet
import com.nice.cxonechat.ui.domain.model.CustomValueItemList

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
    ChatTheme.ModalBottomSheet(
        title = title,
        onDismiss = onCancel,
        modifier = Modifier
            .testTag("edit_custom_values_screen")
            .then(modifier),
        onSubmit = { onConfirm(fields) },
        sheetState = sheetState,
        canSubmit = canSubmit,
    ) {
        CVFieldList(fields, onUpdated)
    }
}
