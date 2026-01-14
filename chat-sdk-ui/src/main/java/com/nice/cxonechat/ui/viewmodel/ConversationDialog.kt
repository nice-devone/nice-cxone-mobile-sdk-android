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

package com.nice.cxonechat.ui.viewmodel

import android.net.Uri
import com.nice.cxonechat.message.Attachment
import java.util.UUID
import com.nice.cxonechat.Popup as SdkPopup

/**
 * Represents different types of dialogs that can be displayed in the conversation UI.
 */
internal sealed interface ConversationDialog {

    /**
     * Represents dialogs that occupy the full screen.
     */
    sealed interface FullScreenDialog : ConversationDialog

    /**
     * Default state, where no dialog should be shown.
     */
    data object None : ConversationDialog

    /**
     * Represents a dialog for editing of custom values in the conversation.
     */
    data object CustomValues : ConversationDialog

    /**
     * Represents a dialog for editing the conversation name.
     */
    data object EditThreadName : ConversationDialog

    /**
     * Represents a dialog for selecting attachments in the conversation.
     *
     * @property attachments The list of prepared attachments.
     */
    data class SelectAttachments(
        val attachments: List<Attachment>,
    ) : ConversationDialog

    /**
     * Represents a fulscreen video player dialog.
     *
     * @property uri The URI of the video to be played.
     * @property title The title of the video, if available.
     * @property attachment The attachment associated with the video.
     */
    data class VideoPlayer(
        val uri: String,
        val title: String?,
        val attachment: Attachment,
    ) : FullScreenDialog

    /**
     * Represents a fullscreen image viewer dialog.
     *
     * @property image The image to be displayed. Can be of any type.
     * @property title The title of the image, if available.
     * @property attachment The attachment associated with the image.
     */
    data class ImageViewer(
        val image: Any?,
        val title: String?,
        val attachment: Attachment,
    ) : FullScreenDialog

    /**
     * Represents a dialog for displaying invalid attachments.
     *
     * @property attachments A map of invalid attachments, where the key is the URI of the attachment
     * and the value is the reason why it is invalid.
     */
    data class InvalidAttachments(
        val attachments: Map<Uri, String>,
    ) : ConversationDialog

    /**
     * Represents a dialog for end of a contact UX.
     */
    data object EndContact : ConversationDialog

    /**
     * Represents a popup dialog.
     *
     * @property popup The popup to be displayed.
     * @property threadId The ID of the thread associated with the popup.
     */
    data class Popup(val popup: SdkPopup, val threadId: UUID) : FullScreenDialog
}
