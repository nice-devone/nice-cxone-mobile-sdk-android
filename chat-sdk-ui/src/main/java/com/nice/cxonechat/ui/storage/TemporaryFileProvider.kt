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

package com.nice.cxonechat.ui.storage

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.nice.cxonechat.ui.R
import java.io.File

/**
 * File provider for attachments which are stored via [TemporaryFileStorage].
 */
class TemporaryFileProvider : FileProvider(R.xml.tmp_file_path) {

    internal companion object {
        private const val AUTHORITY = "com.nice.cxonechat.fileprovider"
        fun getUriForFile(file: File, filename: String, context: Context): Uri =
            getUriForFile(context, AUTHORITY, file, filename)
    }
}
