@file:Suppress("MaxLineLength", "TestFunctionName")

package com.nice.cxonechat.server

import com.nice.cxonechat.AbstractChatTestSubstrate.Companion.TestUUIDValue
import com.nice.cxonechat.enums.VisitorEventType
import com.nice.cxonechat.enums.VisitorEventType.ChatWindowOpened
import com.nice.cxonechat.enums.VisitorEventType.VisitorVisit
import com.nice.cxonechat.internal.model.AttachmentModel
import com.nice.cxonechat.internal.model.CustomFieldModel
import com.nice.cxonechat.internal.model.network.ActionArchiveThread
import com.nice.cxonechat.internal.model.network.ActionAuthorizeCustomer
import com.nice.cxonechat.internal.model.network.ActionCustomerTyping
import com.nice.cxonechat.internal.model.network.ActionExecuteTrigger
import com.nice.cxonechat.internal.model.network.ActionFetchThread
import com.nice.cxonechat.internal.model.network.ActionLoadMoreMessages
import com.nice.cxonechat.internal.model.network.ActionMessage
import com.nice.cxonechat.internal.model.network.ActionMessageSeenByCustomer
import com.nice.cxonechat.internal.model.network.ActionReconnectCustomer
import com.nice.cxonechat.internal.model.network.ActionRecoverThread
import com.nice.cxonechat.internal.model.network.ActionRefreshToken
import com.nice.cxonechat.internal.model.network.ActionSetContactCustomFields
import com.nice.cxonechat.internal.model.network.ActionSetCustomerCustomFields
import com.nice.cxonechat.internal.model.network.ActionStoreVisitor
import com.nice.cxonechat.internal.model.network.ActionStoreVisitorEvent
import com.nice.cxonechat.internal.model.network.ActionUpdateThread
import com.nice.cxonechat.internal.model.network.Conversion
import com.nice.cxonechat.internal.model.network.PageViewData
import com.nice.cxonechat.internal.model.network.ProactiveActionInfo
import com.nice.cxonechat.internal.model.network.VisitorEvent
import com.nice.cxonechat.message.MessageDirection.ToClient
import com.nice.cxonechat.server.ServerRequestAssertions.verifyArchiveThread
import com.nice.cxonechat.server.ServerRequestAssertions.verifyAuthorizeConsumer
import com.nice.cxonechat.server.ServerRequestAssertions.verifyExecuteTrigger
import com.nice.cxonechat.server.ServerRequestAssertions.verifyFetchThreadList
import com.nice.cxonechat.server.ServerRequestAssertions.verifyLoadMore
import com.nice.cxonechat.server.ServerRequestAssertions.verifyMarkThreadRead
import com.nice.cxonechat.server.ServerRequestAssertions.verifyReconnectConsumer
import com.nice.cxonechat.server.ServerRequestAssertions.verifyRecoverThread
import com.nice.cxonechat.server.ServerRequestAssertions.verifyRefreshToken
import com.nice.cxonechat.server.ServerRequestAssertions.verifySendMessage
import com.nice.cxonechat.server.ServerRequestAssertions.verifySendOutbound
import com.nice.cxonechat.server.ServerRequestAssertions.verifySenderTypingEnded
import com.nice.cxonechat.server.ServerRequestAssertions.verifySenderTypingStarted
import com.nice.cxonechat.server.ServerRequestAssertions.verifySetConsumerContactCustomFields
import com.nice.cxonechat.server.ServerRequestAssertions.verifySetConsumerCustomFields
import com.nice.cxonechat.server.ServerRequestAssertions.verifyStoreVisitor
import com.nice.cxonechat.server.ServerRequestAssertions.verifyStoreVisitorEvent
import com.nice.cxonechat.server.ServerRequestAssertions.verifyUpdateThread
import com.nice.cxonechat.state.Connection
import com.nice.cxonechat.storage.ValueStorage
import com.nice.cxonechat.thread.ChatThread
import com.nice.cxonechat.tool.serialize
import java.util.Date
import java.util.UUID

internal object ServerRequest {

    fun LoadMore(connection: Connection, thread: ChatThread): String {
        return ActionLoadMoreMessages(connection, thread).copy(eventId = TestUUIDValue).serialize().verifyLoadMore()
    }

    fun ArchiveThread(connection: Connection, thread: ChatThread): String {
        return ActionArchiveThread(connection, thread).copy(eventId = TestUUIDValue).serialize().verifyArchiveThread()
    }

    fun MarkThreadRead(connection: Connection, thread: ChatThread): String {
        return ActionMessageSeenByCustomer(connection, thread).copy(eventId = TestUUIDValue).serialize().verifyMarkThreadRead()
    }

    fun SenderTypingStarted(connection: Connection, thread: ChatThread): String {
        return ActionCustomerTyping.started(connection, thread).copy(eventId = TestUUIDValue).serialize().verifySenderTypingStarted()
    }

    fun SenderTypingEnded(connection: Connection, thread: ChatThread): String {
        return ActionCustomerTyping.ended(connection, thread).copy(eventId = TestUUIDValue).serialize().verifySenderTypingEnded()
    }

    fun AuthorizeConsumer(connection: Connection, code: String, verifier: String): String {
        return ActionAuthorizeCustomer(connection, code, verifier).copy(eventId = TestUUIDValue).serialize().verifyAuthorizeConsumer()
    }

    fun ReconnectConsumer(connection: Connection, token: String = "token"): String {
        return ActionReconnectCustomer(connection, TestUUIDValue, token).copy(eventId = TestUUIDValue).serialize().verifyReconnectConsumer()
    }

