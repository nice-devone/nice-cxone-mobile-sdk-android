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
        const val serialVersionUID = -7049214473807003049L
    }
}

/**
 * The method being called is not supported with the current channel configuration.
 */
@Public
class UnsupportedChannelConfigException internal constructor() : CXOneException(
    "The method you are trying to call is not supported with your current channel configuration. For example, archiving a thread is only supported on a channel configured for multiple threads."
)

/**
 * Thread creation requires a current list of threads to be fetched in order to perform check that only one thread is created for a single-thread configuration.
 */
@Public
class MissingThreadListFetchException internal constructor() : CXOneException(
    "Your current channel configuration requires that, you need to call threads {} method first to fetch list of threads."
)
