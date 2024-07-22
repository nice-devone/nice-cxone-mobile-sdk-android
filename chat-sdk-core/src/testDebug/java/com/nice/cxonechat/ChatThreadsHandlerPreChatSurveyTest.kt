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

@file:Suppress("FunctionMaxLength")

package com.nice.cxonechat

import com.nice.cxonechat.exceptions.MissingPreChatCustomFieldsException
import com.nice.cxonechat.internal.model.ChannelConfiguration
import com.nice.cxonechat.internal.model.ChannelIdentifier
import com.nice.cxonechat.internal.model.CustomFieldPolyType
import com.nice.cxonechat.internal.model.CustomFieldPolyType.Hierarchy
import com.nice.cxonechat.internal.model.CustomFieldPolyType.Text
import com.nice.cxonechat.internal.model.PreContactCustomFieldDefinitionModel
import com.nice.cxonechat.internal.model.PreContactFormModel
import com.nice.cxonechat.internal.model.SelectorModel
import com.nice.cxonechat.model.nextNode
import com.nice.cxonechat.prechat.PreChatSurveyResponse
import com.nice.cxonechat.state.Connection
import com.nice.cxonechat.state.FieldDefinition
import com.nice.cxonechat.state.HierarchyNode
import com.nice.cxonechat.tool.SocketFactoryMock
import com.nice.cxonechat.tool.awaitResult
import com.nice.cxonechat.tool.nextString
import org.junit.Test
import java.util.UUID
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

internal class ChatThreadsHandlerPreChatSurveyTest : AbstractChatTest() {

    private var channels = listOf(
        ChannelIdentifier(SocketFactoryMock.channelId)
    )

    private var textQuestionId = nextString()
    private var hierarchyQuestionId = nextString()

    private var preChatSurveys = listOf(
        PreContactCustomFieldDefinitionModel(
            isRequired = true,
            definition = CustomFieldPolyType.Text(
                fieldId = textQuestionId,
                label = "Text test",
            )
        )
    )

    private var fields = listOf<CustomFieldPolyType>(
        Text(textQuestionId, "text"),
        Hierarchy(hierarchyQuestionId, "hier", listOf())
    )

    override val config: ChannelConfiguration
        get() {
            val config = super.config.let(::requireNotNull)
            return config.copy(
                preContactForm = PreContactFormModel(
                    name = "pre contact survey test",
                    channels = channels,
                    customFields = preChatSurveys
                ),
                contactCustomFields = fields
            )
        }

    @Test(
        expected = MissingPreChatCustomFieldsException::class
    )
    fun create_throws_withoutRequiredPreChatResponses() {
        val threads = chat.threads()
        threads.create()
    }

    @Test
    fun create_withRequiredPreChatResponse_Text() {
        val threads = chat.threads()
        val survey = threads.preChatSurvey
        assertNotNull(survey)
        val response = PreChatSurveyResponse.Text(
            question = survey.fields.first() as FieldDefinition.Text,
            response = nextString()
        )
        threads.create(sequenceOf(response))
    }

    @Test(
        expected = MissingPreChatCustomFieldsException::class
    )
    fun create_throws_withRequiredPreChatResponse_Text_with_empty_value() {
        val threads = chat.threads()
        val survey = threads.preChatSurvey
        assertNotNull(survey)
        val response = PreChatSurveyResponse.Text(
            question = survey.fields.first() as FieldDefinition.Text,
            response = ""
        )
        threads.create(sequenceOf(response))
    }

    @Test(
        expected = MissingPreChatCustomFieldsException::class
    )
    fun create_throws_withRequiredPreChatResponse_Hierarchy_with_nonLeaf_node() {
        val rootNode = nextNode()
        val branchNode = nextNode(rootNode.name)
        val leafNode = nextNode(branchNode.name)
        preChatSurveys = listOf(
            PreContactCustomFieldDefinitionModel(
                isRequired = true,
                definition = Hierarchy(
                    fieldId = hierarchyQuestionId,
                    label = "Hierarchy test",
                    values = listOf(
                        branchNode,
                        leafNode,
                        rootNode,
                    )
                )
            )
        )
        val threads = build().threads()
        val survey = threads.preChatSurvey
        assertNotNull(survey)
        val question = survey.fields.first() as FieldDefinition.Hierarchy
        val node = question.values.first()
        assertTrue(!node.isLeaf)
        val response = PreChatSurveyResponse.Hierarchy(
            question = question,
            response = node
        )
        threads.create(sequenceOf(response))
    }

