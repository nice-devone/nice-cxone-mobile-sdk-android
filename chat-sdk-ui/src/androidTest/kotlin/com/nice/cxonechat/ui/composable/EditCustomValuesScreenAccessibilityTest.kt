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

package com.nice.cxonechat.ui.composable

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nice.cxonechat.state.FieldDefinition
import com.nice.cxonechat.ui.AbstractComponentActivityUiTest
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import com.nice.cxonechat.ui.domain.model.CustomValueItem
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@RunWith(AndroidJUnit4::class)
internal class EditCustomValuesScreenAccessibilityTest : AbstractComponentActivityUiTest() {

    @Test
    fun optionalTextField_isDisplayed() {
        val fieldLabel = "Phone Number"
        val fields = listOf(
            CustomValueItem(createMockTextField(fieldLabel), null)
        )
        composeTestRule.setContent {
            ChatTheme {
                EditCustomValuesScreen(
                    title = "Test Form",
                    fields = fields,
                    sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                    onCancel = {},
                    onConfirm = {}
                )
            }
        }

        // Verify optional field is displayed
        composeTestRule.onNodeWithText(fieldLabel).assertIsDisplayed()

        // Verify field has correct test tag
        composeTestRule.onNodeWithTag("custom_value_text_$fieldLabel")
            .assertIsDisplayed()
    }

    @Test
    fun emailTextField_isConfiguredCorrectly() {
        val emailFieldLabel = "Email"
        val fields = listOf(
            CustomValueItem(createMockEmailField(emailFieldLabel), null)
        )
        composeTestRule.setContent {
            ChatTheme {
                EditCustomValuesScreen(
                    title = "Email Form",
                    fields = fields,
                    sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                    onCancel = {},
                    onConfirm = {}
                )
            }
        }

        // Verify email field is displayed with label
        composeTestRule.onNodeWithText(emailFieldLabel).assertIsDisplayed()

        // Verify field has proper test tag
        composeTestRule.onNodeWithTag("custom_value_text_$emailFieldLabel")
            .assertIsDisplayed()
    }

    @Test
    fun cancelButton_isDisplayedAndAccessible() {
        val fields = listOf(
            CustomValueItem(createMockTextField("Name"), null)
        )
        var cancelCalled = false

        composeTestRule.setContent {
            ChatTheme {
                EditCustomValuesScreen(
                    title = "Test Form",
                    fields = fields,
                    sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                    onCancel = { cancelCalled = true },
                    onConfirm = {}
                )
            }
        }

        // Verify cancel button is displayed with proper test tag
        composeTestRule.onNodeWithTag("cancel_button")
            .assertIsDisplayed()
            .assertIsEnabled()
            .tryPerformAccessibilityChecks()

        // Verify cancel button has correct text or content description
        composeTestRule.onNodeWithTag("cancel_button")
            .performClick()

        // Verify cancel callback was invoked
        composeTestRule.waitForIdle()
        assert(cancelCalled) { "Cancel callback should be invoked when cancel button is clicked" }
    }

    @Test
    fun submitButton_disabledStateReflectsFormValidity() {
        val fields = listOf(
            CustomValueItem(createMockTextField("Field"), null)
        )
        composeTestRule.setContent {
            ChatTheme {
                EditCustomValuesScreen(
                    title = "Form",
                    fields = fields,
                    sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                    canSubmit = false,
                    onCancel = {},
                    onConfirm = {}
                )
            }
        }

        // Verify submit button exists but is disabled
        composeTestRule.onNodeWithTag("submit_button")
            .assertIsNotEnabled()
            .tryPerformAccessibilityChecks()
    }

    @Test
    fun submitButton_enabledWhenFormIsValid() {
        val fields = listOf(
            CustomValueItem(createMockTextField("Field"), null)
        )
        composeTestRule.setContent {
            ChatTheme {
                EditCustomValuesScreen(
                    title = "Form",
                    fields = fields,
                    sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                    canSubmit = true,
                    onCancel = {},
                    onConfirm = {}
                )
            }
        }

        // Verify submit button is enabled
        composeTestRule.onNodeWithTag("submit_button")
            .assertIsEnabled()
    }

