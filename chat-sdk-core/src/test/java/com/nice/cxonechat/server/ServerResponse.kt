@file:Suppress("MaxLineLength", "TestFunctionName", "StringLiteralDuplication", "LongMethod")

package com.nice.cxonechat.server

import com.nice.cxonechat.AbstractChatTestSubstrate.Companion.TestUUID
import com.nice.cxonechat.AbstractChatTestSubstrate.Companion.TestUUIDValue
import com.nice.cxonechat.enums.ActionType
import com.nice.cxonechat.enums.ActionType.CustomPopupBox
import com.nice.cxonechat.internal.model.AgentModel
import com.nice.cxonechat.internal.model.MessageModel
import com.nice.cxonechat.model.makeAgent
import com.nice.cxonechat.model.makeChatThread
import com.nice.cxonechat.model.makeMessageModel
import com.nice.cxonechat.model.toReceived
import com.nice.cxonechat.thread.ChatThread
import com.nice.cxonechat.tool.nextString
import com.nice.cxonechat.tool.serialize
import java.util.Date
import java.util.UUID

internal object ServerResponse {

    fun ConsumerAuthorized(
        identity: UUID = TestUUIDValue,
        firstName: String = "firstName",
        lastName: String = "lastName",
        accessToken: String = "access-token",
        expiresIn: Long = 0,
    ) = @Suppress("unused") object {
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
    ) = @Suppress("unused") object {
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
    ) = @Suppress("unused") object {
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
    ) = @Suppress("unused") object {
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
    ) = @Suppress("unused") object {
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
    ) = @Suppress("unused") object {
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
        vararg messages: MessageModel,
    ) = @Suppress("unused") object {
        val eventId = TestUUID
        val postback = object {
            val eventType = "ThreadRecovered"
            val data = object {
                val consumerContact = object {
                    val id = "95vq7qRDsC"
                    val threadIdOnExternalPlatform = TestUUID
                    val status = "New"
                    val createdAt = Date(0)
                }
                val messages = messages
                val ownerAssignee = agent
                val thread = thread.toReceived()
                val messagesScrollToken = scrollToken
            }
        }
    }.serialize()

    fun ThreadListFetched(
        threads: List<ChatThread>,
    ) = @Suppress("unused") object {
        val eventId = TestUUID
        val postback = object {
            val eventType = "ThreadListFetched"
            val data = object {
                val threads = threads.map { it.toReceived() }
            }
        }
    }.serialize()

    fun TypingStarted(
        thread: ChatThread,
    ) = @Suppress("unused") object {
        val eventId = TestUUID
        val eventType = "SenderTypingStarted"
        val data = object {
            val thread = thread.toReceived()
        }
    }.serialize()

    fun TypingEnded(
        thread: ChatThread,
    ) = @Suppress("unused") object {
        val eventId = TestUUID
        val eventType = "SenderTypingEnded"
        val data = object {
            val thread = thread.toReceived()
        }
    }.serialize()

