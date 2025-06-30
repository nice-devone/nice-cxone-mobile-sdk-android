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

package com.nice.cxonechat.ui.data.source

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import androidx.core.net.toUri
import com.nice.cxonechat.ui.util.await
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.koin.core.annotation.Single
import java.io.File
import java.util.UUID
import kotlin.coroutines.resumeWithException

/**
 * A data source class for handling file attachments.
 * This class provides functionality to access files from both local and remote URIs,
 * caching remote files locally for efficient reuse.
 *
 * @param context The application context used for accessing resources and cache directories.
 * @param okHttpClient The preconfigured [OkHttpClient] instance used for making network requests.
 */
@Single
internal class AttachmentDataSource(
    private val context: Context,
    private val okHttpClient: OkHttpClient,
) {

    /**
     * Retrieves the cache directory for storing remote files.
     * Ensures the directory exists by creating it if necessary.
     *
     * @return A [File] object representing the cache directory.
     */
    private suspend fun getCacheDir() = withContext(Dispatchers.IO) {
        File(context.cacheDir, "remote_files").apply { mkdirs() }
    }

    /**
     * Retrieves a [ParcelFileDescriptor] for the given URI.
     * If the URI points to a remote file, it downloads and caches the file locally.
     *
     * @param uri The URI of the file to access.
     * @param fileName An optional file name to use when caching the file.
     * @return A [Result] containing the [ParcelFileDescriptor] or an exception if an error occurs.
     */
    @SuppressLint("Recycle")
    suspend fun getFileDescriptor(uri: String, fileName: String?): Result<ParcelFileDescriptor> = withContext(Dispatchers.IO) {
        runCatching {
            val parsedUri: Uri = uri.toUri()

            when (parsedUri.scheme) {
                "http", "https" -> {
                    // Handle remote file access
                    requireNotNull(parsedUri.host) { "Missing domain is not allowed" }
                    val cachedFile = downloadAndCacheFile(uri, fileName)
                    ParcelFileDescriptor.open(cachedFile, ParcelFileDescriptor.MODE_READ_ONLY)
                }

                // Handle local file access
                else -> suspendCancellableCoroutine { continuation ->
                    val cancellationSignal = CancellationSignal()
                    continuation.invokeOnCancellation {
                        cancellationSignal.cancel()
                    }
                    val fd = context.contentResolver.openFileDescriptor(parsedUri, "r", cancellationSignal)
                    if (fd != null) {
                        continuation.resume(fd) { cause, _, _ -> cancellationSignal.cancel() }
                    } else {
                        continuation.resumeWithException(IllegalArgumentException("Could not open file descriptor for $uri"))
                    }
                }
            }
        }
    }

    /**
     * Downloads a remote file and caches it locally.
     * If the file already exists in the cache, it is reused.
     *
     * @param url The URL of the remote file to download.
     * @param fileName An optional file name to use when saving the file.
     * @return A [File] object representing the cached file.
     */
    private suspend fun downloadAndCacheFile(url: String, fileName: String?): File = withContext(Dispatchers.IO) {
        val cacheDir = getCacheDir()
        val filename = fileName ?: url.toUri().lastPathSegment ?: UUID.randomUUID().toString()
        val cachedFile = File(cacheDir, filename)

        if (!cachedFile.exists()) {
            // Download and save the file
            okHttpClient.newCall(
                Request.Builder()
                    .url(url)
                    .build()
            )
                .await()
                .byteStream()
                .use { input ->
                    cachedFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
        }
        cachedFile
    }

    /**
     * Clears all cached files from the cache directory.
     */
    suspend fun clearCache() = withContext(Dispatchers.IO) {
        getCacheDir().listFiles()?.forEach { it.delete() }
    }
}
