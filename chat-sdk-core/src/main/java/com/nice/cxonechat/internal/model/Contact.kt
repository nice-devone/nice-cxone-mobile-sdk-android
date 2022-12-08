package com.nice.cxonechat.internal.model

import com.google.gson.annotations.SerializedName
import com.nice.cxonechat.enums.ContactStatus
import java.util.Date
import java.util.UUID

// ContactView

/**
 * Represents all info about a contact (case).
 */

internal data class Contact constructor(
    /** The id of the contact. */
    @SerializedName("id")
    val id: String,

    /** The id of the thread for which this contact applies. */
    @SerializedName("threadIdOnExternalPlatform")
    val threadIdOnExternalPlatform: UUID,

    @SerializedName("status")
    val status: ContactStatus,

    @SerializedName("createdAt")
    val createdAt: Date,
)
