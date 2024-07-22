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

package com.nice.cxonechat

import androidx.annotation.CheckResult
import com.nice.cxonechat.exceptions.CXOneException
import com.nice.cxonechat.exceptions.InvalidCustomFieldValue
import com.nice.cxonechat.exceptions.MissingPreChatCustomFieldsException
import com.nice.cxonechat.exceptions.MissingThreadListFetchException
import com.nice.cxonechat.exceptions.UndefinedCustomField
import com.nice.cxonechat.exceptions.UnsupportedChannelConfigException
import com.nice.cxonechat.prechat.PreChatSurvey
import com.nice.cxonechat.prechat.PreChatSurveyResponse
import com.nice.cxonechat.state.FieldDefinition
import com.nice.cxonechat.thread.ChatThread

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
     * Sends a request to refresh the thread-list.
     *
     * This method will be called once per [ChatThreadsHandler] creation. Subsequently,
     * the owner can call [refresh] again to update the threads list.  One possible use
     * might be to use it whenever a threads list is redisplayed after a long idle period.
     *
     * Client needs to register only one [OnThreadsUpdatedListener] per chat instance.
     * All subsequent [refresh] calls will notify listeners registered in this instance.
     */
    fun refresh()

    /**
     * Creates a new thread -if permitted by the configuration- and returns a handler for it.
     * [threads] should return this new instance even if it's not created on the server
     * yet.
     *
     * Whenever the configuration doesn't permit creating new threads, this method throws
     * [CXOneException] in response.
     * In cases where the configuration permits only a single thread, the method
     * requires the client to first call [threads] with a listener.
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
     * thread and [threads] method wasn't called before call of this method, or the thread list
     * fetch didn't yet complete.
     * This exception is never thrown for multi thread configurations.
     * @throws MissingPreChatCustomFieldsException when the configuration requires answers
     * for items in [preChatSurvey].
     * @throws InvalidCustomFieldValue in case of internal SDK error.
     * @throws UndefinedCustomField in case of internal SDK error.
     *
     * @return A new instance of [ChatThreadsHandler].
     * @see [threads]
     * @see [preChatSurvey]
     */
    @Throws(
        UnsupportedChannelConfigException::class,
        MissingThreadListFetchException::class,
        MissingPreChatCustomFieldsException::class,
        InvalidCustomFieldValue::class,
        UndefinedCustomField::class,
    )
    fun create(): ChatThreadHandler = create(customFields = emptyMap(), preChatSurveyResponse = emptySequence())

    /**
     * Creates a new thread -if permitted by configuration- and returns a handler for it.
     * [threads] should return this new instance even if it's not created on the server
     * yet.
     *
     * Whenever configuration doesn't permit creating new threads, this method throws
     * [CXOneException] in response.
     * In cases where configuration permits only singular thread, the method
     * requires the client to first call [threads] with a listener.
     * This is to ensure proper validation of creating threads.
     * _Please note that you have to perform this action on every new [ChatThreadsHandler]
     * as it exclusively remembers its own state._
     *
     * The [ChatThreadsHandler] instance remembers at most one thread that contains no
     * messages (i.e. is not created on the server).
     *
     * @param customFields An initial map of custom-field key-values specific to this new thread.
     * These custom-fields can be used for personalization during thread creation
     * (e.g.: for a welcome message) and will be sent with a first outbound message.
     * Possible source is from a pre-chat static survey.
     *
     * @throws UnsupportedChannelConfigException when configuration doesn't permit creation
     * of additional threads.
     * This exception is never thrown for multi thread configurations.
     * @throws MissingThreadListFetchException when the configuration permits only singular
     * thread and [threads] method wasn't called before call of this method, or the thread list
     * fetch didn't yet complete.
     * This exception is never thrown for multi thread configurations.
     * @throws MissingPreChatCustomFieldsException when the configuration requires answers
     * for items in [preChatSurvey].
     * @throws InvalidCustomFieldValue if a value in [customFields] is invalid for any reason.
     * @throws UndefinedCustomField if a key in [customFields] is not defined by the
     * channel configuration.
     *
     * @return A new instance of [ChatThreadsHandler].
     * @see [threads]
     * @see [preChatSurvey]
     */
    @Throws(
        UnsupportedChannelConfigException::class,
        MissingThreadListFetchException::class,
        MissingPreChatCustomFieldsException::class,
        InvalidCustomFieldValue::class,
        UndefinedCustomField::class,
    )
    fun create(customFields: Map<String, String>): ChatThreadHandler = create(
        customFields = customFields,
        preChatSurveyResponse = emptySequence(),
    )

    /**
     * Creates a new thread -if permitted by configuration- and returns a handler for it.
     * [threads] should return this new instance even if it's not created on the server
     * yet.
     *
     * Whenever configuration doesn't permit creating new threads, this method throws
     * [CXOneException] in response.
     * In cases where configuration permits only singular thread, the method
     * requires the client to first call [threads] with a listener.
     * This is to ensure proper validation of creating threads.
     * _Please note that you have to perform this action on every new [ChatThreadsHandler]
     * as it exclusively remembers its own state._
     *
     * The [ChatThreadsHandler] instance remembers at most one thread that contains no
     * messages (i.e. is not created on the server).
     *
     * @param preChatSurveyResponse Iterable sequence of responses to items in [preChatSurvey].
     * The sequence has to contain responses to all items in [PreChatSurvey] which have the flag
     * [FieldDefinition.isRequired] set to `true`.
     *
     * @throws UnsupportedChannelConfigException when configuration doesn't permit creation
     * of additional threads.
     * This exception is never thrown for multi thread configurations.
     * @throws MissingThreadListFetchException when the configuration permits only singular
     * thread and [threads] method wasn't called before call of this method, or the thread list
     * fetch didn't yet complete.
     * This exception is never thrown for multi thread configurations.
     * @throws MissingPreChatCustomFieldsException when the configuration requires answers
     * for items in [preChatSurvey] and those were not supplied, or supplied answer is not valid
     * (non-leaf [com.nice.cxonechat.state.HierarchyNode] for [FieldDefinition.Hierarchy]).
     * @throws InvalidCustomFieldValue in case of internal SDK error.
     * @throws UndefinedCustomField in case of internal SDK error.
     *
     * @return A new instance of [ChatThreadsHandler].
     * @see [threads]
     * @see [preChatSurvey]
     */
    @Throws(
        UnsupportedChannelConfigException::class,
        MissingThreadListFetchException::class,
        MissingPreChatCustomFieldsException::class,
        InvalidCustomFieldValue::class,
        UndefinedCustomField::class
    )
    fun create(
        preChatSurveyResponse: Sequence<PreChatSurveyResponse<out FieldDefinition, out Any>>,
    ): ChatThreadHandler = create(
        customFields = emptyMap(),
        preChatSurveyResponse = preChatSurveyResponse,
    )

    /**
     * Creates a new thread -if permitted by configuration- and returns a handler for it.
     * [threads] should return this new instance even if it's not created on the server
     * yet.
     *
     * Whenever configuration doesn't permit creating new threads, this method throws
     * [CXOneException] in response.
     * In cases where configuration permits only singular thread, the method
     * requires the client to first call [threads] with a listener.
     * This is to ensure proper validation of creating threads.
     * _Please note that you have to perform this action on every new [ChatThreadsHandler]
     * as it exclusively remembers its own state._
     *
     * The [ChatThreadsHandler] instance remembers at most one thread that contains no
     * messages (i.e. is not created on the server).
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
     * This exception is never thrown for multi thread configurations.
     * @throws MissingThreadListFetchException when the configuration permits only singular
     * thread and [threads] method wasn't called before call of this method, or the thread list
     * fetch didn't yet complete.
     * This exception is never thrown for multi thread configurations.
     * @throws MissingPreChatCustomFieldsException when the configuration requires answers
     * for items in [preChatSurvey] and those were not supplied, or supplied answer is not valid.
     * (non-leaf [com.nice.cxonechat.state.HierarchyNode] for [FieldDefinition.Hierarchy]).
     * @throws InvalidCustomFieldValue if a value in [customFields] or [preChatSurveyResponse]
     * is invalid for any reason.
     * @throws UndefinedCustomField if a key in [customFields] is not defined by the
     * channel configuration.
     *
     * @return A new instance of [ChatThreadsHandler].
     * @see [threads]
     * @see [preChatSurvey]
     */
    @Throws(
        UnsupportedChannelConfigException::class,
        MissingThreadListFetchException::class,
        MissingPreChatCustomFieldsException::class,
        InvalidCustomFieldValue::class,
        UndefinedCustomField::class,
    )
    fun create(
        customFields: Map<String, String>,
        preChatSurveyResponse: Sequence<PreChatSurveyResponse<out FieldDefinition, out Any>>,
    ): ChatThreadHandler

    /**
     * Registers a listeners on this instance that returns new value every time client
     * calls [refresh] or server imperatively forces a refresh remotely.
     *
     * It has side effects attached to it, see referenced methods.
     * @see [create]
     * @see [refresh]
     */
    @CheckResult
    fun threads(listener: OnThreadsUpdatedListener): Cancellable

    /**
     * Creates a new thread handler with given thread.
     * [thread] is used as a template and this particular instance will not be updated in response to
     * changes in the handler.
     *
     * @param thread recovered from [threads] callback
     * @see ChatThreadHandler
     * */
    fun thread(thread: ChatThread): ChatThreadHandler

    /**
     * Listener which allows the user to subscribe to thread list changes.
     */
    @Public
    fun interface OnThreadsUpdatedListener {
        /**
         * Notifies the listener about changes to the thread list.
         */
        fun onThreadsUpdated(threads: List<ChatThread>)
    }
}
