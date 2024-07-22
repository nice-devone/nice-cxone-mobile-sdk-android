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

package com.nice.cxonechat.ui.util

import android.content.Context
import androidx.annotation.StringRes
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.nice.cxonechat.ui.R

/**
 * Show alert dialog with the given message and optional [onClick] action.
 *
 * @param message message to be shown
 * @param formatArg formatting argument(s) for the supplied [message], see [Context.getString].
 * @param onClick Action to performed on positive button click.
 */
internal fun Context.showAlert(@StringRes message: Int, vararg formatArg: String, onClick: () -> Unit = {}) =
    showAlert(
        if (formatArg.isEmpty()) getString(message) else getString(message, *formatArg),
        onClick
    )

internal fun Context.showAlert(message: String, onClick: () -> Unit = {}) {
    val builder = MaterialAlertDialogBuilder(this)
    builder.setTitle(R.string.information)
    builder.setMessage(message)

    builder.setPositiveButton(R.string.ok) { dialog, _ ->
        onClick()
        dialog.cancel()
    }

    builder.show()
}
