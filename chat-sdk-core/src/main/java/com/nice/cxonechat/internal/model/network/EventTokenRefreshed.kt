package com.nice.cxonechat.internal.model.network

import com.google.gson.annotations.SerializedName

/** Event received when a token has been successfully refreshed. */
internal data class EventTokenRefreshed(
    @SerializedName("postback")
    val postback: Postback<Data>,
) {

    val token get() = postback.data.accessToken.token
    val expiresAt get() = postback.data.accessToken.expiresAt

    data class Data(
        @SerializedName("accessToken")
        val accessToken: AccessToken,
    )

}
