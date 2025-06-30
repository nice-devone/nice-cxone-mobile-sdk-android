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

package com.nice.cxonechat.ui.domain.model

/**
 * Represents the possible user choices when ending a conversation.
 *
 * @see SHOW_TRANSCRIPT Option to display the conversation transcript to the user.
 * @see NEW_CONVERSATION Option to start a new conversation after ending the current one.
 * @see CLOSE_CHAT Option to simply close the chat interface.
 */
internal enum class EndConversationChoice {
    /**
     * Show the transcript of the ended conversation.
     */
    SHOW_TRANSCRIPT,

    /**
     * Start a new conversation after ending the current one.
     */
    NEW_CONVERSATION,

    /**
     * Close the chat interface without any further actions.
     */
    CLOSE_CHAT,
}
