package com.nice.cxonechat.server

import com.nice.cxonechat.enums.EventType
import strucut.StructScope
import strucut.verifyStructureOf

/**
 * This test is a means to safeguard ServerRequests. If anything shall
 * fail in this suite, then you broke a functionality that was explicitly
 * programmed in to work with the API.
 *
 * Fixing these tests is permitted only in circumstances where the API
 * actually changes, not when this fails. Be warned.
 * */
@Suppress("StringLiteralDuplication", "FunctionMaxLength")
internal object ServerRequestAssertions {

    private const val ChatWindowEvent = "chatWindowEvent"
    private const val Register = "register"

    fun String.verifyLoadMore() = apply {
        verifyStructureOf(this) {
            eventBaseline(action = ChatWindowEvent)
            payload(type = "LoadMoreMessages") {
                prop("scrollToken")
                thread()
                prop("oldestMessageDatetime")
            }
        }
    }

    fun String.verifyLoadThreadMetadata() = apply {
        verifyStructureOf(this) {
            eventBaseline(action = ChatWindowEvent)
            payload(type = "LoadThreadMetadata") {
                thread()
            }
        }
    }

    fun String.verifyArchiveThread() = apply {
        verifyStructureOf(this) {
            eventBaseline(action = ChatWindowEvent)
            payload(type = "ArchiveThread") {
                thread()
            }
        }
    }

    fun String.verifyMarkThreadRead() = apply {
        verifyStructureOf(this) {
            eventBaseline(action = ChatWindowEvent)
            payload(type = "MessageSeenByCustomer") {
                thread()
            }
        }
    }

    fun String.verifySenderTypingStarted() = apply {
        verifyStructureOf(this) {
            eventBaseline(action = ChatWindowEvent)
            payload(type = "SenderTypingStarted") {
                thread()
            }
        }
    }

    fun String.verifySenderTypingEnded() = apply {
        verifyStructureOf(this) {
            eventBaseline(action = ChatWindowEvent)
            payload(type = "SenderTypingEnded") {
                thread()
            }
        }
    }

    fun String.verifyAuthorizeConsumer() = apply {
        verifyStructureOf(this) {
            eventBaseline(action = Register)
            payload(type = "AuthorizeCustomer") {
                prop("authorization") {
                    prop("authorizationCode")
                    prop("codeVerifier")
                }
            }
        }
    }

    fun String.verifyReconnectConsumer() = apply {
        verifyStructureOf(this) {
            eventBaseline(action = ChatWindowEvent)
            legacyPayload(type = "ReconnectConsumer") {
                accessToken()
            }
        }
    }

    fun String.verifyStoreVisitorEvent() = apply {
        verifyStructureOf(this) {
            eventBaseline(action = ChatWindowEvent)
            legacyPayload(
                type = "StoreVisitorEvents"
            ) {
                prop("visitorEvents") {
                    prop("type")
                    prop("id")
                    prop("createdAtWithMilliseconds")
                }
            }
        }
    }

    fun String.verifyStoreVisitor() = apply {
        verifyStructureOf(this) {
            eventBaseline(action = ChatWindowEvent)
            legacyPayload(type = "StoreVisitor") {
                fingerprint("browserFingerprint")
            }
        }
    }

    fun String.verifyExecuteTrigger() = apply {
        verifyStructureOf(this) {
            eventBaseline(action = ChatWindowEvent)
            legacyPayload(type = "ExecuteTrigger") {
                prop("trigger") {
                    prop("id")
                }
            }
        }
    }

    fun String.verifyRefreshToken() = apply {
        verifyStructureOf(this) {
            eventBaseline(action = ChatWindowEvent)
            legacyPayload(type = "RefreshToken") {
                accessToken()
            }
        }
    }

    fun String.verifySendMessage() = apply {
        verifyStructureOf(this) {
            eventBaseline(action = ChatWindowEvent)
            payload(type = "SendMessage") {
                message()
            }
        }
    }

