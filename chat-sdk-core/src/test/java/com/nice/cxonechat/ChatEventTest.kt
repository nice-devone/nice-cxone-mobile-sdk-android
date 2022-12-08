@file:Suppress("FunctionMaxLength")

package com.nice.cxonechat

import com.nice.cxonechat.ChatEventHandlerActions.chatWindowOpen
import com.nice.cxonechat.ChatEventHandlerActions.conversion
import com.nice.cxonechat.ChatEventHandlerActions.customVisitor
import com.nice.cxonechat.ChatEventHandlerActions.event
import com.nice.cxonechat.ChatEventHandlerActions.pageView
import com.nice.cxonechat.ChatEventHandlerActions.proactiveActionClick
import com.nice.cxonechat.ChatEventHandlerActions.proactiveActionDisplay
import com.nice.cxonechat.ChatEventHandlerActions.proactiveActionFailure
import com.nice.cxonechat.ChatEventHandlerActions.proactiveActionSuccess
import com.nice.cxonechat.ChatEventHandlerActions.refresh
import com.nice.cxonechat.ChatEventHandlerActions.visit
import com.nice.cxonechat.analytics.ActionMetadataInternal
import com.nice.cxonechat.enums.ActionType
import com.nice.cxonechat.event.AuthorizeCustomerEvent
import com.nice.cxonechat.event.ReconnectCustomerEvent
import com.nice.cxonechat.server.ServerRequest
import com.nice.cxonechat.server.ServerRequest.StoreVisitorEvents
import org.junit.Test
import java.util.UUID

internal class ChatEventTest : AbstractChatTest() {

    private lateinit var events: ChatEventHandler

    override fun prepare() {
        super.prepare()
        events = chat.events()
    }

    // ---

    @Test
    fun trigger_PageViewEvent_sendsExpectedMessage() {
        val title = "title?"
        val url = "url!"
        val eventData = StoreVisitorEvents.PageView(title, url)
        assertSendText(ServerRequest.StoreVisitorEvent(connection, eventData), replaceDate = true) {
            events.pageView(title, url)
        }
    }

    @Test
    fun trigger_ChatWindowOpenEvent_sendsExpectedMessage() {
        val eventData = StoreVisitorEvents.ChatWindowOpenEvent()
        assertSendText(ServerRequest.StoreVisitorEvent(connection, eventData), replaceDate = true) {
            events.chatWindowOpen()
        }
    }

    @Test
    fun trigger_VisitEvent_sendsExpectedMessage() {
        val eventData = StoreVisitorEvents.VisitorVisit()
        assertSendText(ServerRequest.StoreVisitorEvent(connection, eventData), replaceDate = true) {
            events.visit()
        }
    }

    @Test
    fun trigger_ConversionEvent_sendsExpectedMessage() {
        val type = "my-type"
        val value = 503
        val eventData = StoreVisitorEvents.Conversion(type, value)
        assertSendText(ServerRequest.StoreVisitorEvent(connection, eventData), replaceDate = true) {
            events.conversion(type, value)
        }
    }

    @Test
    fun trigger_CustomVisitorEvent_sendsExpectedMessage() {
        val data = "foo bar"
        val eventData = StoreVisitorEvents.CustomVisitorEvent(data)
        assertSendText(ServerRequest.StoreVisitorEvent(connection, eventData), replaceDate = true) {
            events.customVisitor(data)
        }
    }

    @Test
    fun trigger_ProactiveActionDisplayEvent_sendsExpectedMessage() {
        val actionName = "action-name1"
        val actionType = ActionType.CustomPopupBox
        val eventData = StoreVisitorEvents.ProactiveActionDisplayed(actionName, actionType.value)
        assertSendText(ServerRequest.StoreVisitorEvent(connection, eventData), replaceDate = true) {
            val info = ActionMetadataInternal(TestUUIDValue, actionName, actionType)
            events.proactiveActionDisplay(info)
        }
    }

    @Test
    fun trigger_ProactiveActionClickEvent_sendsExpectedMessage() {
        val actionName = "action-name2"
        val actionType = ActionType.CustomPopupBox
        val eventData = StoreVisitorEvents.ProactiveActionClicked(actionName, actionType.value)
        assertSendText(ServerRequest.StoreVisitorEvent(connection, eventData), replaceDate = true) {
            val info = ActionMetadataInternal(TestUUIDValue, actionName, actionType)
            events.proactiveActionClick(info)
        }
    }

    @Test
    fun trigger_ProactiveActionSuccessEvent_sendsExpectedMessage() {
        val actionName = "action-name3"
        val actionType = ActionType.CustomPopupBox
        val eventData = StoreVisitorEvents.ProactiveActionSuccess(actionName, actionType.value)
        assertSendText(ServerRequest.StoreVisitorEvent(connection, eventData), replaceDate = true) {
            val info = ActionMetadataInternal(TestUUIDValue, actionName, actionType)
            events.proactiveActionSuccess(info)
        }
    }

    @Test
    fun trigger_ProactiveActionFailureEvent_sendsExpectedMessage() {
        val actionName = "action-name4"
        val actionType = ActionType.CustomPopupBox
        val eventData = StoreVisitorEvents.ProactiveActionFailed(actionName, actionType.value)
        assertSendText(ServerRequest.StoreVisitorEvent(connection, eventData), replaceDate = true) {
            val info = ActionMetadataInternal(TestUUIDValue, actionName, actionType)
            events.proactiveActionFailure(info)
        }
    }

    @Test
    fun trigger_TriggerEvent_sendsExpectedMessage() {
        val id = UUID.randomUUID()
        assertSendText(ServerRequest.ExecuteTrigger(connection, id), id.toString()) {
            events.event(id)
        }
    }

    @Test
    fun trigger_RefreshToken_sendsExpectedMessage() {
        assertSendText(ServerRequest.RefreshToken(connection)) {
            events.refresh()
        }
    }

    @Test
    fun trigger_AuthorizeCustomerEvent_sendsExpectedMessage() {
        val code = "aaa-code"
        val verifier = "verifier!"
        assertSendText(ServerRequest.AuthorizeConsumer(connection, code, verifier)) {
            events.trigger(AuthorizeCustomerEvent(code, verifier))
        }
    }

    @Test
    fun trigger_ReconnectCustomerEvent_sendsExpectedMessage() {
        assertSendText(ServerRequest.ReconnectConsumer(connection)) {
            events.trigger(ReconnectCustomerEvent)
        }
    }

}
