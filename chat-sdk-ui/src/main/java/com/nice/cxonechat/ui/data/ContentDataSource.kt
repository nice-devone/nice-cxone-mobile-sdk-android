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

package com.nice.cxonechat.ui.data

import android.net.Uri
import com.nice.cxonechat.message.ContentDescriptor

/**
 * [ContentDataSource] provide an interface to allow the host application to
 * provide an attachment as a [ContentDescriptor] to the SDK for uploading
 * to the chat thread.
 */
interface ContentDataSource {
    /**
     * Regex describing content types this data source can handle.
     */
    val acceptRegex: Regex

    /**
     * test if a given mime type is acceptable to this data source.
     *
     * @param mimeType mime type to test
     * @return true iff this data source can process the given mime type
     */
    fun acceptsMimeType(mimeType: String) = acceptRegex.matchEntire(mimeType) != null

    /**
     * fetch the details (primarily the content) of a content URI
     * for an attachment.
     *
     * @param attachmentUri uri to process
     * @return details of uri prepared for uploading
     */
    suspend fun descriptorForUri(attachmentUri: Uri): ContentDescriptor?
}
