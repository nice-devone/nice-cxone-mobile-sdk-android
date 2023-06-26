package com.nice.cxonechat.internal.model

import com.google.gson.annotations.SerializedName
import com.nice.cxonechat.message.MessageAuthor
import java.util.UUID

internal data class CustomerIdentityModel(
    @SerializedName("idOnExternalPlatform")
    val idOnExternalPlatform: UUID,

    @SerializedName("firstName")
    val firstName: String? = null,

    @SerializedName("lastName")
    val lastName: String? = null,
) {

    fun toMessageAuthor(): MessageAuthor = MessageAuthorInternal(
        id = idOnExternalPlatform.toString(),
        firstName = firstName.orEmpty(),
        lastName = lastName.orEmpty()
    )
}
