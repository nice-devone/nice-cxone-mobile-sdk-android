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

package com.nice.cxonechat

/**
 * Handler permitting to add new fields to the instance it was created from.
 *
 * The instance may or may not update values in the upstream object,
 * resulting in runtime behavior changes. These changes mainly regard to the
 * requests being modified by these fields.
 * */
@Public
interface ChatFieldHandler {

    /**
     * Adds specified [fields] to the instance. If requested on a thread that's
     * newly created, the fields may be lost until a first message is sent.
     *
     * The client should always ensure thread (if applicable) exists before making
     * changes to it. Threads are generally created by sending a first message
     * to it.
     */
    fun add(fields: Map<String, String>)
}
