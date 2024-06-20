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

package com.nice.cxonechat.state

import com.nice.cxonechat.Public

/**
 * Restrictions to be enforced on uploaded attachments.
 */
@Public
interface FileRestrictions {
    /** Maximum uploaded file size in MB. */
    val allowedFileSize: Int

    /** File types allowed to be uploaded. */
    val allowedFileTypes: List<AllowedFileType>

    /** True iff attachments may be uploaded.  If false, no uploads are allowed. */
    val isAttachmentsEnabled: Boolean

    /**
     * Defines a mime type allowed to be uploaded.
     */
    @Public
    interface AllowedFileType {
        /**
         * Mime to be allowed.
         *
         * This may take the form `type&slash;*` in which case the type must match "type" but
         * all subtypes are allowed.
         */
        val mimeType: String

        /** description of allowed mime type. */
        val description: String
    }
}
