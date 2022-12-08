package com.nice.cxonechat.internal.model.network

import com.google.gson.annotations.SerializedName
import com.nice.cxonechat.internal.model.AgentModel
import com.nice.cxonechat.internal.model.Brand
import com.nice.cxonechat.internal.model.ChannelIdentifier
import com.nice.cxonechat.internal.model.Contact

internal data class EventContactInboxAssigneeChanged(
    @SerializedName("data")
    val data: Data,
) {

    val agent get() = data.inboxAssignee.toAgent()
    val formerAgent get() = data.previousInboxAssignee?.toAgent()

    data class Data(
        @SerializedName("brand")
        val brand: Brand,
        @SerializedName("channel")
        val channel: ChannelIdentifier,
        @SerializedName("case")
        val case: Contact,
        @SerializedName("inboxAssignee")
        val inboxAssignee: AgentModel,
        @SerializedName("previousInboxAssignee")
        val previousInboxAssignee: AgentModel?,
    )

}
