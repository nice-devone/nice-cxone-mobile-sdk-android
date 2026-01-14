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

package com.nice.cxonechat.sample.viewModel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nice.cxonechat.sample.data.models.ExtraCustomFields
import com.nice.cxonechat.sample.data.repository.ExtraCustomFieldRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel

/**
 * ViewModel for managing extra custom fields for customers and contacts.
 *
 * @param application The application context.
 * @param extraCustomFieldRepository Repository for managing extra custom fields data.
 */
@KoinViewModel
class ExtraCustomFieldsViewModel internal constructor(
    application: Application,
    private val extraCustomFieldRepository: ExtraCustomFieldRepository,
) : AndroidViewModel(application) {
    private val extraCustomerFieldsMutableFlow =
        MutableStateFlow(emptyMap<String, String>())
    private val extraContactFieldsMutableFlow =
        MutableStateFlow(emptyMap<String, String>())

    /** Flow that emits the current state of extra customer fields. */
    val extraCustomerFieldsFlow = extraCustomerFieldsMutableFlow.asStateFlow()

    /** Flow that emits the current state of extra contact fields. */
    val extraContactFieldsFlow = extraContactFieldsMutableFlow.asStateFlow()

    init {
        // Initialize the ViewModel by loading custom fields from the repository.
        viewModelScope.launch(Dispatchers.IO) {
            val fields = extraCustomFieldRepository.load()
            extraCustomerFieldsMutableFlow.value = fields.customerCustomFields
            extraContactFieldsMutableFlow.value = fields.contactCustomFields
        }
    }

    /**
     * Adds or updates a customer custom field.
     *
     * @param key The key of the custom field.
     * @param value The value of the custom field.
     */
    fun setCustomerCustomField(key: String, value: String) {
        extraCustomerFieldsMutableFlow.value = extraCustomerFieldsMutableFlow.value + (key to value)
    }

    /**
     * Adds or updates a contact custom field.
     *
     * @param key The key of the custom field.
     * @param value The value of the custom field.
     */
    fun setContactCustomField(key: String, value: String) {
        extraContactFieldsMutableFlow.value = extraContactFieldsMutableFlow.value + (key to value)
    }

    /**
     * Removes a customer custom field.
     *
     * @param key The key of the custom field to remove.
     */
    fun removeCustomerCustomField(key: String) {
        extraCustomerFieldsMutableFlow.value = extraCustomerFieldsMutableFlow.value - key
    }

    /**
     * Removes a contact custom field.
     *
     * @param key The key of the custom field to remove.
     */
    fun removeContactCustomField(key: String) {
        extraContactFieldsMutableFlow.value = extraContactFieldsMutableFlow.value - key
    }

    /**
     * Saves the current custom fields to the repository.
     */
    fun save() {
        viewModelScope.launch(Dispatchers.IO) {
            extraCustomFieldRepository.save(
                context = getApplication(),
                item = ExtraCustomFields(
                    customerCustomFields = extraCustomerFieldsMutableFlow.value,
                    contactCustomFields = extraContactFieldsMutableFlow.value,
                ),
            )
        }
    }
}
