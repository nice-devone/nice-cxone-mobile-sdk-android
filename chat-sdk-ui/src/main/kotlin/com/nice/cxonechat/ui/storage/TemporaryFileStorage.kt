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

package com.nice.cxonechat.ui.storage

import android.app.Application
import org.koin.core.annotation.Single
import java.io.File
import java.util.UUID

/**
 * This class manages storage of files which should be accessible via [TemporaryFileProvider].
 * Main usage is for sharing of attachments with other applications on the device.
 */
@Single
internal class TemporaryFileStorage(
    context: Application, // Using Application context to avoid leaks.
) {
    private val cacheDir: File by lazy { context.cacheDir }

    // These folders have to match what is defined in [com.nice.cxonechat.ui.storage.TemporaryFileProvider] resources.
    private val cacheFolder: File by lazy { getOrCreateFolder("/tmp/") }
    private val captureFolder: File by lazy { getOrCreateFolder("/capture/") }

    private fun getOrCreateFolder(child: String): File {
        val directory = File(cacheDir, child)
        if (!directory.exists()) {
            directory.mkdirs()
        }
        return directory
    }

    /**
     * Creates a new temporary file with a randomly generated name.  A [File] reference will be returned.
     *
     * @param suffix An optional suffix for the file name.
     */
    fun createFile(suffix: String? = null): File? {
        val file = File(cacheFolder, UUID.randomUUID().toString() + suffix.orEmpty())
        val fileCreated = runCatching { file.createNewFile() }
        return if (fileCreated.isSuccess) file else null
    }

    /**
     * Creates a new capture file in the capture folder with the given prefix and suffix.
     *
     * @param prefix Prefix for the file name.
     * @param suffix Suffix for the file name (including dot, e.g. ".jpeg").
     */
    fun createCaptureFile(prefix: String, suffix: String): File? {
        val fileName = prefix + UUID.randomUUID().toString() + suffix
        val file = File(captureFolder, fileName)
        val fileCreated = runCatching { file.createNewFile() }
        return if (fileCreated.isSuccess) file else null
    }

    fun clear() {
        cacheFolder.deleteRecursively()
        cacheFolder.mkdirs()
        captureFolder.deleteRecursively()
        captureFolder.mkdirs()
    }
}
