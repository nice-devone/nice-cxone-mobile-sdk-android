package com.nice.cxonechat.prechat

import com.nice.cxonechat.Public
import com.nice.cxonechat.state.FieldDefinition
import com.nice.cxonechat.state.HierarchyNode
import com.nice.cxonechat.state.SelectorNode

/**
 * Definition of possible response formats for questions from [PreChatSurvey].
 * Each format is tied to the question represented by the [FieldDefinition] from [PreChatSurvey.fields].
 * Format definitions provide default minimal implementation, but allows also usage of custom implementations.
 */
@Public
sealed interface PreChatSurveyResponse<T : FieldDefinition, R> {
    /**
     * Question which is being answered.
     */
    val question: T

    /**
     * Supplied / selected response from the user.
     */
    val response: R

    /**
     * Response to [FieldDefinition.Text] with response in a format of user supplied [String].
     */
    @Public
    interface Text : PreChatSurveyResponse<FieldDefinition.Text, String> {
        @Public
        companion object {
            /**
             * Creates instance of [Text] survey response.
             *
             * @param question [FieldDefinition.Text] survey question which is being answered.
             * @param response The user provided text response to the survey.
             * An empty String is considered as an invalid answer, and the whole response will be later discarded.
             */
            @JvmName("create")
            @JvmStatic
            operator fun invoke(
                question: FieldDefinition.Text,
                response: String,
            ) = object : Text {
                override val question = question
                override val response = response
            }
        }
    }

    /**
     * Response to [FieldDefinition.Selector] with response in a format of selected [Selector].
     */
    @Public
    interface Selector : PreChatSurveyResponse<FieldDefinition.Selector, SelectorNode> {
        @Public
        companion object {
            /**
             * Creates instance of [Selector] survey response.
             *
             * @param question [FieldDefinition.Selector] survey question which is being answered.
             * @param response User selected [SelectorNode] instance as a response to the survey.
             */
            @JvmName("create")
            @JvmStatic
            operator fun invoke(
                question: FieldDefinition.Selector,
                response: SelectorNode,
            ) = object : Selector {
                override val question = question
                override val response = response
            }
        }
    }

    /**
     * Response to [FieldDefinition.Hierarchy] with response in a format of selected leaf [HierarchyNode].
     */
    @Public
    interface Hierarchy : PreChatSurveyResponse<FieldDefinition.Hierarchy, HierarchyNode<String>> {
        @Public
        companion object {
            /**
             * Creates instance of [Hierarchy] survey response.
             *
             * @param question [FieldDefinition.Hierarchy] survey question which is being answered.
             * @param response User selected leaf [HierarchyNode] as a response to the survey.
             */
            @JvmName("create")
            @JvmStatic
            operator fun invoke(
                question: FieldDefinition.Hierarchy,
                response: HierarchyNode<String>
            ) = object : Hierarchy {
                override val question: FieldDefinition.Hierarchy = question
                override val response: HierarchyNode<String> = response
            }
        }
    }
}