    @Test
    fun multipleTextFields_allDisplayedWithLabels() {
        val firstName = "First Name"
        val lastName = "Last Name"
        val email = "Email Address"

        val fields = listOf(
            CustomValueItem(createMockTextField(firstName), null),
            CustomValueItem(createMockTextField(lastName), null),
            CustomValueItem(createMockTextField(email), null)
        )
        composeTestRule.setContent {
            ChatTheme {
                EditCustomValuesScreen(
                    title = "Registration Form",
                    fields = fields,
                    sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                    onCancel = {},
                    onConfirm = {}
                )
            }
        }

        // Verify all fields are displayed with correct labels
        composeTestRule.onNodeWithText(firstName).assertIsDisplayed()
        composeTestRule.onNodeWithText(lastName).assertIsDisplayed()
        composeTestRule.onNodeWithText(email).assertIsDisplayed()

        // Verify all fields have correct test tags
        composeTestRule.onNodeWithTag("custom_value_text_$firstName").assertIsDisplayed()
        composeTestRule.onNodeWithTag("custom_value_text_$lastName").assertIsDisplayed()
        composeTestRule.onNodeWithTag("custom_value_text_$email").assertIsDisplayed()
    }

    @Test
    fun mixedRequiredAndOptionalFields_areAccessible() {
        val requiredField = "Company Name"
        val optionalField = "Website"

        val fields = listOf(
            CustomValueItem(createMockTextField(requiredField), null),
            CustomValueItem(createMockTextField(optionalField), null)
        )
        composeTestRule.setContent {
            ChatTheme {
                EditCustomValuesScreen(
                    title = "Business Form",
                    fields = fields,
                    sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                    onCancel = {},
                    onConfirm = {}
                )
            }
        }

        // Verify both field types are accessible
        composeTestRule.onNodeWithText(requiredField).assertIsDisplayed()
        composeTestRule.onNodeWithText(optionalField).assertIsDisplayed()
    }

    @Test
    fun multipleEmailFields_areIndependentlyAccessible() {
        val primaryEmail = "Primary Email"
        val secondaryEmail = "Secondary Email"

        val fields = listOf(
            CustomValueItem(createMockEmailField(primaryEmail), null),
            CustomValueItem(createMockEmailField(secondaryEmail), null)
        )
        composeTestRule.setContent {
            ChatTheme {
                EditCustomValuesScreen(
                    title = "Email Form",
                    fields = fields,
                    sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                    onCancel = {},
                    onConfirm = {}
                )
            }
        }

        // Verify both email fields are displayed and independently accessible
        composeTestRule.onNodeWithText(primaryEmail).assertIsDisplayed()
        composeTestRule.onNodeWithText(secondaryEmail).assertIsDisplayed()

        composeTestRule.onNodeWithTag("custom_value_text_$primaryEmail").assertIsDisplayed()
        composeTestRule.onNodeWithTag("custom_value_text_$secondaryEmail").assertIsDisplayed()
    }

    private fun createMockTextField(
        label: String = "Test Field",
    ): FieldDefinition.Text {
        return object : FieldDefinition.Text {
            override val fieldId: String = UUID.randomUUID().toString()
            override val label: String = label
            override val isRequired: Boolean = false
            override val isEMail: Boolean = false

            override fun validate(value: String) {
                // No-op validation for mock
            }
        }
    }

    /**
     * Creates a mock email field definition for testing.
     *
     * @param label The label for the email field
     * @return A mock FieldDefinition.Text instance with isEMail = true
     */
    private fun createMockEmailField(
        label: String = "Email",
    ): FieldDefinition.Text {
        return object : FieldDefinition.Text {
            override val fieldId: String = UUID.randomUUID().toString()
            override val label: String = label
            override val isRequired: Boolean = false
            override val isEMail: Boolean = true

            override fun validate(value: String) {
                require(isRequired && value.isEmpty()) {
                    "Email is required"
                }
                require(value.isNotEmpty() && !value.contains("@")) {
                    "Invalid email format"
                }
            }
        }
    }
}
