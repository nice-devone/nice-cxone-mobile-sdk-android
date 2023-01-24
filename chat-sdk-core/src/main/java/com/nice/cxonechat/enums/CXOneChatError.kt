package com.nice.cxonechat.enums

/**
 * The different types of errors that may be experienced.
 */
internal enum class CXOneChatError(val value: Exception) {
    // When calling any method

    /** An attempt was made to use a method without connecting first. Make sure you call the `connect` method first. */
    NotConnected(Exception("You are trying to call a method without connecting first. Make sure you call connect first.")),

    /** The conversion from object instance to data failed. */
    InvalidData(Exception("Data could not be converted.")),

    /** The provided ID for the thread was invalid, so the action could not be performed. */
    InvalidThread(Exception("No active thread.")),

    /** There aren't any other messages, so additional messages could not be loaded. */
    NoMoreMessages(Exception("There aren’t any other messages so additional messages could not be loaded.")),

    /** The provided attachment was unable to be sent. */
    AttachmentError(Exception("The provided attachment wasn't able to be sent.")),

    /** The case id was invalid and the operation is unable to be performed. */
    InvalidCaseId(Exception("Could not update customer contact field; need to send a message first to open channel.")),

    /** The server experienced an internal error and was unable to perform the action. */
    ServerError(Exception("Internal server error.")),

//    /** The case id was invalid and the operation is unable to be performed. */
//    missingChannelConfig(Exception("The configuration for the channel is not loaded and the operation could not be performed.")),

    // Errors when calling connect

    /** The WebSocket refused to connect. */
    WebSocketConnectionFailure(Exception("Something went wrong and the WebSocket refused to connect. If you are providing your own chatURL or socketURL, double check that these URLs are correct.")),

    /** The customer could not be authorized anonymously. */
    AnonymousAuthorizationFailure(Exception("Something went wrong and the customer could not be authorized.")),

    /** The customer could not be authorized using the OAuth details configured on the channel. */
    OAuthAuthorizationFailure(Exception("Something went wrong and the channel configuration could not be retrieved.")),

    /** The auth code has not been set, but an attempt has been made to authorize. */
    MissingAuthCode(Exception("You are trying to authorize a customer through OAuth, but haven’t provided the authorization code yet. Make sure you call setAuthCode before calling connect.")),

    /** The returning customer could not be reconnected. */
    ReconnectFailure(Exception("Something went wrong and the returning customer could not be reconnected.")),

    /** The customer was successfully authorized, but an access token wasn't returned. */
    MissingAccessToken(Exception("The customer was successfully authorized using OAuth, but an access token wasn’t returned.")),

    /** The customer was successfully authorized, but a customerId wasn't returned. */
    MissingCustomerId(Exception("The customer was successfully authorized using OAuth, but a customerId wasn’t returned.")),

    /** The customer could not be associated with a visitor. */
    CustomerVisitorAssociationFailure(Exception("The customer could not be successfully associated with a visitor.")),

    /** The request was invalid and couldn't be completed. */
    InvalidRequest(Exception("Could not make the request because the URL was malformed.")),

    InvalidCustomerId(Exception("The customer id is not valid.")),
    MissingContactId(Exception("Missing contact id.")),
    InvalidOldestDate(Exception("No oldest message date is saved."))
}
