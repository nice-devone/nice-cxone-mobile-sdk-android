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

import com.nice.cxonechat.ChatEventHandler.OnEventErrorListener
import com.nice.cxonechat.ChatEventHandler.OnEventSentListener
import com.nice.cxonechat.analytics.ActionMetadata
import com.nice.cxonechat.event.ChatWindowOpenEvent
import com.nice.cxonechat.event.ConversionEvent
import com.nice.cxonechat.event.CustomVisitorEvent
import com.nice.cxonechat.event.PageViewEndedEvent
import com.nice.cxonechat.event.PageViewEvent
import com.nice.cxonechat.event.ProactiveActionClickEvent
import com.nice.cxonechat.event.ProactiveActionDisplayEvent
import com.nice.cxonechat.event.ProactiveActionFailureEvent
import com.nice.cxonechat.event.ProactiveActionSuccessEvent
import com.nice.cxonechat.event.RefreshToken
import com.nice.cxonechat.event.TriggerEvent
import com.nice.cxonechat.internal.model.network.ProactiveActionInfo
import java.util.Date
import java.util.UUID

/**
 * Provides in-one-place interactions to trigger all available events.
 * @see ChatEventHandler
 */
@Public
@Suppress("TooManyFunctions")
object ChatEventHandlerActions {

    /**
     * Send a ChatWindowOpen event to the analytics server.
     *
     * `chatWindowOpen` should be invoked whenever the user opens the chat
     * window.
     *
     * @param date date of the event.
     * @param listener an optional listener to be notified after the event has been sent.
     * @param errorListener an optional error listener to be notified about errors encountered when event is handled.
     * @see ChatEventHandler.trigger
     */
    @JvmOverloads
    @JvmStatic
    @Public
    fun ChatEventHandler.chatWindowOpen(
        date: Date = Date(),
        listener: OnEventSentListener? = null,
        errorListener: OnEventErrorListener? = null,
    ) = trigger(ChatWindowOpenEvent(date), listener, errorListener)

    /**
     * Send a conversion event to the analytics server.
     *
     * `conversion` should be invoked whenever a "conversion" has occurred.
     * The precise definition of "conversion" is left to the implementer but
     * generally speaking corresponds to a completion type event such as a
     * sale or subscription.
     *
     * @param type application-specific string reflecting the type of conversion.  Examples
     * of appropriate strings might be "sale" or "subscription".
     * @param value application-specific value of the conversion.  Typically this will
     * be the sale or subscription price.
     * @param date date of the conversion event.
     * @param listener an optional listener to be notified after the event has been sent.
     * @param errorListener an optional error listener to be notified about errors encountered when event is handled.
     * @see ChatEventHandler.trigger
     */
    @JvmOverloads
    @JvmStatic
    @Public
    fun ChatEventHandler.conversion(
        type: String,
        value: Number,
        date: Date = Date(),
        listener: OnEventSentListener? = null,
        errorListener: OnEventErrorListener? = null,
    ) = trigger(ConversionEvent(type, value, date), listener, errorListener)

    /**
     * Send an arbitrary custom analytics event to the analytics server.
     *
     * The implementer is free to determine both the contents and usage of the custom
     * visitor event.
     *
     * @param data data to be sent in the custom event.  Must be json encodable.
     * @param listener an optional listener to be notified after the event has been sent.
     * @param errorListener an optional error listener to be notified about errors encountered when event is handled.
     * @see ChatEventHandler.trigger
     */
    @JvmOverloads
    @JvmStatic
    @Public
    fun ChatEventHandler.customVisitor(
        data: Any,
        listener: OnEventSentListener? = null,
        errorListener: OnEventErrorListener? = null,
    ) = trigger(CustomVisitorEvent(data), listener, errorListener)

    /**
     * send a page viewed event to the server.
     *
     * onPageView and onPageViewEnded should be invoked from each page (i.e., fragment
     * or activity) in your application, as in:
     *
     *     OnLifecycleEvent { _, event ->
     *         when (event) {
     *             ON_RESUME -> viewModel.onPageView(pageTitle, pageUrl)
     *             ON_PAUSE -> viewModel.onPageViewEnded(pageTitle, pageUrl)
     *             else -> Ignored
     *         }
     *     }
     *
     * @param title application-defined "title" string uniquely identifying the
     * page viewed.  Examples might include "category?cellphones" or "details?item=4568".
     * @param uri application-defined "uri" uniquely identifying the page viewed.
     * The `uri` **must** be a valid uri, although a relative URI is allowed.
     * Examples might include "com.nice.cxonechat.sample://category/cellphones" or
     * "/details/4568".
     * @param date date of the event.
     * @param listener an optional listener to be notified after the event has been sent.
     * @param errorListener an optional error listener to be notified about errors encountered when event is handled.
     * @see ChatEventHandler.trigger
     */
    @JvmOverloads
    @JvmStatic
    @Public
    fun ChatEventHandler.pageView(
        title: String,
        uri: String,
        date: Date = Date(),
        listener: OnEventSentListener? = null,
        errorListener: OnEventErrorListener? = null,
    ) = trigger(PageViewEvent(title, uri, date), listener, errorListener)

