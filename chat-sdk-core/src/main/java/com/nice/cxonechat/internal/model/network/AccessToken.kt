package com.nice.cxonechat.internal.model.network

import com.google.gson.annotations.SerializedName
import com.nice.cxonechat.util.plus
import java.util.Date
import kotlin.time.Duration.Companion.seconds

/**
 * An access token used by the customer for sending messages if OAuth authorization is on for the
 * channel.
 */
internal data class AccessToken(
    @SerializedName("token")
    val token: String,
    @SerializedName("expiresIn")
    private val expiresIn: Long,
) {

    private val createdAt = Date()
    val expiresAt = createdAt + expiresIn.seconds.inWholeMilliseconds

    /** Whether the token has expired or not. */
    val isExpired
        get() = Date().after(expiresAt)

}
