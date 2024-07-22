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

@file:Suppress("ForbiddenImport")

package com.nice.cxonechat.ui.domain

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.nice.cxonechat.log.Logger
import com.nice.cxonechat.log.LoggerScope
import com.nice.cxonechat.log.error
import com.nice.cxonechat.log.scope
import com.nice.cxonechat.message.Attachment
import com.nice.cxonechat.ui.R.string
import com.nice.cxonechat.ui.storage.TemporaryFileProvider
import com.nice.cxonechat.ui.storage.TemporaryFileStorage
import com.nice.cxonechat.ui.util.await
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request.Builder
import okio.buffer
import okio.source
import org.koin.core.annotation.Single
import java.io.File
import java.io.InputStream

/**
 * Class responsible for caching of attachments before they can be shared
 * and creation of [Intent] which will be used for sharing.
 *
 * @param storage [TemporaryFileStorage] to cache files before sharing.
 * @param httpClient [OkHttpClient] used to fetch remote attachments for sharing.
 * @param logger [Logger] for logging warnings and errors.
 */
@Single
internal class AttachmentSharingRepository(
    private val storage: TemporaryFileStorage,
    private val httpClient: OkHttpClient,
    private val logger: Logger,
) : LoggerScope by LoggerScope<AttachmentSharingRepository>(logger) {
    /** return the type of a string representing mimetype. */
    private val String.type: String?
        get() = split("/").firstOrNull()

    /**
     * Caches file from the original url to storage and creates Intent with action [Intent.ACTION_SEND] set to provide
     * data via stream from [TemporaryFileProvider].
     *
     * @param attachments Attachments that will be cached to local storage and shared.
     * @param context Context which will be used for caching.
     * @return Prepared intent or null if caching has failed.
     */
    suspend fun createSharingIntent(
        attachments: Collection<Attachment>,
        context: Context,
    ): Intent? {
        val mapped = attachments.mapNotNull {
            context.prepareAttachment(it)
        }

        return when (mapped.count()) {
            0 -> null
            1 -> Intent().apply {
                val attachment = mapped.first()
                val url = Uri.parse(attachment.url)

                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TITLE, context.getString(string.share_attachment, attachment.friendlyName))
                putExtra(Intent.EXTRA_TEXT, attachment.friendlyName)
                putExtra(Intent.EXTRA_STREAM, url)
                setDataAndType(url, attachment.mimeType)
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            }

            else -> Intent().apply {
                action = Intent.ACTION_SEND_MULTIPLE
                putExtra(
                    Intent.EXTRA_TITLE,
                    context.getString(
                        string.share_attachment_others,
                        mapped.first().friendlyName,
                        mapped.count() - 1
                    )
                )
                putExtra(Intent.EXTRA_TEXT, ArrayList(mapped.map(Attachment::friendlyName)))
                putExtra(Intent.EXTRA_STREAM, ArrayList(mapped.map { Uri.parse(it.url) }))
                type = mapped.map(Attachment::mimeType).commonMimeType()
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
        }
    }

    private fun Collection<String?>.commonMimeType() = fold(null as String?) { acc, value ->
        when {
            acc == null -> value ?: "*/*"
            acc == value -> acc
            acc.type == value?.type -> "${acc.type}/*"
            else -> "*/*"
        }
    } ?: "*/*"

    private suspend fun Context.prepareAttachment(attachment: Attachment) = scope("Context.prepareAttachment") {
        runCatching {
            val file = cacheUrlContents(url = attachment.url, hint = attachment.friendlyName) ?: return null
            val uri = TemporaryFileProvider.getUriForFile(file, file.name, this@prepareAttachment)

            object : Attachment {
                override val url = uri.toString()
                override val friendlyName = attachment.friendlyName
                override val mimeType = attachment.mimeType
            }
        }
            .onFailure {
                error("Error preparing: ${attachment.friendlyName}", it)
            }
            .getOrNull()
    }

    private suspend fun Context.cacheUrlContents(
        url: String,
        hint: String,
    ): File? = scope("Context.cacheUrlContents") {
        val uri = Uri.parse(url)
        val stream: InputStream = when (uri.scheme) {
            // Attachment can be available via content if the message was just sent, before it is updated from backend.
            "content" ->
                runCatching {
                    contentResolver.openInputStream(uri)
                }
                    .onFailure {
                        error("Error opening content: $url", it)
                    }
                    .getOrNull()

            "http", "https" ->
                runCatching {
                    httpClient.newCall(
                        Builder().url(url).build()
                    ).await()
                }
                    .onFailure {
                        error("Error opening http: $url", it)
                    }
                    .getOrNull()
                    ?.byteStream()

            else -> null
        } ?: return null

        return storeToCache(stream, hint)
    }

    /**
     * Simple function which writes given [InputStream] to file which will be accessible from [TemporaryFileProvider].
     * A new filename will be randomly created.  [hint] will be used for error messages.
     */
    private suspend fun storeToCache(inputStream: InputStream, hint: String): File? = scope("storeToCache") {
        withContext(Dispatchers.IO) {
            runCatching {
                storage.createFile()?.also { file ->
                    file.outputStream().buffered().use { output ->
                        inputStream.source().buffer().use { source ->
                            source.buffer.copyTo(output)
                        }
                    }
                }
            }
                .onFailure {
                    error("Error copying: $hint", it)
                }
                .getOrNull()
        }
    }
}
