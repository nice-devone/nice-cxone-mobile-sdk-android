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

package com.nice.cxonechat.prechat

import com.nice.cxonechat.Public
import com.nice.cxonechat.state.FieldDefinitionList

/**
 * A definition of the pre-chat form which should be answered by the user before the new thread is created.
 */
@Public
interface PreChatSurvey {

    /**
     * Name of the dynamic pre-chat survey.
     */
    val name: String

    /**
     * Sequence of pre-chat survey fields which should be presented to user before chat thread is created.
     */
    val fields: FieldDefinitionList
}
