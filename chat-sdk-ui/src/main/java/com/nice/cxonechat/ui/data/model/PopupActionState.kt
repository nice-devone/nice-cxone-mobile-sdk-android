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

package com.nice.cxonechat.ui.data.model

import com.nice.cxonechat.Popup

/**
 * Represents the state of a popup action in the UI.
 */
internal sealed interface PopupActionState {
    /**
     * Represents a default, empty state where no popup action is present.
     */
    data object Empty : PopupActionState

    /**
     * Represents a state where a mock/preview of a popup action is available.
     */
    data class PreviewPopupAction(
        override val popup: Popup,
        val metadata: Any,
    ) : PopupActionState, PopupActionData

    data class ThreadPopupAction(
        override val popup: Popup,
    ) : PopupActionState, PopupActionData

    /**
     * Represents shared data for popup actions.
     */
    sealed interface PopupActionData {
        /**
         * A map of variables associated with the popup action.
         */
        val popup: Popup
    }
}
