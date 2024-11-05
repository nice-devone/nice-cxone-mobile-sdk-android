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

package com.nice.cxonechat.ui

import android.net.Uri
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.contract.ActivityResultContracts.GetContent
import androidx.activity.result.contract.ActivityResultContracts.OpenDocument
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner

/**
 * This class is responsible for launching a system activity which will report selected attachment(s)
 * via the provided [sendAttachment] callback.
 *
 * The [onCreate] method must be called from the owning activity's onCreate method (before the activity is in state Started).
 */
internal class SelectAttachmentActivityLauncher(
    private val sendAttachment: (uri: Uri) -> Unit,
    private val registry: ActivityResultRegistry
) : DefaultLifecycleObserver {
    private var getContent: ActivityResultLauncher<String>? = null
    private var getDocument: ActivityResultLauncher<Array<String>>? = null

    override fun onCreate(owner: LifecycleOwner) {
        getContent = registry.register("com.nice.cxonechat.ui.content", owner, GetContent()) { uri ->
            val safeUri = uri ?: return@register
            sendAttachment(safeUri)
        }
        getDocument = registry.register("com.nice.cxonechat.ui.document", owner, OpenDocument()) { uri ->
            val safeUri = uri ?: return@register
            sendAttachment(safeUri)
        }
    }

    /**
     * start a foreign activity to find an attachment with the indicated mime types
     *
     * [mimeTypes] is one of the strings supplied by the chat instance.
     *
     * Note that this will work for finding existing resources, but not for opening
     * the camera for photos or videos.
     *
     * @param mimeTypes attachment types to find.
     *
     */
    fun getDocument(mimeTypes: Array<String>) {
        if (mimeTypes.size == 1) {
            getContent?.launch(mimeTypes[0])
        } else {
            getDocument?.launch(mimeTypes)
        }
    }
}
