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

package com.nice.cxonechat.ui.data.source

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import androidx.core.net.toUri
import com.nice.cxonechat.log.Logger
import com.nice.cxonechat.log.LoggerNoop
import com.nice.cxonechat.log.LoggerScope
import com.nice.cxonechat.log.debug
import com.nice.cxonechat.ui.UiModule
import com.nice.cxonechat.ui.util.await
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import org.koin.core.annotation.Named
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single
import java.io.File
import java.security.MessageDigest
import java.util.UUID
import kotlin.coroutines.resumeWithException

/**
 * A data source class for handling file attachments.
 * This class provides functionality to access files from both local and remote URIs,
 * caching remote files locally for efficient reuse.
 *
 * @param context The application context used for accessing resources and cache directories.
 * @param okHttpClient The preconfigured [OkHttpClient] instance used for making network requests.
 * @param logger Receives cache hits, misses, clears and seed outcomes. Never a URL or a local Uri.
 */
@Single
internal class AttachmentDataSource(
    private val context: Context,
    private val okHttpClient: OkHttpClient,
    @Provided
    @Named(UiModule.LOGGER_NAME)
    logger: Logger = LoggerNoop,
) : LoggerScope by LoggerScope<AttachmentDataSource>(logger) {

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
     * The caller owns the returned descriptor and closes it. A descriptor opened just as the caller is cancelled
     * is closed here instead, because the value never reaches the caller: [withContext] discards its result when
     * the calling job is cancelled while the block runs, so the local it was assigned to is the only thing left
     * holding it.
     *
     * @param uri The URI of the file to access.
     * @param fileName An optional display name. Only its extension is kept on the cached file.
     * @return A [Result] containing the [ParcelFileDescriptor] or an exception if an error occurs.
     */
    @SuppressLint("Recycle")
    suspend fun getFileDescriptor(uri: String, fileName: String?): Result<ParcelFileDescriptor> {
        var opened: ParcelFileDescriptor? = null
        return try {
            withContext(Dispatchers.IO) {
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
                        else -> openLocalFileDescriptor(parsedUri)
                    }.also { opened = it }
                }
            }
        } catch (expected: CancellationException) {
            // Dropped rather than thrown: a failing close must not replace the cancellation on its way out.
            runCatching { opened?.close() }
            throw expected
        }
    }

    /**
     * Opens [uri] through the [android.content.ContentResolver].
     *
     * The descriptor is closed here when cancellation lands between the open returning one and the resumed value
     * reaching the caller: [kotlin.coroutines.Continuation.resume] discards its value in that window, leaving the
     * cancellation handler as the only place that can still see it.
     */
    private suspend fun openLocalFileDescriptor(uri: Uri): ParcelFileDescriptor = suspendCancellableCoroutine { continuation ->
        val cancellationSignal = CancellationSignal()
        continuation.invokeOnCancellation {
            cancellationSignal.cancel()
        }
        val fd = context.contentResolver.openFileDescriptor(uri, "r", cancellationSignal)
        if (fd != null) {
            continuation.resume(fd) { _, descriptor, _ ->
                cancellationSignal.cancel()
                // Dropped rather than thrown: a cancellation handler which throws yields a
                // CompletionHandlerException with nothing left to catch it.
                runCatching { descriptor.close() }
            }
        } else {
            continuation.resumeWithException(IllegalArgumentException("Could not open file descriptor for $uri"))
        }
    }

    /**
     * Downloads a remote file and caches it locally.
     * If the file already exists in the cache, it is reused.
     *
     * @param url The URL of the remote file to download. Its path alone names the cached file, so two attachments
     * that share a display name never share a file, and a re-signed URL still finds its file.
     * @param fileName An optional display name. Only its extension is kept.
     * @return A [File] object representing the cached file.
     */
    private suspend fun downloadAndCacheFile(url: String, fileName: String?): File = withContext(Dispatchers.IO) {
        val cachedFile = File(getCacheDir(), cacheFileName(url, fileName))

        if (cachedFile.exists()) {
            debug("Cache hit: ${cachedFile.name}")
        } else {
            val written = okHttpClient.newCall(
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
            debug("Cache miss: ${cachedFile.name}, wrote $written bytes")
        }
        cachedFile
    }

    /**
     * Copies a picked local file into the cache folder. Once its message is echoed back, [adoptSeed] moves the copy
     * onto the uploaded attachment's cache name, so that preview needs no download.
     *
     * @param localUri The content Uri of the picked file.
     * @return The copy, or `null` when the file could not be read.
     */
    suspend fun seed(localUri: String): File? = withContext(Dispatchers.IO) {
        val seed = File(getCacheDir(), "$SEED_PREFIX${UUID.randomUUID()}")
        runCatching {
            context.contentResolver.openInputStream(localUri.toUri()).use { input ->
                requireNotNull(input) { "No content for the picked file" }
                seed.outputStream().use { output -> input.copyTo(output) }
            }
        }.fold(
            onSuccess = { seed },
            onFailure = {
                seed.delete()
                null
            },
        )
    }

    /**
     * Moves [seed] onto the cache name of the attachment at [url]. When that attachment is already cached, [seed] is
     * deleted instead.
     */
    suspend fun adoptSeed(seed: File, url: String, fileName: String?) {
        withContext(Dispatchers.IO) {
            val target = File(getCacheDir(), cacheFileName(url, fileName))
            if (!target.exists() && seed.renameTo(target)) {
                debug("Seed moved to ${target.name}")
            } else {
                seed.delete()
                debug("Seed deleted, ${target.name} already cached")
            }
        }
    }

    /** Deletes a seed that will not be adopted. */
    suspend fun discardSeed(seed: File) {
        withContext(Dispatchers.IO) { seed.delete() }
    }

    /**
     * Clears all cached files from the cache directory.
     */
    suspend fun clearCache() = withContext(Dispatchers.IO) {
        val removed = getCacheDir().listFiles().orEmpty().count { it.delete() }
        debug("Removed $removed cached files")
    }

    private fun cacheFileName(url: String, fileName: String?): String {
        // The query carries a download token that changes on every event; the path holds the attachment id.
        val key = url.toHttpUrlOrNull()?.newBuilder()?.query(null)?.fragment(null)?.build()?.toString() ?: url
        val hash = MessageDigest.getInstance("SHA-256").digest(key.toByteArray()).toHexString()
        val extension = fileName?.substringAfterLast('.', "")?.takeIf(SAFE_EXTENSION::matches)
        return if (extension == null) hash else "$hash.$extension"
    }

    private companion object {
        const val SEED_PREFIX = "seed-"
        val SAFE_EXTENSION = Regex("[A-Za-z0-9]{1,8}")
    }
}
