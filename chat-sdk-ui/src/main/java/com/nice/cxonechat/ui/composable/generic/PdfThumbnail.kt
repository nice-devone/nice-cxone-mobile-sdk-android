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

import android.annotation.SuppressLint
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.nice.cxonechat.message.Attachment
import com.nice.cxonechat.ui.R
import com.nice.cxonechat.ui.data.source.AttachmentDataSource
import com.nice.cxonechat.ui.screen.ChatActivity
import com.nice.cxonechat.ui.util.PdfRender
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.compose.koinInject

@Composable
internal fun PdfThumbnail(
    attachment: Attachment,
    modifier: Modifier = Modifier,
    fallbackModifier: Modifier = modifier,
    fallbackSize: ThumbnailSize = ThumbnailSize.LARGE,
    showFrame: (Boolean) -> Unit,
) {
    val attachmentDataSource: AttachmentDataSource? =
        if (!LocalInspectionMode.current && LocalActivity.current is ChatActivity) {
            koinInject<AttachmentDataSource>()
        } else {
            null
        }
    var pdfRender by remember { mutableStateOf<PdfRender?>(null) }
    val uri = attachment.url
    LaunchedEffect(attachment) {
        withContext(Dispatchers.Unconfined) {
            val fd = attachmentDataSource?.getFileDescriptor(uri, attachment.friendlyName)?.getOrNull()
            if (fd != null) {
                pdfRender = PdfRender.create(fd, 1).getOrNull()
            }
        }
    }
    AnimatedContent(pdfRender) { render ->
        when (render) {
            null -> {
                showFrame(false)
                FallbackThumbnail(
                    uri = uri,
                    modifier = fallbackModifier,
                    mimeType = attachment.mimeType,
                    thumbnailSize = fallbackSize
                )
            }

            else -> {
                showFrame(true)
                PdfThumbnailContent(
                    modifier = modifier,
                    uri = uri,
                    mimeType = attachment.mimeType,
                    render = render,
                    fallbackModifier = fallbackModifier
                )
            }
        }
    }
}

@Composable
private fun PdfThumbnailContent(
    modifier: Modifier,
    uri: String,
    render: PdfRender,
    fallbackModifier: Modifier,
    mimeType: String?,
) {
    Box(
        modifier = modifier.fillMaxSize()
    ) {
        DisposableEffect(key1 = uri) {
            onDispose {
                render.close()
            }
        }
        render.pageLists.firstOrNull()?.let { page ->
            @SuppressLint(
                "UnusedBoxWithConstraintsScope" // FP - `constraints` is used in `heightByWidth`
            )
            BoxWithConstraints(
                modifier = Modifier.fillMaxWidth()
            ) {
                DisposableEffect(key1 = uri) {
                    page.load()
                    onDispose {
                        page.recycle()
                    }
                }
                page.pageContent.collectAsState().value?.asImageBitmap()?.let {
                    Image(
                        bitmap = it,
                        contentDescription = stringResource(R.string.content_description_document_preview, "pdf"),
                        modifier = Modifier.fillMaxWidth(),
                        contentScale = ContentScale.FillWidth
                    )
                } ?: FallbackThumbnail(
                    uri = uri,
                    modifier = fallbackModifier
                        .height(
                            with(LocalDensity.current.density) {
                                page.heightByWidth(constraints.maxWidth).dp
                            }
                        ),
                    mimeType = mimeType,
                )
            }
        }
    }
}