    fun StoreVisitorEvent(connection: Connection, vararg events: VisitorEvent): String {
        return ActionStoreVisitorEvent(connection, TestUUIDValue, TestUUIDValue, events = events).copy(eventId = TestUUIDValue).serialize().verifyStoreVisitorEvent()
    }

    fun StoreVisitor(connection: Connection, token: String?): String {
        return ActionStoreVisitor(connection, TestUUIDValue, token).copy(eventId = TestUUIDValue).serialize().verifyStoreVisitor()
    }

    fun ExecuteTrigger(connection: Connection, id: UUID): String {
        return ActionExecuteTrigger(connection, TestUUIDValue, TestUUIDValue, id).copy(eventId = TestUUIDValue).serialize().verifyExecuteTrigger()
    }

    fun RefreshToken(connection: Connection, token: String = "token"): String {
        return ActionRefreshToken(connection, token).copy(eventId = TestUUIDValue).serialize().verifyRefreshToken()
    }

    @Suppress("LongParameterList")
    fun SendMessage(
        connection: Connection,
        thread: ChatThread,
        storage: ValueStorage,
        message: String,
        fields: Map<String, String> = emptyMap(),
        attachments: List<AttachmentModel> = emptyList(),
    ): String {
        return ActionMessage(connection, thread, TestUUIDValue, message, attachments, fields.map(::CustomFieldModel), storage.authToken).copy(eventId = TestUUIDValue).serialize().verifySendMessage()
    }

    fun SetConsumerCustomFields(
        connection: Connection,
        fields: Map<String, String> = emptyMap(),
    ): String {
        return ActionSetCustomerCustomFields(connection, fields.map(::CustomFieldModel)).copy(eventId = TestUUIDValue).serialize().verifySetConsumerCustomFields()
    }

    fun SetConsumerContactCustomFields(
        connection: Connection,
        thread: ChatThread,
        fields: Map<String, String> = emptyMap(),
    ): String {
        return ActionSetContactCustomFields(connection, thread, fields.map(::CustomFieldModel)).copy(eventId = TestUUIDValue).serialize().verifySetConsumerContactCustomFields()
    }

    fun UpdateThread(connection: Connection, thread: ChatThread): String {
        return ActionUpdateThread(connection, thread).copy(eventId = TestUUIDValue).serialize().verifyUpdateThread()
    }

    fun FetchThreadList(connection: Connection): String {
        return ActionFetchThread(connection).copy(eventId = TestUUIDValue).serialize().verifyFetchThreadList()
    }

    fun RecoverThread(connection: Connection, thread: ChatThread): String {
        return ActionRecoverThread(connection, thread).copy(eventId = TestUUIDValue).serialize().verifyRecoverThread()
    }

    fun SendOutbound(
        connection: Connection,
        thread: ChatThread,
        storage: ValueStorage,
        message: String,
    ): String {
        return ActionMessage(connection, thread, TestUUIDValue, message, emptyList(), emptyList(), storage.authToken, ToClient).copy(eventId = TestUUIDValue).serialize().verifySendOutbound()
    }

    object StoreVisitorEvents {

        fun ChatWindowOpenEvent(date: Date = Date(0)): VisitorEvent {
            return VisitorEvent(type = ChatWindowOpened, id = TestUUIDValue, createdAt = date)
        }

        fun VisitorVisit(date: Date = Date(0)): VisitorEvent {
            return VisitorEvent(type = VisitorVisit, id = TestUUIDValue, createdAt = date)
        }

        fun Conversion(
            type: String,
            value: Int,
            createdAt: Date = Date(0),
        ): VisitorEvent {
            val model = Conversion(type, value, timestamp = createdAt)
            return VisitorEvent(VisitorEventType.Conversion, id = TestUUIDValue, createdAt, model)
        }

        fun CustomVisitorEvent(data: String, date: Date = Date(0)): VisitorEvent {
            return VisitorEvent(type = VisitorEventType.ProactiveActionDisplayed, createdAt = date, data = data, id = TestUUIDValue)
        }

        fun ProactiveActionDisplayed(
            actionName: String,
            actionType: String,
            date: Date = Date(0),
        ): VisitorEvent {
            val info = ProactiveActionInfo(TestUUIDValue, actionName, actionType)
            return VisitorEvent(type = VisitorEventType.ProactiveActionDisplayed, createdAt = date, data = info, id = TestUUIDValue)
        }

        fun ProactiveActionClicked(
            actionName: String,
            actionType: String,
            date: Date = Date(0),
        ): VisitorEvent {
            val info = ProactiveActionInfo(TestUUIDValue, actionName, actionType)
            return VisitorEvent(type = VisitorEventType.ProactiveActionClicked, createdAt = date, data = info, id = TestUUIDValue)
        }

        fun ProactiveActionSuccess(
            actionName: String,
            actionType: String,
            date: Date = Date(0),
        ): VisitorEvent {
            val info = ProactiveActionInfo(TestUUIDValue, actionName, actionType)
            return VisitorEvent(type = VisitorEventType.ProactiveActionSuccess, createdAt = date, data = info, id = TestUUIDValue)
        }

        fun ProactiveActionFailed(
            actionName: String,
            actionType: String,
            date: Date = Date(0),
        ): VisitorEvent {
            val info = ProactiveActionInfo(TestUUIDValue, actionName, actionType)
            return VisitorEvent(type = VisitorEventType.ProactiveActionFailed, createdAt = date, data = info, id = TestUUIDValue)
        }

        fun PageView(title: String, uri: String, date: Date = Date(0)): VisitorEvent {
            return VisitorEvent(VisitorEventType.PageView, id = TestUUIDValue, createdAt = date, data = PageViewData(title, uri))
        }
    }
}
