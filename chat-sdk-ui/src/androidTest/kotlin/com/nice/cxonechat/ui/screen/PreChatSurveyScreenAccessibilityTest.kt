/*
 * Copyright (c) 2021-2026. NICE Ltd. All rights reserved.
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

package com.nice.cxonechat.ui.screen

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nice.cxonechat.prechat.PreChatSurvey
import com.nice.cxonechat.state.FieldDefinition
import com.nice.cxonechat.state.FieldDefinitionList
import com.nice.cxonechat.ui.AbstractComponentActivityUiTest
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@RunWith(AndroidJUnit4::class)
internal class PreChatSurveyScreenAccessibilityTest : AbstractComponentActivityUiTest() {

    @Test
    fun preChatSurveyScreen_hasCorrectTestTag() {
        val mockSurvey = createMockPreChatSurvey("Test Survey", listOf(createMockTextField()))
        composeTestRule.setContent {
            ChatTheme {
                PreChatSurveyScreen(
                    survey = mockSurvey,
                    onCancel = {},
                    onValidSurveySubmission = {}
                )
            }
        }

        // Verify main screen container exists and is displayed
        composeTestRule.onNodeWithTag("prechat_survey").assertIsDisplayed()
    }

    @Test
    fun preChatSurveyScreen_fieldsContainer_hasAccessibleStructure() {
        val fieldLabel = "Full Name"
        val mockSurvey = createMockPreChatSurvey("Survey", listOf(createMockTextField(label = fieldLabel)))

        composeTestRule.setContent {
            ChatTheme {
                PreChatSurveyScreen(
                    survey = mockSurvey,
                    onCancel = {},
                    onValidSurveySubmission = {}
                )
            }
        }

        // Verify the field is accessible using its test tag
        // Text field test tags follow pattern: "custom_value_text_${fieldLabel}"
        composeTestRule.onNodeWithTag("custom_value_text_$fieldLabel").assertIsDisplayed()
    }

    @Test
    fun preChatSurveyScreen_requiredFields_areAccessible() {
        val requiredFieldLabel = "Email Address"
        val mockSurvey = createMockPreChatSurvey(
            "Survey",
            listOf(createMockTextField(label = requiredFieldLabel, isRequired = true))
        )

        composeTestRule.setContent {
            ChatTheme {
                PreChatSurveyScreen(
                    survey = mockSurvey,
                    onCancel = {},
                    onValidSurveySubmission = {}
                )
            }
        }

        // Verify required field is displayed using its test tag
        // Test tags for text fields follow pattern: "custom_value_text_${fieldLabel}"
        composeTestRule.onNodeWithTag("custom_value_text_$requiredFieldLabel").assertIsDisplayed()
    }

    @Test
    fun preChatSurveyScreen_submitButton_isAccessible_withOptionalFields() {
        val mockSurvey = createMockPreChatSurvey(
            "Survey",
            listOf(createMockTextField(label = "Optional Field", isRequired = false))
        )

        composeTestRule.setContent {
            ChatTheme {
                PreChatSurveyScreen(
                    survey = mockSurvey,
                    onCancel = {},
                    onValidSurveySubmission = {}
                )
            }
        }

        // Submit button should be enabled when all optional fields are present
        composeTestRule.onNodeWithTag("submit_button").assertIsEnabled()
    }

    @Test
    fun preChatSurveyScreen_submitButton_withRequiredFields_isInitiallyDisabled() {
        val mockSurvey = createMockPreChatSurvey(
            "Survey",
            listOf(
                createMockTextField(label = "Required Field", isRequired = true)
            )
        )

        composeTestRule.setContent {
            ChatTheme {
                PreChatSurveyScreen(
                    survey = mockSurvey,
                    onCancel = {},
                    onValidSurveySubmission = {}
                )
            }
        }

        // Submit button should be disabled when required fields are empty
        composeTestRule.onNodeWithTag("submit_button").assertIsNotEnabled()
    }

    @Test
    fun preChatSurveyScreen_cancelButton_isAccessible() {
        val mockSurvey = createMockPreChatSurvey("Survey", listOf(createMockTextField()))

        composeTestRule.setContent {
            ChatTheme {
                PreChatSurveyScreen(
                    survey = mockSurvey,
                    onCancel = {},
                    onValidSurveySubmission = {}
                )
            }
        }

        // Verify cancel button is displayed and enabled
        composeTestRule.onNodeWithTag("cancel_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("cancel_button").assertIsEnabled()
    }

    @Test
    fun preChatSurveyScreen_interactiveElements_haveSemantics() {
        val fieldLabel = "Interactive Field"
        val mockSurvey = createMockPreChatSurvey(
            "Survey",
            listOf(createMockTextField(label = fieldLabel))
        )

        composeTestRule.setContent {
            ChatTheme {
                PreChatSurveyScreen(
                    survey = mockSurvey,
                    onCancel = {},
                    onValidSurveySubmission = {}
                )
            }
        }

        // Verify that the survey screen is rendered with proper structure
        composeTestRule.onNodeWithTag("prechat_survey").assertIsDisplayed()

        // Verify field is accessible using its test tag
        composeTestRule.onNodeWithTag("custom_value_text_$fieldLabel").assertIsDisplayed()

        // Verify buttons are available for interaction
        composeTestRule.onNodeWithTag("cancel_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("submit_button").assertIsDisplayed()
    }

    @Test
    fun preChatSurveyScreen_hasMinimumAccessibilityRequirements() {
        val surveyName = "Customer Survey"
        val firstNameLabel = "First Name"
        val lastNameLabel = "Last Name"
        val mockSurvey = createMockPreChatSurvey(
            surveyName,
            listOf(
                createMockTextField(label = firstNameLabel),
                createMockTextField(label = lastNameLabel)
            )
        )

        composeTestRule.setContent {
            ChatTheme {
                PreChatSurveyScreen(
                    survey = mockSurvey,
                    onCancel = {},
                    onValidSurveySubmission = {}
                )
            }
        }

        // 1. All form fields are displayed using their test tags
        composeTestRule.onNodeWithTag("custom_value_text_$firstNameLabel").assertIsDisplayed()
        composeTestRule.onNodeWithTag("custom_value_text_$lastNameLabel").assertIsDisplayed()

        // 2. Interactive buttons are available
        composeTestRule.onNodeWithTag("cancel_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("submit_button").assertIsDisplayed()
    }

    /**
     * Creates a mock PreChatSurvey for testing.
     *
     * @param name The name of the survey
     * @param fields The list of field definitions
     * @return A mock PreChatSurvey instance
     */
    private fun createMockPreChatSurvey(name: String, fields: List<FieldDefinition>): PreChatSurvey {
        return object : PreChatSurvey {
            override val name: String = name
            override val fields: FieldDefinitionList = fields.asSequence()
        }
    }

    /**
     * Creates a mock Text field definition for testing.
     *
     * @param label The label for the field
     * @param isRequired Whether the field is required
     * @return A mock FieldDefinition.Text instance
     */
    private fun createMockTextField(
        label: String = "Test Field",
        isRequired: Boolean = false,
    ): FieldDefinition.Text {
        return object : FieldDefinition.Text {
            override val fieldId: String = UUID.randomUUID().toString()
            override val label: String = label
            override val isRequired: Boolean = isRequired
            override val isEMail: Boolean = false

            override fun validate(value: String) {
                // No-op validation for mock - real implementation would validate email format
            }
        }
    }
}
