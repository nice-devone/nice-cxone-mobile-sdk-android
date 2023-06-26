package com.nice.cxonechat.internal.model

import com.google.gson.annotations.SerializedName
import com.nice.cxonechat.internal.model.network.CustomVariable
import com.nice.cxonechat.internal.model.network.DeviceFingerprint
import com.nice.cxonechat.internal.model.network.Journey

// Visitor

/**
 * All information about a visitor.
 */
internal data class Visitor constructor(
    @SerializedName("customerIdentity")
    val customerIdentity: CustomerIdentityModel? = null,
    @SerializedName("browserFingerprint")
    val deviceFingerprint: DeviceFingerprint,
    @SerializedName("journey")
    val journey: Journey? = null,
    @SerializedName("customVariables")
    val customVariables: List<CustomVariable>? = null,
) {

    constructor(
        deviceToken: String,
    ) : this(
        deviceFingerprint = DeviceFingerprint(deviceToken = deviceToken)
    )
}
