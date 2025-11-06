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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import com.nice.cxonechat.message.Attachment
import com.nice.cxonechat.ui.composable.generic.PresetAsyncImage
import com.nice.cxonechat.ui.util.contentDescription
import java.util.UUID

@Composable
internal fun ImagePreview(
    attachment: Attachment,
    modifier: Modifier = Modifier,
    messageId: UUID? = null,
) {
    val cacheKey = rememberSaveable(messageId, attachment.friendlyName) {
        "${messageId?.let { "${it}_" }}${attachment.friendlyName}"
    }
    PresetAsyncImage(
        model = attachment.url,
        cacheKey = cacheKey,
        contentDescription = attachment.contentDescription,
        contentScale = ContentScale.Crop,
        modifier = modifier,
    )
}
