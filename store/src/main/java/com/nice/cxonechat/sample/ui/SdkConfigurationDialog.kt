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

package com.nice.cxonechat.sample.ui

import android.content.Context
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nice.cxonechat.enums.CXOneEnvironment
import com.nice.cxonechat.sample.R.string
import com.nice.cxonechat.sample.data.models.SdkConfiguration
import com.nice.cxonechat.sample.data.models.SdkConfigurations
import com.nice.cxonechat.sample.data.models.asSdkEnvironment
import com.nice.cxonechat.sample.data.repository.SdkConfigurationListRepository
import com.nice.cxonechat.sample.ui.components.DropdownField
import com.nice.cxonechat.sample.ui.components.DropdownItem
import com.nice.cxonechat.sample.ui.theme.AppTheme
import com.nice.cxonechat.sample.ui.theme.AppTheme.space
import com.nice.cxonechat.sample.ui.theme.Dialog
import com.nice.cxonechat.sample.ui.theme.OutlinedButton
import com.nice.cxonechat.sample.ui.theme.TextField
import com.nice.cxonechat.sample.utilities.Requirements.allOf
import com.nice.cxonechat.sample.utilities.Requirements.integer
import com.nice.cxonechat.sample.utilities.Requirements.required
import kotlinx.coroutines.runBlocking

/**
 * Composable dialog to allow an SDK configuration to be picked from a predefined
 * list or defined from it's component elements.
 *
 * @param configuration Current configuration, if any.
 * @param configurations List of predefined configurations available.
 * @param onDismiss Function to dismiss the dialog, if it's allowed.
 * @param onConfigurationSelected Callback when the user accepts a configuration.
 */
@Composable
fun SdkConfigurationDialog(
    configuration: SdkConfiguration?,
    configurations: SdkConfigurations,
    onDismiss: () -> Unit,
    onConfigurationSelected: (SdkConfiguration) -> Unit,
) {
    val context = LocalContext.current
    val state = remember { SdkConfigurationState(context, configuration, configurations) }
    val builtConfiguration = if (state.validate()) {
        state.build()
    } else {
        null
    }

    AppTheme.Dialog(
        modifier = Modifier.wrapContentHeight(),
        onDismiss = if (state.configuration != null) {
            onDismiss
        } else {
            {}
        },
        title = stringResource(string.sdk_configuration),
        confirmButton = {
            AppTheme.OutlinedButton(
                text = stringResource(string.continue_button),
                enabled = builtConfiguration != null
            ) {
                builtConfiguration?.let(onConfigurationSelected)
            }
        },
        dismissButton = {
            if(state.configuration != null) {
                AppTheme.OutlinedButton(text = stringResource(string.cancel), onClick = onDismiss)
            }
        },
    ) {
        Column {
            ConfigurationSelector(
                state = state,
                modifier = Modifier.padding(top = 16.dp),
            )

            CustomEnvironmentDetails(state = state)
        }
    }
}

private class SdkConfigurationState(
    context: Context,
    val configuration: SdkConfiguration?,
    val configurations: SdkConfigurations,
) {
    val customConfigurationName = context.getString(string.custom)
    var configurationName by mutableStateOf(configuration?.name ?: customConfigurationName)
        private set
    val environments = CXOneEnvironment.values()
    var environment by mutableStateOf(configuration?.environment?.name ?: "")
    var brandId by mutableStateOf(configuration?.brandId?.toString() ?: "")
    var channelId by mutableStateOf(configuration?.channelId ?: "")
    val isCustomConfiguration by derivedStateOf { configurationName == customConfigurationName }

    fun useConfiguration(name: String) {
        // This will have the effect of prepopulating custom configuration
        // options with value from last selected predefined configuration.
        if (configurationName != name && name != customConfigurationName) {
            configurations
                .firstOrNull { it.name == name }
                ?.also { configuration ->
                    environment = configuration.environment.name
                    brandId = configuration.brandId.toString()
                    channelId = configuration.channelId
                }
        }
        configurationName = name
    }

    fun validate() = when {
        configurationName != customConfigurationName -> true
        environment.isBlank() -> false
        brandId.isBlank() -> false
        brandId.toLongOrNull() == null -> false
        channelId.isBlank() -> false
        else -> true
    }

    fun build(): SdkConfiguration? {
        /* prefer the named configuration */
        val sdkConfiguration = configurations.firstOrNull {
            it.name == configurationName
        }

        return sdkConfiguration ?: environments
            /* Otherwise we need the named environment to build a custom configuration */
            .firstOrNull { it.name == environment }
            ?.let {
                SdkConfiguration(
                    name = customConfigurationName,
                    environment = it.asSdkEnvironment,
                    brandId = brandId.toLong(),
                    channelId = channelId
                )
            }
    }
}

@Composable
private fun ConfigurationSelector(
    state: SdkConfigurationState,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val choices: Sequence<DropdownItem<String>> =
        (state.configurations.map { DropdownItem(it.name) } + DropdownItem(context.getString(string.custom)))
            .asSequence()

    Column(
        modifier
            .border(1.dp, AppTheme.colorScheme.onBackground.copy(alpha = 0.50f), RoundedCornerShape(4.dp))
    ) {
        DropdownField(
            modifier = Modifier.padding(space.defaultPadding),
            label = stringResource(string.configuration),
            value = state.configurationName,
            options = choices,
            onSelect = state::useConfiguration
        )
    }
}

@Composable
private fun CustomEnvironmentDetails(state: SdkConfigurationState) {
    if(state.isCustomConfiguration) {
        HorizontalDivider(modifier = Modifier.padding(vertical = space.medium))
        EnvironmentSelector(state)
        BrandIdField(state)
        ChannelIdField(state)
    }
}

@Composable
private fun EnvironmentSelector(
    state: SdkConfigurationState,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .padding(top = space.medium)
            .border(1.dp, AppTheme.colorScheme.onBackground.copy(alpha = 0.50f), RoundedCornerShape(4.dp))
    ) {
        DropdownField(
            modifier = modifier.padding(space.defaultPadding),
            label = stringResource(string.environment),
            value = state.environment,
            options = state.environments.map { DropdownItem(it.name) }.asSequence(),
        ) {
            state.environment = it
        }
    }
}

@Composable
private fun BrandIdField(state: SdkConfigurationState) {
    AppTheme.TextField(
        label = stringResource(string.brand_id),
        value = state.brandId,
        requirement = allOf(required, integer),
    ) {
        state.brandId = it
    }
}

@Composable
private fun ChannelIdField(state: SdkConfigurationState) {
    AppTheme.TextField(
        label = stringResource(string.channel_id),
        value = state.channelId,
        requirement = allOf(required),
    ) {
        state.channelId = it
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewWithCustom() {
    val context = LocalContext.current
    val configurations = runBlocking { SdkConfigurationListRepository(context).load() }

    AppTheme {
        SdkConfigurationDialog(
            null,
            configurations = configurations,
            onDismiss = {},
            onConfigurationSelected = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewWithSelection() {
    val context = LocalContext.current
    val configurations = runBlocking { SdkConfigurationListRepository(context).load() }

    AppTheme {
        SdkConfigurationDialog(
            configurations.first(),
            configurations = configurations,
            onDismiss = {},
            onConfigurationSelected = {}
        )
    }
}
