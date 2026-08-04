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

import com.nice.cxonechat.exceptions.CXoneException
import com.nice.cxonechat.exceptions.InvalidCustomFieldValue
import com.nice.cxonechat.exceptions.MissingPreChatCustomFieldsException
import com.nice.cxonechat.exceptions.MissingThreadListFetchException
import com.nice.cxonechat.exceptions.UnsupportedChannelConfigException
import com.nice.cxonechat.prechat.PreChatSurvey
import com.nice.cxonechat.prechat.PreChatSurveyResponse
import com.nice.cxonechat.state.FieldDefinition
import com.nice.cxonechat.thread.ChatThread
import kotlinx.coroutines.flow.Flow

/**
 * Instance that allows modification, manipulation and observing of [threads][ChatThread].
 *
 * Some methods of this class depend on configuration sent from the server.
 * If the configuration changes remotely, you need to call [ChatBuilder.build] again
 * and use the newly created instance.
 * The saved configuration will never change at run-time.
 *
 * There are no effects attached to initialization of this class.
 */
@Public
interface ChatThreadsHandler {

    /**
     * A pre-chat form which should be answered by the user before [create] is called.
     * Answers have to be supplied as custom fields in the [create] call.
     */
    val preChatSurvey: PreChatSurvey?

    /**
     * A hot [Flow] that emits the current thread list. Replays the last value to new subscribers.
     *
     * Emits on:
     * - Server responses to thread-list/thread-recovery requests (triggered automatically on first collection
     *   and by [refresh]; the specific server event is mode-dependent)
     * - Local [create] calls (optimistically, before server confirmation)
     * - Real-time case-status changes from the server (multi-thread and live-chat modes only)
     * - Per-thread metadata updates (multi-thread mode only)
     *
     * Collect this flow before calling [create] in single-thread or live-chat configurations
     * to ensure proper thread-creation validation.
     *
     * @see [create]
     * @see [refresh]
     */
    val threadsFlow: Flow<List<ChatThread>>

    /**
     * Sends a request to refresh the thread list.
     *
     * This method is triggered automatically the first time [threadsFlow] is collected.
     * The owner can call [refresh] again to force an update — for example after a long
     * idle period when the user revisits the thread list.
     */
    fun refresh()

    /**
     * Creates a new thread -if permitted by the configuration- and returns a handler for it.
     * [threadsFlow] will emit this new instance even if it's not created on the server yet.
     *
     * Whenever the configuration doesn't permit creating new threads, this method throws
     * [CXoneException] in response.
     * In cases where the configuration permits only a single thread, the method
     * requires the client to first collect [threadsFlow].
     * This is to ensure proper validation of creating threads.
     * _Please note that you have to perform this action on every new [ChatThreadsHandler]
     * as it exclusively remembers its own state._
     *
     * The [ChatThreadsHandler] instance remembers at most one thread that contains no
     * messages (i.e. is not created on the server).
     *
     * @throws UnsupportedChannelConfigException when configuration doesn't permit creation
     * of additional threads.
     * This exception is never thrown for multi thread configurations.
     * @throws MissingThreadListFetchException when the configuration permits only a singular
     * thread and [threadsFlow] collection hasn't yet received the thread list.
     * This exception is never thrown for multi thread configurations.
     * @throws MissingPreChatCustomFieldsException when the configuration requires answers
     * for items in [preChatSurvey].
     * @throws InvalidCustomFieldValue in case of internal SDK error.
     *
     * @return A new instance of [ChatThreadHandler].
     * @see [threadsFlow]
     * @see [preChatSurvey]
     */
    @Throws(
        UnsupportedChannelConfigException::class,
        MissingThreadListFetchException::class,
        MissingPreChatCustomFieldsException::class,
        InvalidCustomFieldValue::class,
    )
    fun create(): ChatThreadHandler = create(customFields = emptyMap(), preChatSurveyResponse = emptySequence())

    /**
     * Creates a new thread -if permitted by configuration- and returns a handler for it.
     * [threadsFlow] will emit this new instance even if it's not created on the server yet.
     *
     * @param customFields An initial map of custom-field key-values specific to this new thread.
     * These custom-fields can be used for personalization during thread creation
     * (e.g.: for a welcome message) and will be sent with a first outbound message.
     * Possible source is from a pre-chat static survey.
     *
     * @throws UnsupportedChannelConfigException when configuration doesn't permit creation
     * of additional threads.
     * @throws MissingThreadListFetchException when the configuration permits only singular
     * thread and [threadsFlow] collection hasn't yet received the thread list.
     * @throws MissingPreChatCustomFieldsException when the configuration requires answers
     * for items in [preChatSurvey].
     * @throws InvalidCustomFieldValue if a value in [customFields] is invalid for any reason.
     *
     * @return A new instance of [ChatThreadHandler].
     * @see [threadsFlow]
     * @see [preChatSurvey]
     */
    @Throws(
        UnsupportedChannelConfigException::class,
        MissingThreadListFetchException::class,
        MissingPreChatCustomFieldsException::class,
        InvalidCustomFieldValue::class,
    )
    fun create(customFields: Map<String, String>): ChatThreadHandler = create(
        customFields = customFields,
        preChatSurveyResponse = emptySequence(),
    )

