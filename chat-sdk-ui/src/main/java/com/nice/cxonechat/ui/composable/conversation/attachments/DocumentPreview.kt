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

package com.nice.cxonechat.ui.composable.conversation.attachments

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.nice.cxonechat.message.Attachment
import com.nice.cxonechat.ui.composable.generic.PdfThumbnail
import com.nice.cxonechat.ui.composable.generic.ThumbnailSize

@Composable
internal fun DocumentPreview(
    attachment: Attachment,
    modifier: Modifier,
    thumbnailSize: ThumbnailSize,
    showFrame: (Boolean) -> Unit,
) {
    // For now we only support PDF documents, so we can use the PDF thumbnail which has fallback
    PdfThumbnail(attachment = attachment, modifier = modifier, fallbackSize = thumbnailSize, showFrame = showFrame)
}