    fun String.verifySetCustomerCustomFields() = apply {
        verifyStructureOf(this) {
            eventBaseline(action = ChatWindowEvent)
            payload(type = "SetCustomerCustomFields") {
                customFields()
            }
        }
    }

    fun String.verifySetContactCustomFields() = apply {
        verifyStructureOf(this) {
            eventBaseline(action = ChatWindowEvent)
            payload(type = "SetContactCustomFields") {
                thread()
                customFields()
                prop("contact") {
                    prop("id")
                }
            }
        }
    }

    fun String.verifyUpdateThread() = apply {
        verifyStructureOf(this) {
            eventBaseline(action = ChatWindowEvent)
            payload(type = "UpdateThread") {
                thread()
            }
        }
    }

    fun String.verifyFetchThreadList() = apply {
        verifyStructureOf(this) {
            eventBaseline(action = ChatWindowEvent)
            payload(type = "FetchThreadList") {}
        }
    }

    fun String.verifyRecoverThread() = apply {
        verifyStructureOf(this) {
            eventBaseline(action = ChatWindowEvent)
            payload(type = "RecoverThread") {
                thread()
            }
        }
    }

    fun String.verifySendOutbound() = apply {
        verifyStructureOf(this) {
            eventBaseline(action = ChatWindowEvent)
            legacyPayload(type = EventType.SendOutbound.value) {
                message {
                    fingerprint("browserFingerprint")
                }
            }
        }
    }

    // ---

    private fun StructScope.eventBaseline(action: String) {
        prop("action", action)
        prop("eventId")
    }
    private fun StructScope.payload(
        type: String,
        identityStruct: StructScope.() -> Unit = { identity() },
        data: StructScope.() -> Unit,
    ) {
        prop("payload") {
            prop("brand") {
                prop("id")
            }
            prop("channel") {
                prop("id")
            }
            identityStruct()
            prop("eventType", type)
            prop("data", data)
            propOpt("destination") {
                prop("id")
            }
            propOpt("visitor") {
                prop("id")
            }
        }
    }

    private fun StructScope.legacyPayload(
        type: String,
        data: StructScope.() -> Unit,
    ) = payload(
        type = type,
        identityStruct = { identity("consumerIdentity") },
        data = data,
    )

    private fun StructScope.identity(
        type: String = "customerIdentity",
    ) {
        prop(type) {
            prop("idOnExternalPlatform")
            prop("firstName")
            prop("lastName")
        }
    }

    private fun StructScope.message(
        fingerprintStruct: StructScope.() -> Unit = { fingerprint() },
    ) {
        thread()
        prop("messageContent") {
            prop("type")
            prop("payload") {
                prop("text")
                prop("elements") {
                    prop("id")
                    prop("type")
                    prop("text")
                    prop("postback")
                    prop("url")
                    prop("fileName")
                    prop("mimeType")
                    prop("elements")
                    prop("variables")
                }
            }
        }
        prop("idOnExternalPlatform")
        prop("attachments") {
            prop("url")
            prop("friendlyName")
            prop("mimeType")
        }
        fingerprintStruct()
        accessToken()
    }

    private fun StructScope.fingerprint(
        type: String = "deviceFingerprint"
    ) {
        prop(type) {
            prop("deviceToken")
            prop("browser")
            prop("browserVersion")
            prop("country")
            prop("ip")
            prop("language")
            prop("location")
            prop("applicationType")
            prop("os")
            prop("osVersion")
            prop("deviceType")
        }
    }

    private fun StructScope.customFields() {
        prop("customFields") {
            prop("ident")
            prop("value")
        }
    }

    private fun StructScope.thread() {
        prop("thread") {
            prop("idOnExternalPlatform")
        }
    }

    private fun StructScope.accessToken() {
        prop("accessToken") {
            prop("token")
        }
    }
}
