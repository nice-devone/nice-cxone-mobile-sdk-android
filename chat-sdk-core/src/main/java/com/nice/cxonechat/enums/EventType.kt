/*
 * Copyright (c) 2021-2024. NICE Ltd. All rights reserved.
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

package com.nice.cxonechat.enums

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The different types of WebSocket events.
 */
@Serializable
internal enum class EventType(val value: String) {
    /** An event sent to authorize a customer. */
    @SerialName("AuthorizeCustomer")
    AuthorizeCustomer("AuthorizeCustomer"),

    /** An event received when the customer has been successfully authorized. */
    @SerialName("ConsumerAuthorized")
    CustomerAuthorized("ConsumerAuthorized"),

    /** An event sent to reconnect a returning customer. */
    @SerialName("ReconnectConsumer")
    ReconnectCustomer("ReconnectConsumer"),

    /** An event received when the customer has been successfully reconnected. */
    @SerialName("ConsumerReconnected")
    CustomerReconnected("ConsumerReconnected"),

    /** An event sent to refresh an access token. */
    @SerialName("RefreshToken")
    RefreshToken("RefreshToken"),

    /** An event received when the token has been successfully refreshed. */
    @SerialName("TokenRefreshed")
    TokenRefreshed("TokenRefreshed"),

    @SerialName("EndContact")
    EndContact("EndContact"),

    // Message

    /** An event to send a message in a chat thread. */
    @SerialName("SendMessage")
    SendMessage("SendMessage"),

    /** An event received when a message has been received in a chat. */
    @SerialName("MessageCreated")
    MessageCreated("MessageCreated"),

    @SerialName("SendOutbound")
    SendOutbound("SendOutbound"),

    /** An event to send to load more messages in a chat thread. */
    @SerialName("LoadMoreMessages")
    LoadMoreMessages("LoadMoreMessages"),

    /** An event received when more messages have been received for the chat thread. */
    @SerialName("MoreMessagesLoaded")
    MoreMessagesLoaded("MoreMessagesLoaded"),

    /** An event to send to mark a chat message as seen by the customer. */
    @SerialName("MessageSeenByCustomer")
    MessageSeenByCustomer("MessageSeenByCustomer"),

    /** An event received when a message has been seen by an agent. */
    @SerialName("MessageSeenByUser")
    MessageSeenByAgent("MessageSeenByUser"),

    /** An event received when a read status of a message has been changed. */
    @SerialName("MessageReadChanged")
    MessageReadChanged("MessageReadChanged"),

    // Thread

    /** An event to send to recover an existing chat thread in a single-thread channel. */
    @SerialName("RecoverThread")
    RecoverThread("RecoverThread"),

    /** An event received when a chat thread has been recovered. */
    @SerialName("ThreadRecovered")
    ThreadRecovered("ThreadRecovered"),

    /** An event to send to fetch the list of chat threads for the customer in a multi-thread channel. */
    @SerialName("FetchThreadList")
    FetchThreadList("FetchThreadList"),

    /** An event received when a list of chat threads has been fetched. */
    @SerialName("ThreadListFetched")
    ThreadListFetched("ThreadListFetched"),

    /** An event to send to archive a chat thread in a multi-thread channel. */
    @SerialName("ArchiveThread")
    ArchiveThread("ArchiveThread"),

    /** An event received when a chat thread has been archived. */
    @SerialName("ThreadArchived")
    ThreadArchived("ThreadArchived"),

    /** An event to send to load metadata about a chat thread. This includes the most recent message in the thread. */
    @SerialName("LoadThreadMetadata")
    LoadThreadMetadata("LoadThreadMetadata"),

    /** An event received when metadata for a chat thread has been loaded. */
    @SerialName("ThreadMetadataLoaded")
    ThreadMetadataLoaded("ThreadMetadataLoaded"),

    /** An event sent to update the thread name and other info. */
    @SerialName("UpdateThread")
    UpdateThread("UpdateThread"),

    /** An event received when the thread has been updated. */
    @SerialName("ThreadUpdated")
    ThreadUpdated("ThreadUpdated"),

    /** Position in queue updated. */
    @SerialName("SetPositionInQueue")
    SetPositionInQueue("SetPositionInQueue"),

    // LiveChat

    /** Event which triggers livechat recover. **/
    @SerialName("RecoverLivechat")
    RecoverLivechat("RecoverLivechat"),

    /** An event received when a live-chat thread data has been recovered. */
    @SerialName("LivechatRecovered")
    LivechatRecovered("LivechatRecovered"),

    // Contact

    /** An event received when the assigned agent changes for a contact. */
    @SerialName("CaseInboxAssigneeChanged")
    CaseInboxAssigneeChanged("CaseInboxAssigneeChanged"),

    @SerialName("CaseCreated")
    CaseCreated("CaseCreated"), // TODO: Remove?

    @SerialName("CaseStatusChanged")
    CaseStatusChanged("CaseStatusChanged"),

    // Custom Fields

    /** An event to send to set custom field values for a contact (thread). */
    @SerialName("SetContactCustomFields")
    SetContactCustomFields("SetContactCustomFields"),

    /** An event to send to set custom field values for a customer. */
    @SerialName("SetCustomerCustomFields")
    SetCustomerCustomFields("SetCustomerCustomFields"),

    // Typing
    /** An event received when an agent or customer starts typing in a chat thread. */
    @SerialName("SenderTypingStarted")
    SenderTypingStarted("SenderTypingStarted"),

    /** An event received when an agent or customer stops typing in a chat thread. */
    @SerialName("SenderTypingEnded")
    SenderTypingEnded("SenderTypingEnded"),

    // Proactive Chat

    /** An event to send to execute an automation trigger manually. */
    @SerialName("ExecuteTrigger")
    ExecuteTrigger("ExecuteTrigger"),

    // Visitor

    @SerialName("StoreVisitorEvents")
    StoreVisitorEvents("StoreVisitorEvents"),

    @SerialName("SendPageViews")
    SendPageViews("SendPageViews"),

    @SerialName("FireProactiveAction")
    FireProactiveAction("FireProactiveAction"),

    // Meta Events

    /** A meta event sent when the actual event should be retrieved from s3. */
    @SerialName("EventInS3")
    EventInS3("EventInS3"),
}
