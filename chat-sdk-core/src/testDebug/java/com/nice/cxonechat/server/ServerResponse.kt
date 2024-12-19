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

@file:Suppress("MaxLineLength", "TestFunctionName", "StringLiteralDuplication", "LongMethod")

package com.nice.cxonechat.server

import com.nice.cxonechat.AbstractChatTestSubstrate.Companion.TestContactId
import com.nice.cxonechat.AbstractChatTestSubstrate.Companion.TestUUID
import com.nice.cxonechat.AbstractChatTestSubstrate.Companion.TestUUIDValue
import com.nice.cxonechat.enums.ActionType
import com.nice.cxonechat.enums.ActionType.CustomPopupBox
import com.nice.cxonechat.enums.ContactStatus
import com.nice.cxonechat.enums.ContactStatus.New
import com.nice.cxonechat.enums.EventType
import com.nice.cxonechat.enums.EventType.CustomerAuthorized
import com.nice.cxonechat.internal.model.AgentModel
import com.nice.cxonechat.internal.model.CustomFieldModel
import com.nice.cxonechat.internal.model.MessageModel
import com.nice.cxonechat.internal.model.network.EventCaseStatusChanged.CaseStatus
import com.nice.cxonechat.model.makeAgent
import com.nice.cxonechat.model.makeChatThread
import com.nice.cxonechat.model.makeMessageModel
import com.nice.cxonechat.model.toReceived
import com.nice.cxonechat.state.Connection
import com.nice.cxonechat.thread.ChatThread
import com.nice.cxonechat.thread.CustomField
import com.nice.cxonechat.tool.nextString
import com.nice.cxonechat.tool.serialize
import com.nice.cxonechat.util.DateTime
import java.util.Date
import java.util.UUID

@Suppress(
    "unused", // serialization uses fields in objects
    "TooManyFunctions",
    "FunctionNaming",
    "LongParameterList"
)
internal object ServerResponse {

    fun ConsumerAuthorized(
        identity: String = TestUUIDValue.toString(),
        firstName: String = "firstName",
        lastName: String = "lastName",
        accessToken: String = "access-token",
        expiresIn: Long = 0,
    ) = object {
        val eventId = UUID.randomUUID()
        val postback = object {
            val eventType = "ConsumerAuthorized"
            val data = object {
                val consumerIdentity = object {
                    val idOnExternalPlatform = identity
                    val firstName = firstName
                    val lastName = lastName
                }
                val accessToken = object {
                    val token = accessToken
                    val expiresIn = expiresIn
                    val isExpired = false
                }
            }
        }
    }.serialize()

    fun WelcomeMessage(
        message: String,
        fields: Map<String, String> = emptyMap(),
    ) = object {
        val eventId = TestUUID
        val eventObject = "ChatWindow"
        val eventType = "FireProactiveAction"
        val createdAt = Date(0)
        val data = object {
            val destination = object {
                val id = "dc5b212b-2bac-43f2-81cc-c0c4b1f24667"
            }
            val proactiveAction = object {
                val action = object {
                    val actionId = TestUUID
                    val actionName = "action-name"
                    val actionType = "WelcomeMessage"
                    val data = object {
                        val content = object {
                            val bodyText = message
                        }
                        val handover = object {
                            val customFields = fields.map { (key, value) ->
                                object {
                                    val ident = key
                                    val value = value
                                    val updatedAt = Date(0)
                                }
                            }
                        }
                    }
                }
            }
        }
    }.serialize()