    @Test
    fun create_withRequiredPreChatResponse_Hierarchy_with_leaf_node() {
        val rootNode = nextNode()
        val branchNode = nextNode(rootNode.name)
        val leafNode = nextNode(branchNode.name)
        preChatSurveys = listOf(
            PreContactCustomFieldDefinitionModel(
                isRequired = true,
                definition = Hierarchy(
                    fieldId = UUID.randomUUID().toString(),
                    label = "Hierarchy test",
                    values = listOf(
                        branchNode,
                        leafNode,
                        rootNode,
                    )
                )
            )
        )
        val threads = build().threads()
        val survey = threads.preChatSurvey
        assertNotNull(survey)
        val question = survey.fields.first() as FieldDefinition.Hierarchy
        val response = PreChatSurveyResponse.Hierarchy(
            question = question,
            response = findLeaf(question.values.first())
        )
        threads.create(sequenceOf(response))
    }

    @Test
    fun create_withRequiredPreChatResponse_Selector() {
        preChatSurveys = listOf(
            PreContactCustomFieldDefinitionModel(
                isRequired = true,
                definition = CustomFieldPolyType.Selector(
                    fieldId = nextString(),
                    label = "Selector test",
                    values = listOf(
                        SelectorModel(
                            name = nextString(),
                            label = "1"
                        ),
                        SelectorModel(
                            name = nextString(),
                            label = "2"
                        )
                    )
                )
            )
        )
        val threads = build().threads()
        val survey = threads.preChatSurvey
        assertNotNull(survey)
        val question = survey.fields.first() as FieldDefinition.Selector
        val response = PreChatSurveyResponse.Selector(
            question = question,
            response = question.values.first()
        )
        threads.create(sequenceOf(response))
    }

    @Test
    fun create_withRequiredPreChatResponse_Email() {
        preChatSurveys = listOf(
            PreContactCustomFieldDefinitionModel(
                isRequired = false,
                definition = CustomFieldPolyType.Email(
                    fieldId = nextString(),
                    label = "Email test",
                )
            )
        )
        val threads = build().threads()
        val survey = threads.preChatSurvey
        assertNotNull(survey)
        val response = PreChatSurveyResponse.Text(
            question = survey.fields.first() as FieldDefinition.Text,
            response = "foo@bar.com"
        )
        threads.create(sequenceOf(response))
    }

    @Test
    fun create_withOptionalPreChatResponse_Email_no_response() {
        preChatSurveys = listOf(
            PreContactCustomFieldDefinitionModel(
                isRequired = false,
                definition = CustomFieldPolyType.Email(
                    fieldId = nextString(),
                    label = "Email test",
                )
            )
        )
        val threads = build().threads()
        val survey = threads.preChatSurvey
        assertNotNull(survey)
        threads.create()
    }

    @Test
    fun null_survey_for_Noop() {
        preChatSurveys = listOf(
            PreContactCustomFieldDefinitionModel(
                isRequired = false,
                definition = CustomFieldPolyType.Noop
            )
        )
        val threads = build().threads()
        val survey = threads.preChatSurvey
        assertNull(survey)
        threads.create()
    }

    private fun prepareBuilder(): Pair<Connection, ChatBuilder> {
        val factory = SocketFactoryMock(socket, proxyListener)
        val connection = factory.getConfiguration(storage)
        return connection to ChatBuilder(entrails, factory)
    }

    @Suppress("DEPRECATED", "DEPRECATION")
    private fun build(
        builder: ChatBuilder = prepareBuilder().second,
        body: ChatBuilder.() -> ChatBuilder = { this },
    ): Chat = awaitResult {
        builder
            .setDevelopmentMode(true)
            .body()
            .build(it)
    }

    private tailrec fun findLeaf(node: HierarchyNode<String>): HierarchyNode<String> =
        if (node.isLeaf) node else findLeaf(node.children.first())
}
