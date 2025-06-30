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

package com.nice.cxonechat.ui.viewmodel

import com.nice.cxonechat.Chat
import com.nice.cxonechat.ChatEventHandlerActions.proactiveActionClick
import com.nice.cxonechat.ChatEventHandlerActions.proactiveActionDisplay
import com.nice.cxonechat.ChatEventHandlerActions.proactiveActionFailure
import com.nice.cxonechat.ChatEventHandlerActions.proactiveActionSuccess
import com.nice.cxonechat.analytics.ActionMetadata
import com.nice.cxonechat.ui.data.model.PopupActionState
import com.nice.cxonechat.ui.domain.ReportOnPopupAction
import com.nice.cxonechat.ui.domain.ReportOnPopupAction.Clicked
import com.nice.cxonechat.ui.domain.ReportOnPopupAction.Displayed
import com.nice.cxonechat.ui.domain.ReportOnPopupAction.Failure
import com.nice.cxonechat.ui.domain.ReportOnPopupAction.Success

/**
 * Handles reporting of popup actions to the chat event system.
 *
 * @param chat The chat instance used for reporting events.
 * @param clearAction A function to clear the popup action after reporting.
 */
internal class PopupActionReporter(
    private val chat: Chat,
    private val clearAction: (PopupActionState.ReceivedPopupAction) -> Unit,
) {

    /**
     * Reports that a popup action has been displayed.
     *
     * @param metadata Metadata associated with the displayed popup action.
     */
    fun reportPopupActionDisplayed(metadata: ActionMetadata) {
        chat.events().proactiveActionDisplay(metadata)
    }

    /**
     * Reports that a popup action has been clicked.
     *
     * @param metadata Metadata associated with the clicked popup action.
     */
    fun reportOnPopupActionClicked(metadata: ActionMetadata) {
        chat.events().proactiveActionClick(metadata)
    }

    /**
     * Reports a specific type of popup action and handles its state.
     *
     * @param reportType The type of report to be made (e.g., Displayed, Clicked, Success, Failure).
     * @param action The popup action to be reported.
     */
    fun reportOnPopupAction(
        reportType: ReportOnPopupAction,
        action: PopupActionState.ReceivedPopupAction,
    ) {
        val events = chat.events()
        when (reportType) {
            Displayed -> events.proactiveActionDisplay(action.metadata)
            Clicked -> events.proactiveActionClick(action.metadata)
            Success -> {
                events.proactiveActionSuccess(action.metadata)
                clearAction(action)
            }

            Failure -> {
                events.proactiveActionFailure(action.metadata)
                clearAction(action)
            }
        }
    }
}