    fun ActionPopup(
        params: Map<String, Any?>,
        fields: Map<String, String> = emptyMap(),
        actionId: UUID = TestUUIDValue,
        actionName: String = "actionName!",
        actionType: ActionType = CustomPopupBox,
    ) = object {
        val eventId = TestUUID
        val eventObject = "Thread"
        val eventType = "FireProactiveAction"
        val createdAt = Date(0)
        val data = object {
            val destination = object {
                val id = "5b33c4db-fae1-4bac-af7f-9b67ee367521"
            }
            val proactiveAction = object {
                val action = object {
                    val actionId = actionId
                    val actionName = actionName
                    val actionType = actionType
                    val data = object {
                        val content = object {
                            val bodyText = ""
                            val variables = params
                        }
                        val handover = object {
                            val customFields = fields.map { (key, value) ->
                                object {
                                    val ident = key
                                    val value = value
                                }
                            }
                        }
                    }
                }
            }
        }
    }.serialize()

    fun MoreMessagesLoaded(
        scrollToken: String,
        vararg messages: MessageModel,
    ) = object {
        val eventId = TestUUID
        val postback = object {
            val eventType = "MoreMessagesLoaded"
            val data = object {
                val messages = messages
                val scrollToken = scrollToken
            }
        }
    }.serialize()

    fun ThreadMetadataLoaded(
        agent: AgentModel = makeAgent(),
        message: MessageModel = makeMessageModel(),
    ) = object {
        val eventId = TestUUID
        val postback = object {
            val eventType = "ThreadMetadataLoaded"
            val data = object {
                val ownerAssignee = agent
                val lastMessage = message
            }
        }
    }.serialize()

    fun ThreadMetadataLoaded(
        message: Any,
    ) = object {
        val eventId = TestUUID
        val postback = object {
            val eventType = "ThreadMetadataLoaded"
            val data = object {
                val ownerAssignee = makeAgent()
                val lastMessage = message
            }
        }
    }.serialize()

    fun ThreadRecovered(
        scrollToken: String = nextString(),
        thread: ChatThread = makeChatThread(),
        agent: AgentModel = makeAgent(),
        customerCustomFields: List<CustomField> = emptyList(),
        vararg messages: MessageModel,
    ) = object {
        val eventId = TestUUID
        val postback = object {
            val eventType = "ThreadRecovered"
            val data = object {
                val contact = object {
                    val id = thread.contactId ?: TestContactId
                    val threadIdOnExternalPlatform = TestUUID
                    val status = "new"
                    val createdAt = Date(0)
                    val customFields = thread.fields.map(::CustomFieldModel)
                }
                val customer = object {
                    val customFields = customerCustomFields.map(::CustomFieldModel)
                }
                val messages = messages
                val inboxAssignee = agent
                val thread = thread.toReceived()
                val messagesScrollToken = scrollToken
            }
        }
    }.serialize()

    fun ThreadArchived(
        eventId: String = TestUUID,
    ) = object {
        val eventId = eventId
        val postback = object {
            val eventType = "ThreadArchived"
        }
    }.serialize()

    fun ThreadListFetched(
        threads: List<ChatThread>,
    ) = object {
        val eventId = TestUUID
        val postback = object {
            val eventType = "ThreadListFetched"
            val data = object {
                val threads = threads.map(ChatThread::toReceived)
            }
        }
    }.serialize()

    fun TypingStarted(
        thread: ChatThread,
        agent: AgentModel? = null,
    ) = object {
        val eventId = TestUUID
        val eventType = "SenderTypingStarted"
        val data = object {
            val thread = thread.toReceived()
            val user = agent
        }
    }.serialize()

    fun TypingEnded(
        thread: ChatThread,
        agent: AgentModel? = null,
    ) = object {
        val eventId = TestUUID
        val eventType = "SenderTypingEnded"
        val data = object {
            val thread = thread.toReceived()
            val user = agent
        }
    }.serialize()

    fun TokenRefreshed(
        accessToken: String = "access-token",
        expiresIn: Long = 0,
    ) = object {
        val eventId = UUID.randomUUID()
        val postback = object {
            val eventType = "TokenRefreshed"
            val data = object {
                val accessToken = object {
                    val token = accessToken
                    val expiresIn = expiresIn
                    val isExpired = false
                }
            }
        }
    }.serialize()