    /**
     * Creates a new thread -if permitted by configuration- and returns a handler for it.
     * [threadsFlow] will emit this new instance even if it's not created on the server yet.
     *
     * @param preChatSurveyResponse Iterable sequence of responses to items in [preChatSurvey].
     * The sequence has to contain responses to all items in [PreChatSurvey] which have the flag
     * [FieldDefinition.isRequired] set to `true`.
     *
     * @throws UnsupportedChannelConfigException when configuration doesn't permit creation
     * of additional threads.
     * @throws MissingThreadListFetchException when the configuration permits only singular
     * thread and [threadsFlow] collection hasn't yet received the thread list.
     * @throws MissingPreChatCustomFieldsException when the configuration requires answers
     * for items in [preChatSurvey] and those were not supplied, or supplied answer is not valid
     * (non-leaf [com.nice.cxonechat.state.HierarchyNode] for [FieldDefinition.Hierarchy]).
     * @throws InvalidCustomFieldValue in case of internal SDK error.
     *
     * @return A new instance of [ChatThreadHandler].
     * @see [threadsFlow]
     * @see [preChatSurvey]
     */
    @Throws(
        UnsupportedChannelConfigException::class,
        MissingThreadListFetchException::class,
        MissingPreChatCustomFieldsException::class,
        InvalidCustomFieldValue::class,
    )
    fun create(
        preChatSurveyResponse: Sequence<PreChatSurveyResponse<out FieldDefinition, out Any>>,
    ): ChatThreadHandler = create(
        customFields = emptyMap(),
        preChatSurveyResponse = preChatSurveyResponse,
    )

    /**
     * Creates a new thread -if permitted by configuration- and returns a handler for it.
     * [threadsFlow] will emit this new instance even if it's not created on the server yet.
     *
     * @param customFields An initial map of custom-field key-values specific to this new thread.
     * These custom-fields can be used for personalization during thread creation
     * (e.g.: for a welcome message) and will be sent with a first outbound message.
     * Possible source is from a pre-chat static survey.
     * @param preChatSurveyResponse Iterable sequence of responses to items in [preChatSurvey].
     * The sequence has to contain responses to all items in [PreChatSurvey] which have the flag
     * [FieldDefinition.isRequired] set to `true`.
     *
     * @throws UnsupportedChannelConfigException when configuration doesn't permit creation
     * of additional threads.
     * @throws MissingThreadListFetchException when the configuration permits only singular
     * thread and [threadsFlow] collection hasn't yet received the thread list.
     * @throws MissingPreChatCustomFieldsException when the configuration requires answers
     * for items in [preChatSurvey] and those were not supplied, or supplied answer is not valid.
     * (non-leaf [com.nice.cxonechat.state.HierarchyNode] for [FieldDefinition.Hierarchy]).
     * @throws InvalidCustomFieldValue if a value in [customFields] or [preChatSurveyResponse]
     * is invalid for any reason.
     *
     * @return A new instance of [ChatThreadHandler].
     * @see [threadsFlow]
     * @see [preChatSurvey]
     */
    @Throws(
        UnsupportedChannelConfigException::class,
        MissingThreadListFetchException::class,
        MissingPreChatCustomFieldsException::class,
        InvalidCustomFieldValue::class,
    )
    fun create(
        customFields: Map<String, String>,
        preChatSurveyResponse: Sequence<PreChatSurveyResponse<out FieldDefinition, out Any>>,
    ): ChatThreadHandler

    /**
     * Creates a new thread handler with given thread.
     * [thread] is used as a template and this particular instance will not be updated in response to
     * changes in the handler.
     *
     * **Live-chat side effect:** in live-chat configurations creating the handler arms automatic
     * welcome-message handling for the thread. This is intentional and eager: the welcome message is
     * delivered as the thread's first message as soon as possible — including for a freshly created
     * (pending) thread, and without waiting for the thread to be observed via
     * [ChatThreadHandler.threadFlow]. The side effect is one-shot per thread: the welcome-message
     * event is dispatched at most once for a given thread — re-creating a handler for the same thread
     * does not re-arm or re-send it. This welcome behaviour is specific to live-chat configurations.
     *
     * @param thread recovered from [threadsFlow]
     * @see ChatThreadHandler
     */
    fun thread(thread: ChatThread): ChatThreadHandler
}
