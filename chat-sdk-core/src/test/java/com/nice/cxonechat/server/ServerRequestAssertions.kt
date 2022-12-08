package com.nice.cxonechat.server

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
            payload(type = "MessageSeenByConsumer") {
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
            payload(type = "AuthorizeConsumer") {
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
            payload(type = "ReconnectConsumer") {
                accessToken()
            }
        }
    }

    fun String.verifyStoreVisitorEvent() = apply {
        verifyStructureOf(this) {
            eventBaseline(action = ChatWindowEvent)
            payload(
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
            payload(type = "StoreVisitor") {
                prop("browserFingerprint") {
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
        }
    }

    fun String.verifyExecuteTrigger() = apply {
        verifyStructureOf(this) {
            eventBaseline(action = ChatWindowEvent)
            payload(type = "ExecuteTrigger") {
                prop("trigger") {
                    prop("id")
                }
            }
        }
    }

    fun String.verifyRefreshToken() = apply {
        verifyStructureOf(this) {
            eventBaseline(action = ChatWindowEvent)
            payload(type = "RefreshToken") {
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

    fun String.verifySetConsumerCustomFields() = apply {
        verifyStructureOf(this) {
            eventBaseline(action = ChatWindowEvent)
            payload(type = "SetConsumerCustomFields") {
                customFields()
            }
        }
    }

    fun String.verifySetConsumerContactCustomFields() = apply {
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
            payload(type = "SendMessage") {
                message()
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
        data: StructScope.() -> Unit,
    ) {
        prop("payload") {
            prop("brand") {
                prop("id")
            }
            prop("channel") {
                prop("id")
            }
            prop("consumerIdentity") {
                prop("idOnExternalPlatform")
                prop("firstName")
                prop("lastName")
            }
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

    private fun StructScope.message() {
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
        prop("browserFingerprint") {
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
        accessToken()
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
