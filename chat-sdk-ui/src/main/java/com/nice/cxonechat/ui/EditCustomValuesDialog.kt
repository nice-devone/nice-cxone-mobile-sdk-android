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

package com.nice.cxonechat.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import com.nice.cxonechat.ui.composable.theme.Dialog
import com.nice.cxonechat.ui.composable.theme.OutlinedButton
import com.nice.cxonechat.ui.customvalues.CVFieldList
import com.nice.cxonechat.ui.customvalues.CustomValueItemList

@Composable
internal fun EditCustomValuesDialog(
    title: String,
    fields: CustomValueItemList,
    modifier: Modifier = Modifier,
    onCancel: () -> Unit,
    onConfirm: (CustomValueItemList) -> Unit,
) {
    ChatTheme.Dialog(
        title = title,
        onDismiss = onCancel,
        modifier = modifier,
        confirmButton = { ChatTheme.OutlinedButton("Ok") { onConfirm(fields) } },
        dismissButton = { ChatTheme.OutlinedButton(text = "Cancel", onClick = onCancel) }
    ) {
        CVFieldList(fields)
    }
}