    fun ErrorResponse(errorCode: String) = object {
        val error = object {
            val errorCode = errorCode
            val transactionId = UUID.randomUUID()
        }
    }.serialize()

    fun MessageCreated(
        thread: ChatThread,
        message: MessageModel,
    ) = object {
        val eventId = TestUUID
        val eventType = "MessageCreated"
        val data = object {
            val message = message
            val case = object {
                val id = TestContactId
                val threadIdOnExternalPlatform = thread.id
                val status = "new"
                val createdAt = Date(0)
            }
            val thread = object {
                val idOnExternalPlatform = thread.id
                val threadName = "thread.threadName"
            }
        }
    }.serialize()

    fun MessageReadChanged(
        message: MessageModel,
    ) = object {
        val eventId = TestUUID
        val eventType = "MessageReadChanged".also { assert(it == EventType.MessageReadChanged.value) }
        val data = object {
            val message = message.copy(
                userStatistics = message.userStatistics.copy(readAt = Date(0))
            )
        }
    }.serialize()

    fun SetPositionInQueue(
        position: Int,
        isAgentAvailable: Boolean,
        threadId: UUID,
    ) = object {
        val eventId = TestUUID
        val eventType = "SetPositionInQueue".also { assert(it == EventType.SetPositionInQueue.value) }
        val data = object {
            val consumerContact = object {
                val id = TestContactId
                val threadIdOnExternalPlatform = threadId
                val status = ContactStatus.Pending.value
                val createdAt = Date(0)
            }
            val routingQueue = object {
                val id = "a:b:c"
            }
            val positionInQueue = position
            val isAnyAgentOnlineForQueue = isAgentAvailable
        }
    }.serialize()

    fun CaseStatusChanged(
        thread: ChatThread,
        status: CaseStatus,
    ) = object {
        val eventId = TestUUID
        val eventType = "CaseStatusChanged".also { assert(it == EventType.CaseStatusChanged.value) }
        val createdAt = DateTime(Date(0))
        val data = object {
            val case = object {
                val threadIdOnExternalPlatform = thread.id
                val status = status
                val statusUpdatedAt = DateTime(Date(0))
            }
        }
    }.serialize()

    fun EventInS3(
        url: String = "https://some.other.url/some/path",
        date: String = "2024-05-06T20:45:53.654Z",
        eventType: EventType = CustomerAuthorized
    ) = object {
        val eventId = TestUUID
        val eventType = "EventInS3".also { assert(it == EventType.EventInS3.value) }
        val createdAt = date
        val data = object {
            val s3Object = object {
                val url = url
            }
            val originEvent = object {
                val eventType = eventType.value
            }
        }
    }.serialize()

    fun LivechatRecovered(
        scrollToken: String = nextString(),
        thread: ChatThread = makeChatThread(),
        agent: AgentModel? = makeAgent(),
        customerCustomFields: List<CustomField> = emptyList(),
        status: ContactStatus = New,
        vararg messages: MessageModel,
    ) = object {
        val eventId = TestUUID
        val postback = object {
            val eventType = "LivechatRecovered"
            val data = object {
                val contact = object {
                    val id = thread.contactId ?: TestContactId
                    val threadIdOnExternalPlatform = TestUUID
                    val status = status
                    val createdAt = Date(0)
                    val customFields = thread.fields.map(::CustomFieldModel)
                }
                val customer = object {
                    val customFields = customerCustomFields.map(::CustomFieldModel)
                }
                val messages = messages
                val inboxAssignee = agent
                val thread = thread.toReceived()
                val messagesScrollToken = scrollToken
            }
        }
    }.serialize()