    /**
     * send a page view ended event to the server when a previously viewed page
     * is left.
     *
     * onPageView and onPageViewEnded should be invoked from each page (i.e., fragment
     * or activity) in your application.
     *
     * @param title application-defined "title" string uniquely identifying the
     * page viewed.  Examples might include "category?cellphones" or "details?item=4568".
     * @param uri application-defined "uri" uniquely identifying the page viewed.
     * The `uri` **must** be a valid uri, although a relative URI is allowed.
     * Examples might include "com.nice.cxonechat.sample://category/cellphones" or
     * "/details/4568".
     * @param date date of the event.
     * @param listener an optional listener to be notified after the event has been sent.
     * @param errorListener an optional error listener to be notified about errors encountered when event is handled.
     * @see ChatEventHandler.trigger
     */
    @JvmOverloads
    @JvmStatic
    @Public
    fun ChatEventHandler.pageViewEnded(
        title: String,
        uri: String,
        date: Date = Date(),
        listener: OnEventSentListener? = null,
        errorListener: OnEventErrorListener? = null,
    ) = trigger(PageViewEndedEvent(title, uri, date), listener, errorListener)

    /**
     * Send a proactive action click event to the analytics.
     *
     * [proactiveActionClick] **should** be invoked when the user clicks or taps
     * on a proactive action offered via [ChatActionHandler.onPopup] listener.
     *
     * @param data [ActionMetadata] provided in [ChatActionHandler.onPopup] listener.
     * @param date date of the event.
     * @param listener an optional listener to be notified after the event has been sent.
     * @param errorListener an optional error listener to be notified about errors encountered when event is handled.
     * @see ChatEventHandler.trigger
     */
    @JvmOverloads
    @JvmStatic
    @Public
    fun ChatEventHandler.proactiveActionClick(
        data: ActionMetadata,
        date: Date = Date(),
        listener: OnEventSentListener? = null,
        errorListener: OnEventErrorListener? = null,
    ) = trigger(ProactiveActionClickEvent(data, date), listener, errorListener)

    /**
     * Send a proactive action display event to the analytics.
     *
     * [proactiveActionClick] **should** be invoked when a proactive action offered
     * via [ChatActionHandler.onPopup] listener is displayed to the user.
     *
     * @param data [ActionMetadata] provided in [ChatActionHandler.onPopup] listener.
     * @param date date of the event.
     * @param listener an optional listener to be notified after the event has been sent.
     * @param errorListener an optional error listener to be notified about errors encountered when event is handled.
     * @see ChatEventHandler.trigger
     */
    @JvmOverloads
    @JvmStatic
    @Public
    fun ChatEventHandler.proactiveActionDisplay(
        data: ActionMetadata,
        date: Date = Date(),
        listener: OnEventSentListener? = null,
        errorListener: OnEventErrorListener? = null,
    ) = trigger(ProactiveActionDisplayEvent(data, date), listener, errorListener)

    /**
     * Send a proactive action failure event to the analytics.
     *
     * [proactiveActionClick] **should** be invoked when the user declines
     * a proactive action offered via [ChatActionHandler.onPopup] listener
     * either by ignoring it or by explicitly declining it by, e.g. tapping
     * a close box.
     *
     * @param data [ActionMetadata] provided in [ChatActionHandler.onPopup] listener.
     * @param date date of the event.
     * @param listener an optional listener to be notified after the event has been sent.
     * @param errorListener an optional error listener to be notified about errors encountered when event is handled.
     * @see ChatEventHandler.trigger
     */
    @JvmOverloads
    @JvmStatic
    @Public
    fun ChatEventHandler.proactiveActionFailure(
        data: ActionMetadata,
        date: Date = Date(),
        listener: OnEventSentListener? = null,
        errorListener: OnEventErrorListener? = null,
    ) = trigger(ProactiveActionFailureEvent(data, date), listener, errorListener)

    /**
     * Send a proactive action success event to the analytics.
     *
     * [proactiveActionClick] **should** be invoked when the user accepts
     * a proactive action offered via [ChatActionHandler.onPopup] listener
     * either by clicking or tapping it.
     *
     * @param data [ActionMetadata] provided in [ChatActionHandler.onPopup] listener.
     * @param date date of the event.
     * @param listener an optional listener to be notified after the event has been sent.
     * @param errorListener an optional error listener to be notified about errors encountered when event is handled.
     * @see ChatEventHandler.trigger
     */
    @JvmOverloads
    @JvmStatic
    @Public
    fun ChatEventHandler.proactiveActionSuccess(
        data: ActionMetadata,
        date: Date = Date(),
        listener: OnEventSentListener? = null,
        errorListener: OnEventErrorListener? = null,
    ) = trigger(ProactiveActionSuccessEvent(ProactiveActionInfo(data), date), listener, errorListener)

    /**
     * Refresh the authentication token associated with the chat.
     *
     * @param listener an optional listener to be notified after the event has been sent.
     * @param errorListener an optional error listener to be notified about errors encountered when event is handled.
     * @see ChatEventHandler.trigger
     */
    @JvmOverloads
    @JvmStatic
    fun ChatEventHandler.refresh(
        listener: OnEventSentListener? = null,
        errorListener: OnEventErrorListener? = null,
    ) = trigger(RefreshToken, listener, errorListener)

    /**
     * Trigger event specified in agent console or elsewhere as per your
     * representative instructions. This event is not mandatory though, consult your
     * representative for more information.
     *
     * @param id ID of event to trigger per representative instructions.
     * @param listener an optional listener to be notified after the event has been sent.
     * @param errorListener an optional error listener to be notified about errors encountered when event is handled.
     * @see ChatEventHandler.trigger
     */
    @JvmOverloads
    @JvmStatic
    fun ChatEventHandler.event(
        id: UUID,
        listener: OnEventSentListener? = null,
        errorListener: OnEventErrorListener? = null,
    ) = trigger(TriggerEvent(id), listener, errorListener)
}
