/*
 * Copyright (c) 2021-2026. NICE Ltd. All rights reserved.
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

package com.nice.cxonechat

import com.nice.cxonechat.message.ContentDescriptor
import com.nice.cxonechat.message.OutboundMessage

/**
 * Manages all the necessary procedures required for service messages.
 * This newly created object influences changes in parent [ChatThreadHandler] in number
 * of ways.
 * Though it does not modify the parent directly, it can force some
 * events to happen which in-turn update the data.
 */
@Public
interface ChatThreadMessageHandler {

    /**
     * Notifies server that we need to load more messages. This expects that
     * the instance provided by parent [ChatThreadHandler] has the most
     * up-to-date data. Be warned that calling this method on a thread that
     * has not been updated has undefined consequences. It can result, for
     * example, in message duplication.
     *
     * Please note that you should always call [ChatThreadHandler.get] with
     * a callback for this method to work. In any other case, this results in
     * undefined behavior.
     *
     * Call only if the client reaches the top (or bottom, depending on the view
     * you've chosen) of the loaded messages after having them loaded by
     * [ChatThreadHandler.get].
     *
     * If [ChatThreadHandler.get] returns repeatedly empty list of messages,
     * the thread has probably no more messages left to load, therefore
     * calling this method makes no sense as it will again update the thread
     * with an empty list.
     */
    fun loadMore()

    /**
     * Sends [message] to the server and suspends until the message has been dispatched.
     *
     * On success, returns the ID assigned to the sent message as a [String].
     * On failure, throws an exception (e.g., [com.nice.cxonechat.exceptions.InvalidParameterException]
     * for an empty message, or [com.nice.cxonechat.exceptions.CXoneException] for server-side errors).
     *
     * Note that messages with no text, attachments, or postback specified will
     * throw [com.nice.cxonechat.exceptions.InvalidParameterException].
     *
     * If you are supplying attachments for upload, then be aware of the following.
     *
     * * If attachment misses file name, the file is named to "document"
     * upon being sent to the server. Please take care to provide localized
     * file names if you want to display them to the user.
     *
     * * The upload of files is performed at most **once** before subsequent
     * processing of the message and sending it to the server. If the file
     * call succeeds, it's cached internally to avoid doubling uploads.
     * Therefore, subsequent calls (if the primary were to fail) are much
     * faster. Also, if the user tries to send the same attachment again,
     * it will not be uploaded again instead it's referenced by the origin
     * upload. This cache is active as long as the [ChatBuilder] instance
     * remains the same. Reinitializing the [Chat] doesn't clear the cache.
     *
     * * If attachments violate file restrictions (wrong type, too large, or uploads
     * disabled), the error is first reported via [Chat.stateFlow] as a
     * [ChatStateEvent.RuntimeException], and then this method throws
     * [com.nice.cxonechat.exceptions.RuntimeChatException.AttachmentUploadError].
     *
     * * If any upload of any attachment fails by connection error during HTTP upload,
     * the error is reported via [Chat.stateFlow] as a [ChatStateEvent.RuntimeException]
     * with an instance of [com.nice.cxonechat.exceptions.RuntimeChatException.AttachmentUploadError],
     * and the attachment is skipped.
     *
     * * If any upload of any attachment fails by server error (returns but an
     * empty body), then the attachment is skipped, and execution continues.
     *
     * @param message Message to be sent.
     * @return The ID assigned to the sent message.
     * @throws com.nice.cxonechat.exceptions.InvalidParameterException if the message is empty, ie., has no attachment,
     * message, or postback.
     * @throws com.nice.cxonechat.exceptions.RuntimeChatException.AttachmentUploadError if attachments violate
     * file restrictions (wrong type, too large, or uploads disabled).
     * @throws com.nice.cxonechat.exceptions.RuntimeChatException.ServerCommunicationError if the message could not be
     * enqueued to the WebSocket.
     * @throws IllegalStateException in case the [com.nice.cxonechat.thread.ChatThread.canAddMoreMessages] is false
     * and application tries to send a new message to such thread.
     */
    suspend fun send(message: OutboundMessage): String

    /**
     * Simplified method to send a simple user message to the agent.
     *
     * @param message message to send to agent.
     * @param postback optional "postback" to send with the message.
     * @return The ID assigned to the sent message.
     * @see send
     */
    suspend fun send(message: String, postback: String? = null): String =
        send(OutboundMessage(message, postback = postback))

    /**
     * Simplified method to send an attachment to the agent.
     *
     * @param attachments attachments to send.
     * @param message optional message to send with attachments.
     * @param postback optional "postback" to send with attachments.
     * @return The ID assigned to the sent message.
     * @see send
     */
    suspend fun send(
        attachments: Iterable<ContentDescriptor>,
        message: String = "",
        postback: String? = null,
    ): String = send(OutboundMessage(attachments, message, postback))
}
