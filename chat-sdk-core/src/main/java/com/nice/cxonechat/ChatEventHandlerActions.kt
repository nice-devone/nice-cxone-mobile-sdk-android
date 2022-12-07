package com.nice.cxonechat

import com.nice.cxonechat.ChatEventHandler.OnEventSentListener
import com.nice.cxonechat.analytics.ActionMetadata
import com.nice.cxonechat.event.ChatWindowOpenEvent
import com.nice.cxonechat.event.ConversionEvent
import com.nice.cxonechat.event.CustomVisitorEvent
import com.nice.cxonechat.event.PageViewEvent
import com.nice.cxonechat.event.ProactiveActionClickEvent
import com.nice.cxonechat.event.ProactiveActionDisplayEvent
import com.nice.cxonechat.event.ProactiveActionFailureEvent
import com.nice.cxonechat.event.ProactiveActionSuccessEvent
import com.nice.cxonechat.event.RefreshToken
import com.nice.cxonechat.event.TriggerEvent
import com.nice.cxonechat.event.VisitEvent
import java.util.UUID

/**
 * Provides in-one-place interactions to trigger all available events.
 * @see ChatEventHandler
 * */
@Public
object ChatEventHandlerActions {

    /**
     * @see ChatEventHandler.trigger
     * @see ChatWindowOpenEvent
     * */
    @JvmOverloads
    @JvmStatic
    fun ChatEventHandler.chatWindowOpen(
        listener: OnEventSentListener? = null,
    ) = trigger(ChatWindowOpenEvent, listener)

    /**
     * @see ChatEventHandler.trigger
     * @see ConversionEvent
     * */
    @JvmOverloads
    @JvmStatic
    fun ChatEventHandler.conversion(
        type: String,
        value: Number,
        listener: OnEventSentListener? = null,
    ) = trigger(ConversionEvent(type, value), listener)

    /**
     * @see ChatEventHandler.trigger
     * @see CustomVisitorEvent
     * */
    @JvmOverloads
    @JvmStatic
    fun ChatEventHandler.customVisitor(
        data: Any,
        listener: OnEventSentListener? = null,
    ) = trigger(CustomVisitorEvent(data), listener)

    /**
     * @see ChatEventHandler.trigger
     * @see PageViewEvent
     * */
    @JvmOverloads
    @JvmStatic
    fun ChatEventHandler.pageView(
        title: String,
        uri: String,
        listener: OnEventSentListener? = null,
    ) = trigger(PageViewEvent(title, uri), listener)

    /**
     * @see ChatEventHandler.trigger
     * @see ProactiveActionClickEvent
     * */
    @JvmOverloads
    @JvmStatic
    fun ChatEventHandler.proactiveActionClick(
        data: ActionMetadata,
        listener: OnEventSentListener? = null,
    ) = trigger(ProactiveActionClickEvent(data), listener)

    /**
     * @see ChatEventHandler.trigger
     * @see ProactiveActionDisplayEvent
     * */
    @JvmOverloads
    @JvmStatic
    fun ChatEventHandler.proactiveActionDisplay(
        data: ActionMetadata,
        listener: OnEventSentListener? = null,
    ) = trigger(ProactiveActionDisplayEvent(data), listener)

    /**
     * @see ChatEventHandler.trigger
     * @see ProactiveActionFailureEvent
     * */
    @JvmOverloads
    @JvmStatic
    fun ChatEventHandler.proactiveActionFailure(
        data: ActionMetadata,
        listener: OnEventSentListener? = null,
    ) = trigger(ProactiveActionFailureEvent(data), listener)

    /**
     * @see ChatEventHandler.trigger
     * @see ProactiveActionSuccessEvent
     * */
    @JvmOverloads
    @JvmStatic
    fun ChatEventHandler.proactiveActionSuccess(
        data: ActionMetadata,
        listener: OnEventSentListener? = null,
    ) = trigger(ProactiveActionSuccessEvent(data), listener)

    /**
     * @see ChatEventHandler.trigger
     * @see RefreshToken
     * */
    @JvmOverloads
    @JvmStatic
    fun ChatEventHandler.refresh(
        listener: OnEventSentListener? = null,
    ) = trigger(RefreshToken, listener)

    /**
     * @see ChatEventHandler.trigger
     * @see TriggerEvent
     * */
    @JvmOverloads
    @JvmStatic
    fun ChatEventHandler.event(
        id: UUID,
        listener: OnEventSentListener? = null,
    ) = trigger(TriggerEvent(id), listener)

    /**
     * @see ChatEventHandler.trigger
     * @see VisitEvent
     * */
    @JvmOverloads
    @JvmStatic
    fun ChatEventHandler.visit(
        listener: OnEventSentListener? = null,
    ) = trigger(VisitEvent, listener)

}
