/*
 * Copyright (c) 2026. NICE Ltd. All rights reserved.
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

import com.nice.cxonechat.message.Attachment
import com.nice.cxonechat.message.Message
import com.nice.cxonechat.ui.viewmodel.PreparedAttachment
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Factory
import java.io.File
import java.util.UUID

/**
 * Makes the local copy of a sent PDF the cached preview of the uploaded attachment, so that preview is not downloaded
 * after the message is echoed back.
 *
 * The echo is paired with the sent attachments by message id and position. The pairing is used only when the echo has
 * as many attachments as were sent and every position has the same MIME type, and the same name where a copy exists.
 * Otherwise every copy is deleted and the preview is downloaded as usual: a wrong pairing would show another file.
 */
@Factory
internal class SentAttachmentSeeder(
    private val attachmentDataSource: AttachmentDataSource,
) {
    private val pending = MutableStateFlow<Map<UUID, List<Seed>>>(emptyMap())

    /** Ids of sent messages whose echo has not been handled yet. */
    val pendingIds: Set<UUID> get() = pending.value.keys

    /** Copies every PDF among [attachments]; the result keeps their order. */
    suspend fun seed(attachments: List<PreparedAttachment>): List<Seed> = attachments.map { attachment ->
        val friendlyName = attachment.contentDescriptor.friendlyName
        val file = if (attachment.mimeType.baseType() == PDF && friendlyName != null) attachmentDataSource.seed(attachment.url) else null
        Seed(friendlyName, attachment.mimeType, file)
    }

    /** Waits for the echo of [messageId]. A message without copies is not tracked. */
    fun register(messageId: UUID, seeds: List<Seed>) {
        if (seeds.any { it.file != null }) pending.update { it + (messageId to seeds) }
    }

    /**
     * Moves or deletes the copies of [messageId]. Runs once per message; later calls do nothing. Not cancellable: once
     * the echo is claimed, nothing else would handle its copies.
     */
    suspend fun onEcho(messageId: UUID, echoed: List<Attachment>) = withContext(NonCancellable) {
        val seeds = pending.getAndUpdate { it - messageId }[messageId] ?: return@withContext
        val paired = seeds.size == echoed.size && seeds.zip(echoed).all { (seed, remote) -> seed.matches(remote) }
        seeds.forEachIndexed { index, seed ->
            val file = seed.file ?: return@forEachIndexed
            if (paired) {
                attachmentDataSource.adoptSeed(file, echoed[index].url, echoed[index].friendlyName)
            } else {
                attachmentDataSource.discardSeed(file)
            }
        }
    }

    /** Runs [onEcho] for every pending message present in [received]. */
    suspend fun adoptEchoes(received: Map<UUID, Message>) {
        pendingIds.forEach { id -> received[id]?.let { echo -> onEcho(id, echo.attachments.toList()) } }
    }

    /** Deletes copies whose message was never sent. */
    suspend fun discard(seeds: List<Seed>) {
        seeds.forEach { seed -> seed.file?.let { attachmentDataSource.discardSeed(it) } }
    }

    internal data class Seed(val friendlyName: String?, val mimeType: String, val file: File?) {
        fun matches(remote: Attachment): Boolean =
            mimeType.baseType() == remote.mimeType?.baseType() && (file == null || friendlyName == remote.friendlyName)
    }

    private companion object {
        const val PDF = "application/pdf"

        /** The MIME type without parameters, in lower case, so `Application/PDF; version=1.7` is a PDF. */
        fun String.baseType(): String = substringBefore(';').trim().lowercase()
    }
}
