/*
 * Copyright (c) 2021-2023. NICE Ltd. All rights reserved.
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

/**
 * The different types of errors that may be experienced.
 */
internal enum class CXOneChatError(val value: Exception) {
    // When calling any method

    /** An attempt was made to use a method without connecting first. Make sure you call the `connect` method first. */
    NotConnected(
        Exception("You are trying to call a method without connecting first. Make sure you call connect first.")
    ),

    /** The provided attachment was unable to be sent. */
    AttachmentError(Exception("The provided attachment wasn't able to be sent.")),

    /** The server experienced an internal error and was unable to perform the action. */
    ServerError(Exception("Internal server error.")),

    // Errors when calling connect

    /** The WebSocket refused to connect. */
    WebSocketConnectionFailure(
        Exception(
            "Something went wrong and the WebSocket refused to connect. If you are providing your own chatURL or" +
                " socketURL, double check that these URLs are correct."
        )
    ),

    /** The customer could not be authorized anonymously. */
    AnonymousAuthorizationFailure(Exception("Something went wrong and the customer could not be authorized.")),

    /** The customer could not be authorized using the OAuth details configured on the channel. */
    OAuthAuthorizationFailure(Exception("Something went wrong and the channel configuration could not be retrieved.")),

    /** The auth code has not been set, but an attempt has been made to authorize. */
    MissingAuthCode(
        Exception(
            "You are trying to authorize a customer through OAuth, but haven’t provided the authorization code yet." +
                " Make sure you call setAuthCode before calling connect."
        )
    ),

    /** The returning customer could not be reconnected. */
    ReconnectFailure(Exception("Something went wrong and the returning customer could not be reconnected.")),

    /** The customer was successfully authorized, but an access token wasn't returned. */
    MissingAccessToken(
        Exception("The customer was successfully authorized using OAuth, but an access token wasn’t returned.")
    ),

    /** The customer could not be associated with a visitor. */
    CustomerVisitorAssociationFailure(Exception("The customer could not be successfully associated with a visitor.")),

    /** The request was invalid and couldn't be completed. */
    InvalidRequest(Exception("Could not make the request because the URL was malformed.")),

    InvalidOldestDate(Exception("No oldest message date is saved."))
}
