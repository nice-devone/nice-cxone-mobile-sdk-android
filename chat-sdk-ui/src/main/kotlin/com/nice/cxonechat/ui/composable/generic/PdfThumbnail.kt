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

package com.nice.cxonechat.ui.composable.generic

import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalInspectionMode
import com.nice.cxonechat.log.Logger
import com.nice.cxonechat.log.LoggerNoop
import com.nice.cxonechat.message.Attachment
import com.nice.cxonechat.ui.UiModule.Companion.LOGGER_NAME
import com.nice.cxonechat.ui.data.source.AttachmentDataSource
import com.nice.cxonechat.ui.screen.ChatActivity
import com.nice.cxonechat.ui.util.LocalPdfPageSource
import com.nice.cxonechat.ui.util.PdfPageSource
import com.nice.cxonechat.ui.util.PdfRender
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.withContext
import org.koin.compose.koinInject
import org.koin.core.qualifier.named

/**
 * Draws the first page of [attachment] as a thumbnail: a spinner until the document opens, and the file fallback
 * tile until that page has rendered.
 *
 * @param attachment The document to draw.
 * @param modifier Modifier to apply to the page itself.
 * @param fallbackModifier Modifier to apply to the spinner and to the fallback tile, defaults to [modifier].
 * @param isGroupAttachment Whether the attachment is one of several in its message, used for styling decisions.
 * @param showFrame Called with whether the message frame should be drawn behind the thumbnail.
 * @param thumbnailSize Layout of the fallback tile, which decides whether it fills the slot it was given.
 * @param contentDescription The content description for accessibility.
 */
@Composable
internal fun PdfThumbnail(
    attachment: Attachment,
    modifier: Modifier = Modifier,
    fallbackModifier: Modifier = modifier,
    isGroupAttachment: Boolean,
    showFrame: (Boolean) -> Unit,
    thumbnailSize: ThumbnailSize,
    contentDescription: String? = null,
) {
    // Keyed on the attachment so a document swapped for another takes the whole subtree that draws it with it.
    // Without the key, `AnimatedContent` keeps the outgoing page composed and drawing for its exit transition
    // while the render behind it has already been closed, and closing recycles that page's bitmap.
    key(attachment) {
        // Nothing in the chat provides the local, so this opens the attachment itself; a screenshot test stubs it.
        val source = LocalPdfPageSource.current ?: rememberAttachmentPdf(attachment)
        AnimatedContent(source) { render ->
            when (render) {
                null -> {
                    showFrame(false)
                    LoadingSpinner(modifier = fallbackModifier, isGroupAttachment = isGroupAttachment)
                }

                else -> {
                    showFrame(true)
                    PdfThumbnailContent(
                        modifier = modifier,
                        uri = attachment.url,
                        mimeType = attachment.mimeType,
                        render = render,
                        fallbackModifier = fallbackModifier,
                        thumbnailSize = thumbnailSize,
                        contentDescription = contentDescription,
                    )
                }
            }
        }
    }
}

/**
 * Opens [attachment] as a PDF, staying `null` until it is open and if opening fails. Outside [ChatActivity]
 * there is no Koin graph to ask for the file, so nothing is opened at all — a preview and a test both land
 * here.
 */
@Composable
private fun rememberAttachmentPdf(attachment: Attachment): PdfPageSource? {
    val inChatActivity = !LocalInspectionMode.current && LocalActivity.current is ChatActivity
    val attachmentDataSource: AttachmentDataSource? = if (inChatActivity) koinInject<AttachmentDataSource>() else null
    val logger = if (inChatActivity) koinInject<Logger>(named(LOGGER_NAME)) else LoggerNoop
    var pdfRender by remember(attachment) { mutableStateOf<PdfRender?>(null) }
    // The effect which opens the document also owns it: it holds the render until it is cancelled and closes it
    // there, being the only thing that opens one. Every boundary the document crosses on the way has to hand it
    // over rather than drop it. It is assigned to a local inside the `withContext` block instead of being returned
    // from it, because `withContext` discards its result when this effect is cancelled while the block runs. And
    // opening is `NonCancellable` because `PdfRender.create` is itself a `withContext`, which checks for
    // cancellation before running its body: cancelled a moment earlier, it would neither build the render nor reach
    // the failure path on which it closes the file, leaving an open file with nothing holding it.
    LaunchedEffect(attachment) {
        var opened: PdfRender? = null
        try {
            withContext(Dispatchers.IO) {
                opened = attachmentDataSource
                    ?.getFileDescriptor(attachment.url, attachment.friendlyName)
                    ?.getOrNull()
                    ?.let { fd -> withContext(NonCancellable) { PdfRender.create(fd, 1, logger) }.getOrNull() }
            }
            pdfRender = opened
            awaitCancellation()
        } finally {
            opened?.close()
        }
    }
    return pdfRender
}

@Composable
private fun PdfThumbnailContent(
    modifier: Modifier,
    uri: String,
    render: PdfPageSource,
    fallbackModifier: Modifier,
    thumbnailSize: ThumbnailSize,
    mimeType: String?,
    contentDescription: String?,
) {
    Box(
        modifier = modifier.fillMaxSize()
    ) {
        render.pageList.firstOrNull()?.let { page ->
            DisposableEffect(key1 = uri) {
                page.load()
                onDispose {
                    page.recycle()
                }
            }
            page.pageContent.collectAsState().value?.asImageBitmap()?.let {
                Image(
                    bitmap = it,
                    contentDescription = contentDescription,
                    modifier = Modifier.fillMaxWidth(),
                    contentScale = ContentScale.FillWidth
                )
            } ?: FallbackThumbnail(
                uri = uri,
                modifier = fallbackModifier,
                contentDescription = contentDescription,
                mimeType = mimeType,
                thumbnailSize = thumbnailSize,
            )
        }
    }
}
