/*
 * Copyright (c) 2021-2023. NICE Ltd. All rights reserved.
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

package com.nice.cxonechat.exceptions

import com.nice.cxonechat.Public

/**
 * Wrapping class for exceptions thrown during SDK operation, which are caused by incorrect usage, or possible
 * internal errors.
 */
@Public
sealed class CXOneException : Exception {
    constructor(message: String?) : super(message)

    @Suppress(
        "UNUSED" // Reserved for future usage
    )
    constructor(message: String?, cause: Throwable?) : super(message, cause)

    @Suppress(
        "UNUSED" // Reserved for future usage
    )
    constructor(cause: Throwable?) : super(cause)

    internal companion object {
        @Suppress(
            "UndocumentedPublicProperty"
        )
        const val serialVersionUID = -7_049_214_473_807_003_049L
    }
}

/**
 * The method being called is not supported with the current channel configuration.
 */
@Public
class UnsupportedChannelConfigException internal constructor() : CXOneException(
    "The method you are trying to call is not supported with your current channel configuration." +
    " For example, archiving a thread is only supported on a channel configured for multiple threads."
)

/**
 * Thread creation requires a current list of threads to be fetched in order to perform check that only one thread is created for
 * a single-thread configuration.
 */
@Public
class MissingThreadListFetchException internal constructor() : CXOneException(
    "Your current channel configuration requires that, you need to call threads {} method first to fetch list of threads."
)

/**
 * The channel configuration requires that, before creation of a chat thread,
 * user fills out pre-chat survey and their answers have to be supplied as custom field parameters during thread creation.
 *
 * @property missing The required field labels that are missing.
 */
@Public
class MissingPreChatCustomFieldsException internal constructor(
    val missing: Iterable<String>,
) : CXOneException(
    "The method you are trying to call requires that you supply all mandatory custom fields from pre-chat survey." +
            " Missing custom fields:\n ${missing.joinToString()}"
)

/**
 * A custom value passed to [com.nice.cxonechat.ChatFieldHandler.add] or
 * [com.nice.cxonechat.ChatThreadsHandler.create] has an invalid format.
 */
@Public
class InvalidCustomFieldValue internal constructor(
    label: String,
    error: String
) : CXOneException(
    "An invalid value was specified for a custom field '$label': $error"
)

/**
 * A custom value passed to [ChatFieldHandler.add] or [ChatThreadsHandler.create] for an invalid (ie., missing) field.
 */
@Public
class UndefinedCustomField internal constructor(
    fieldId: String
) : CXOneException(
    "A custom value was passed for an undefined custom field: '$fieldId'"
)

/**
 * Backend has returned invalid `customerId` value causing invalid internal SDK state.
 *
 * Troubleshooting: (Assuming development mode)
 * Gather application logs with tag `CXOneChat` and contact CXone support representative.
 */
@Public
class MissingCustomerId internal constructor() : CXOneException(
    "The customer was successfully authorized, but an invalid customerId was returned."
)

/**
 * An "impossible" error has occurred.
 *
 * Troubleshooting: Report to CXone support.
 */
@Public
class InternalError internal constructor(message: String) : CXOneException(message)
