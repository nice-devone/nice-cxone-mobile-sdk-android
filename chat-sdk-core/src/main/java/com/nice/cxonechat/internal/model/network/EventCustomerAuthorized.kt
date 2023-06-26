package com.nice.cxonechat.internal.model.network

import com.google.gson.annotations.SerializedName
import com.nice.cxonechat.internal.model.CustomerIdentityModel

/** Event received when a customer is successfully authorized. */
internal data class EventCustomerAuthorized(
    @SerializedName("postback")
    val postback: Postback<Data>,
) {

    val id get() = postback.data.consumerIdentity.idOnExternalPlatform
    val firstName get() = postback.data.consumerIdentity.firstName
    val lastName get() = postback.data.consumerIdentity.lastName

    val token get() = postback.data.accessToken?.token
    val tokenExpiresAt get() = postback.data.accessToken?.expiresAt

    data class Data(
        @SerializedName("consumerIdentity")
        val consumerIdentity: CustomerIdentityModel,
        @SerializedName("accessToken")
        val accessToken: AccessToken?,
    )
}