    fun CaseInboxAssigneeChanged(
        thread: ChatThread,
        agent: AgentModel?,
        connection: Connection,
    ) = object {
        val eventId = TestUUID
        val eventType = "CaseInboxAssigneeChanged".also { assert(it == EventType.CaseInboxAssigneeChanged.value) }
        val createdAt = DateTime(Date(0))
        val data = object {
            val channel = object {
                val id = connection.channelId
            }
            val brand = object {
                val id = connection.brandId
            }
            val case = object {
                val id = thread.contactId ?: TestContactId
                val threadIdOnExternalPlatform = thread.id
                val status = New.value
                val createdAt = DateTime(Date(0))
                val statusUpdatedAt = DateTime(Date(0))
            }
            val inboxAssignee = agent
        }
    }.serialize()

    object Message {

        private operator fun invoke(threadId: UUID, content: Any) = object {
            val idOnExternalPlatform = TestUUID
            val threadIdOnExternalPlatform = threadId
            val messageContent = content
            val createdAt = Date(0)
            val direction = "inbound"
            val authorEndUserIdentity = object {
                val idOnExternalPlatform = TestUUID
                val firstName = "Prokop"
                val lastName = "Buben"
                val nickname = "@prokopBuben123"
                val image = "https=//www.nice.com/resources/pages/global-management/Managment/Paul_Jarman.jpg"
            }
            val attachments = listOf(object {
                val friendlyName = "image.jpg"
                val url = "https=//bla.com/image.jpg"
            })
            val userStatistics = object {}
        }

        fun Text(threadId: UUID, text: String = "Test inbound message") = Message(
            threadId = threadId,
            content = object {
                val type = "TEXT"
                val payload = object {
                    val text = text
                }
            }
        )

        fun QuickReplies(threadId: UUID) = Message(
            threadId = threadId,
            content = object {
                val type = "QUICK_REPLIES"
                val fallbackText = "Text sent if rich message is not available on external platform"
                val payload = object {
                    val text = object {
                        val content = "Hello, we will deliver the package between 12:00 and 16:00. Please specify which day."
                    }
                    val actions = listOf(
                        object {
                            val type = "REPLY_BUTTON"
                            val text = "Button 1"
                            val postback = "click-on-button-1"
                        },
                        object {
                            val type = "REPLY_BUTTON"
                            val text = "Button 2"
                            val postback = "click-on-button-2"
                        },
                        object {
                            val type = "REPLY_BUTTON"
                            val text = "Button 3"
                            val postback = "click-on-button-3"
                        }
                    )
                }
            }
        )

        fun ListPicker(threadId: UUID) = Message(
            threadId = threadId,
            content = object {
                val type = "LIST_PICKER"
                val fallbackText = "Text sent if rich message is not available on external platform"
                val payload = object {
                    val title = object {
                        val content = "Choose a color!"
                    }
                    val text = object {
                        val content = "What is your favourite color?"
                    }
                    val actions = listOf(
                        object {
                            val type = "REPLY_BUTTON"
                            val icon = object {
                                val fileName = "place-kitten.jpg"
                                val url = "https://placekitten.com/200/300"
                                val mimeType = "image/jpeg"
                            }
                            val text = "red"
                            val description = "Like a tomato"
                            val postback = "/red"
                        },
                        object {
                            val type = "REPLY_BUTTON"
                            val text = "Green"
                        }
                    )
                }
            }
        )

        fun RichLink(threadId: UUID) = Message(
            threadId = threadId,
            content = object {
                val type = "RICH_LINK"
                val fallbackText = "Text sent if rich message is not available on external platform"
                val payload = object {
                    val title = object {
                        val content = "Choose a color!"
                    }
                    val media = object {
                        val fileName = "place-kitten.jpg"
                        val url = "https://placekitten.com/200/300"
                        val mimeType = "image/jpeg"
                    }
                    val url = "https://www.google.com"
                }
            }
        )

        fun InvalidContent(threadId: UUID) = Message(
            threadId = threadId,
            content = object {
                val type = "FOOBAR"
            }
        )
    }
}
