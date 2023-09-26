/*
 * Copyright (c) 2021-2023. NICE Ltd. All rights reserved.
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
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * This class manages storage of files which should be accessible via [TemporaryFileProvider].
 */
@Singleton
internal class TemporaryFileStorage(
    context: Context,
    private val baseDirectory: String?,
) {

    private val cacheDir: File by lazy { baseDirectory?.let(::File) ?: context.cacheDir }
    private val cacheFolder: File by lazy {
        val directory = File(cacheDir, "/tmp/")
        if (!directory.exists()) {
            directory.mkdirs()
        }
        directory
    }

    @Inject
    constructor(@ApplicationContext context: Context) : this(context, null)

    fun createFile(name: String): File? {
        val file = File(cacheFolder, name)
        val fileCreated = runCatching { file.createNewFile() }
        return if (fileCreated.isSuccess) file else null
    }

    fun clear() {
        cacheFolder.deleteRecursively()
        cacheFolder.mkdirs()
    }
}