    fun TokenRefreshed(
        accessToken: String = "access-token",
        expiresIn: Long = 0,
    ) = @Suppress("unused") object {
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

    fun MessageCreated(
        thread: ChatThread,
        message: MessageModel,
    ) = @Suppress("unused") object {
        val eventId = TestUUID
        val eventType = "MessageCreated"
        val data = object {
            val message = message
            val case = object {
                val id = "id"
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

    object Message {

        private operator fun invoke(threadId: UUID, content: Any) = @Suppress("unused") object {
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
            content = @Suppress("unused") object {
                val type = "TEXT"
                val payload = object {
                    val text = text
                }
            }
        )

        fun Menu(threadId: UUID) = Message(
            threadId = threadId,
            content = @Suppress("unused") object {
                val type = "PLUGIN"
                val payload = object {
                    val postback = ""
                    val elements = listOf(object {
                        val id = "Ek4tPy1h4"
                        val type = "MENU"
                        val elements = listOf(object {
                            val id = "Uk4tPy1h2"
                            val type = "FILE"
                            val url = "https=//picsum.photos/300/150"
                            val filename = "photo.jpg"
                            val mimeType = "image/jpeg"
                        }, object {
                            val id = "Ck4tPy1h3"
                            val type = "TITLE"
                            val text = "Hello!"
                        }, object {
                            val id = "CA4tPy333"
                            val type = "SUBTITLE"
                            val text = "Hello, but smaller!"
                        }, object {
                            val id = "Ek4tPy1h1"
                            val type = "TEXT"
                            val text = "Lorem Impsum..."
                        }, object {
                            val id = "Nkm0hRAiE"
                            val type = "BUTTON"
                            val text = "Click me!"
                            val postback = "click-on-button-1"
                        }, object {
                            val id = "NkGJ6CAiN"
                            val type = "BUTTON"
                            val text = "No click me!"
                            val postback = "click-on-button-2"
                        }, object {
                            val id = "EyCyTRCi4"
                            val type = "BUTTON"
                            val text = "Aww don`t click on me"
                            val postback = "click-on-button-3"
                        })
                    })
                }
            }
        )

        fun TextAndButtons(threadId: UUID) = Message(
            threadId = threadId,
            content = @Suppress("unused") object {
                val type = "PLUGIN"
                val payload = object {
                    val postback = ""
                    val elements = listOf(object {
                        val id = "Ek4tPy1h4"
                        val type = "TEXT_AND_BUTTONS"
                        val elements = listOf(object {
                            val id = "Ek4tPy1h1"
                            val type = "TEXT"
                            val text = "Lorem Impsum..."
                        }, object {
                            val id = "Nkm0hRAiE"
                            val type = "BUTTON"
                            val text = "Click me!"
                            val postback = "click-on-button-1"
                        }, object {
                            val id = "NkGJ6CAiN"
                            val type = "BUTTON"
                            val text = "No click me!"
                            val postback = "click-on-button-2"
                        }, object {
                            val id = "EyCyTRCi4"
                            val type = "BUTTON"
                            val text = "Aww don`t click on me"
                            val postback = "click-on-button-3"
                        })
                    })
                }
            }
        )

        fun QuickReplies(threadId: UUID) = Message(
            threadId = threadId,
            content = @Suppress("unused") object {
                val type = "PLUGIN"
                val payload = object {
                    val elements = listOf(object {
                        val id = "Ukm0hRAiA"
                        val type = "QUICK_REPLIES"
                        val elements = listOf(object {
                            val id = "Akm0hRAiX"
                            val type = "TEXT"
                            val text = "This is some text"
                        }, object {
                            val id = "Nkm0hRAiE"
                            val type = "BUTTON"
                            val text = "Button 1"
                            val postback = "click-on-button-1"
                        }, object {
                            val id = "TkGJ6CAiN"
                            val type = "BUTTON"
                            val text = "Button 2"
                            val postback = "click-on-button-2"
                        }, object {
                            val id = "EyCyTRCi4"
                            val type = "BUTTON"
                            val text = "Button 3"
                            val postback = "click-on-button-3"
                        })
                    })
                }
            }
        )

        fun InactivityPopup(threadId: UUID) = Message(
            threadId = threadId,
            content = @Suppress("unused") object {
                val type = "PLUGIN"
                val payload = object {
                    val postback = ""
                    val elements = listOf(object {
                        val id = "Ukm0hRAiA"
                        val type = "INACTIVITY_POPUP"
                        val elements = listOf(object {
                            val id = "dbd76ae4"
                            val type = "TITLE"
                            val text = "Chat session expires"
                        }, object {
                            val id = "dbd76aeC"
                            val type = "SUBTITLE"
                            val text = "Chat session expires, but smaller"
                        }, object {
                            val id = "Akm0hRAiX"
                            val type = "TEXT"
                            val text = "Attention! Due to inactivity, your session expires in:"
                        }, object {
                            val id = "Nkm0hRAiE"
                            val type = "COUNTDOWN"
                            val variables = object {
                                val startedAt = "2021-10-26T07:52:56+0000"
                                val numberOfSeconds = 3600
                            }
                        }, object {
                            val id = "07b13436108c"
                            val type = "TEXT"
                            val text = "Would you like to continue the conversation?"
                        }, object {
                            val id = "TkGJ6CAiN"
                            val type = "BUTTON"
                            val text = "Yes"
                            val postback = "{'\''type'\'':'\''sessionExpiration'\'', '\''isExpired'\'':false, '\''workflowJobId'\'':'\''40180433-4438-4ef7-af4e-d6f69d3bda25'\''}"
                        }, object {
                            val id = "EyCyTRCi4"
                            val type = "BUTTON"
                            val text = "No"
                            val postback = "{'\''type'\'':'\''sessionExpiration'\'', '\''isExpired'\'':true, '\''workflowJobId'\'':'\''40180433-4438-4ef7-af4e-d6f69d3bda25'\''}"
                        })
                    })
                }
            }
        )

        fun Custom(threadId: UUID) = Message(
            threadId = threadId,
            content = @Suppress("unused") object {
                val type = "PLUGIN"
                val payload = object {
                    val postback = ""
                    val elements = listOf(object {
                        val id = "Nkm0hRAiE"
                        val type = "CUSTOM"
                        val text = "See this page"
                        val variables = object {
                            val color = "green"
                            val buttons = listOf(
                                object {
                                    val id = "0edc9bf6-4922-4695-a6ad-1bdb248dd42f"
                                    val name = "Confirm"
                                },
                                object {
                                    val id = "0b4ad5a5-5f6b-477d-8013-d6dcf7b87704"
                                    val name = "Decline"
                                }
                            )
                            val size = object {
                                val ios = "big"
                                val android = "middle"
                            }
                        }
                    })
                }
            }
        )

        fun Gallery(threadId: UUID) = Message(
            threadId = threadId,
            content = @Suppress("unused") object {
                val type = "PLUGIN"
                val payload = object {
                    val postback = ""
                    val elements = listOf(
                        object {
                            val id = "Ek4tPy1h41"
                            val type = "MENU"
                            val elements = listOf(
                                object {
                                    val id = "Uk4tPy1h21"
                                    val type = "FILE"
                                    val url = "https://picsum.photos/300/150"
                                    val filename = "photo1.jpeg"
                                    val mimeType = "image/jpeg"
                                },
                                object {
                                    val id = "Ck4tPy1h31"
                                    val type = "TITLE"
                                    val text = "Hello!"
                                },
                                object {
                                    val id = "Ek4tPy1h11"
                                    val type = "TEXT"
                                    val text = "Lorem Impsum..."
                                },
                                object {
                                    val id = "Nkm0hRAiE1"
                                    val type = "BUTTON"
                                    val text = "Click me!"
                                    val postback = "click-on-button-1"
                                },
                                object {
                                    val id = "NkGJ6CAiN1"
                                    val type = "BUTTON"
                                    val text = "No click me!"
                                    val postback = "click-on-button-2"
                                },
                                object {
                                    val id = "EyCyTRCi41"
                                    val type = "BUTTON"
                                    val text = "Aww don`t click on me"
                                    val postback = "click-on-button-3"
                                }
                            )
                        },
                        object {
                            val id = "Ek4tPy1h52"
                            val type = "MENU"
                            val elements = listOf(
                                object {
                                    val id = "Uk4tPy1h22"
                                    val type = "FILE"
                                    val url = "https://picsum.photos/300/150"
                                    val filename = "photo1.jpeg"
                                    val mimeType = "image/jpeg"
                                },
                                object {
                                    val id = "Ck4tPy1h32"
                                    val type = "TITLE"
                                    val text = "Hello!"
                                },
                                object {
                                    val id = "Ek4tPy1h12"
                                    val type = "TEXT"
                                    val text = "Lorem Impsum..."
                                },
                                object {
                                    val id = "Nkm0hRAiE2"
                                    val type = "BUTTON"
                                    val text = "Click me!"
                                    val postback = "click-on-button-1"
                                },
                                object {
                                    val id = "NkGJ6CAiN2"
                                    val type = "BUTTON"
                                    val text = "No click me!"
                                    val postback = "click-on-button-2"
                                },
                                object {
                                    val id = "EyCyTRCi42"
                                    val type = "BUTTON"
                                    val text = "Aww don`t click on me"
                                    val postback = "click-on-button-3"
                                }
                            )
                        },
                        object {
                            val id = "Ek4tPy1h63"
                            val type = "MENU"
                            val elements = listOf(
                                object {
                                    val id = "Uk4tPy1h23"
                                    val type = "FILE"
                                    val url = "https://picsum.photos/300/150"
                                    val filename = "photo1.jpeg"
                                    val mimeType = "image/jpeg"
                                },
                                object {
                                    val id = "Ck4tPy1h33"
                                    val type = "TITLE"
                                    val text = "Hello!"
                                },
                                object {
                                    val id = "Ek4tPy1h13"
                                    val type = "TEXT"
                                    val text = "Lorem Impsum..."
                                },
                                object {
                                    val id = "Nkm0hRAiE3"
                                    val type = "BUTTON"
                                    val text = "Click me!"
                                    val postback = "click-on-button-1"
                                },
                                object {
                                    val id = "NkGJ6CAiN3"
                                    val type = "BUTTON"
                                    val text = "No click me!"
                                    val postback = "click-on-button-2"
                                },
                                object {
                                    val id = "EyCyTRCi43"
                                    val type = "BUTTON"
                                    val text = "Aww don`t click on me"
                                    val postback = "click-on-button-3"
                                }
                            )
                        }
                    )
                }
            }
        )

        fun SatisfactionSurveyInternal(threadId: UUID) = Message(
            threadId = threadId,
            content = @Suppress("unused") object {
                val type = "PLUGIN"
                val payload = object {
                    val text = ""
                    val postback = ""
                    val elements = listOf(object {
                        val id = "0ddf0614-8d82-4117-bfbe-5f42ffe46948"
                        val type = "SATISFACTION_SURVEY"
                        val elements = listOf(
                            object {
                                val type = "TEXT"
                                val id = "b4d315be-725d-44bc-8b92-6bd42fe6aeb2"
                                val text = "Satisfaction survey message"
                                val mimeType = "text/plain"
                            },
                            object {
                                val id = "7e8bb08a-5f4e-4e65-8b11-7f6e5d258fa9"
                                val type = "IFRAME_BUTTON"
                                val text = "Satisfaction survey button"
                                val url = "https://my-satisfaction-survey.com/will-smith"
                            }
                        )
                    })
                }
            }
        )

        fun SatisfactionSurveyExternal(threadId: UUID) = Message(
            threadId = threadId,
            content = @Suppress("unused") object {
                val type = "PLUGIN"
                val payload = object {
                    val text = ""
                    val postback = ""
                    val elements = listOf(object {
                        val id = "0ddf0614-8d82-4117-bfbe-5f42ffe46948"
                        val type = "SATISFACTION_SURVEY"
                        val elements = listOf(
                            object {
                                val type = "TEXT"
                                val id = "b4d315be-725d-44bc-8b92-6bd42fe6aeb2"
                                val text = "Satisfaction survey message"
                                val mimeType = "text/plain"
                            },
                            object {
                                val id = "7e8bb08a-5f4e-4e65-8b11-7f6e5d258fa9"
                                val type = "BUTTON"
                                val text = "Satisfaction survey button"
                                val url = "https://my-satisfaction-survey.com/will-smith"
                            }
                        )
                    })
                }
            }
        )

        fun InvalidSatisfactionSurvey(threadId: UUID) = Message(
            threadId = threadId,
            content = @Suppress("unused") object {
                val type = "PLUGIN"
                val payload = object {
                    val text = ""
                    val postback = ""
                    val elements = listOf(object {
                        val id = "0ddf0614-8d82-4117-bfbe-5f42ffe46948"
                        val type = "SATISFACTION_SURVEY"
                        val elements = listOf(
                            object {
                                val id = "Ck4tPy1h32"
                                val type = "TITLE"
                                val text = "Hello!"
                            }
                        )
                    })
                }
            }
        )

        fun InvalidContent(threadId: UUID) = Message(
            threadId = threadId,
            content = @Suppress("unused") object {
                val type = "FOOBAR"
            }
        )

        fun InvalidPlugin(threadId: UUID) = Message(
            threadId = threadId,
            content = @Suppress("unused") object {
                val type = "PLUGIN"
                val payload = object {
                    val postback = ""
                    val elements = listOf(object {
                        val id = "1234"
                        val type = "FOOBAR"
                    })
                }
            }
        )
    }
}
