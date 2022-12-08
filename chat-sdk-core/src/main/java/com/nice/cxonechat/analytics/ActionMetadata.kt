package com.nice.cxonechat.analytics

import com.nice.cxonechat.Public
import com.nice.cxonechat.event.ProactiveActionClickEvent
import com.nice.cxonechat.event.ProactiveActionDisplayEvent
import com.nice.cxonechat.event.ProactiveActionFailureEvent
import com.nice.cxonechat.event.ProactiveActionSuccessEvent

/**
 * Metadata received from proactive actions. You should use
 * these when notifying server about your events.
 *
 * @see ProactiveActionClickEvent
 * @see ProactiveActionDisplayEvent
 * @see ProactiveActionFailureEvent
 * @see ProactiveActionSuccessEvent
 * */
@Public
sealed interface ActionMetadata
