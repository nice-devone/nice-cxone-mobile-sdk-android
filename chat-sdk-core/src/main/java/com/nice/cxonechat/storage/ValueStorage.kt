package com.nice.cxonechat.storage

import java.util.Date
import java.util.UUID

/**
 * Abstraction for classes providing persistent storage of provided variables.
 */
internal interface ValueStorage {
    /**
     * OAuth authorization token.
     * Default value is `null`.
     */
    var authToken: String?

    /**
     * Expiration date of [authToken], before which it should be refreshed.
     * Default value is null.
     */
    var authTokenExpDate: Date?

    /**
     * User application specific id.
     * Value is not writeable and remains the same for the time the app is
     * installed.
     */
    val visitorId: UUID

    /**
     * Authorized user id.
     * Default value is null.
     */
    var customerId: UUID?

    /**
     * Connection session id.
     * Value is not writeable and remains the same for the duration of
     * session on this device.
     */
    val destinationId: UUID

    /**
     * Personalized welcome message.
     * Default value is empty string.
     */
    var welcomeMessage: String

    fun clearStorage()
}
