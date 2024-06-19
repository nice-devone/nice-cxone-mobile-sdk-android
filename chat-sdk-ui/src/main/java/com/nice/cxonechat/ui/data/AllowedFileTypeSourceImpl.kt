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

import com.nice.cxonechat.ChatInstanceProvider
import com.nice.cxonechat.state.FileRestrictions
import org.koin.core.annotation.Factory

/**
 * Source of allowed attachment mime-types.
 */
internal interface AllowedFileTypeSource {

    /**
     * List of allowed mime-types.
     */
    val allowedMimeTypes: List<AllowedFileType>
}

/**
 * UI representation of allowed file/mime type.
 *
 * @property description User presentable description of the file/mime type.
 * @property mimeType Allowed mime-type, it can be also contain a wildcard.
 */
internal data class AllowedFileType(val description: String, val mimeType: String) {

    constructor(allowedFileType: FileRestrictions.AllowedFileType) : this(allowedFileType.description, allowedFileType.mimeType)
    internal constructor(pair: Pair<Description, MimeType>) : this(pair.first, pair.second)
}

private typealias Description = String
private typealias MimeType = String

@Factory(binds = [AllowedFileTypeSource::class])
internal class AllowedFileTypeSourceImpl(
    private val chatInstanceProvider: ChatInstanceProvider,
) : AllowedFileTypeSource {

    override val allowedMimeTypes by lazy {
        chatInstanceProvider
            .chat
            ?.configuration
            ?.fileRestrictions
            ?.allowedFileTypes
            .orEmpty()
            .map(::AllowedFileType)
    }
}
