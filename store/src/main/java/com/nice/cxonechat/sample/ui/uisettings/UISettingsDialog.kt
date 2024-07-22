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

package com.nice.cxonechat.sample.ui.uisettings

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize.Min
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil.compose.AsyncImage
import com.nice.cxonechat.sample.R.string
import com.nice.cxonechat.sample.data.models.UISettingsModel
import com.nice.cxonechat.sample.data.repository.UISettings
import com.nice.cxonechat.sample.data.repository.UISettingsState
import com.nice.cxonechat.sample.ui.theme.AppTheme
import com.nice.cxonechat.sample.ui.theme.AppTheme.space
import com.nice.cxonechat.sample.ui.theme.Dialog
import com.nice.cxonechat.sample.ui.theme.LocalSpace
import com.nice.cxonechat.sample.ui.theme.MultiToggleButton
import com.nice.cxonechat.sample.ui.theme.OutlinedButton
import kotlinx.coroutines.Dispatchers

/**
 * Edit the UI Settings currently in place.
 *
 * @param value current settings
 * @param onDismiss close the dialog with no changes.
 * @param pickImage action which when finished will return string which will be resolvable as an image.
 * @param onReset ui settings should be reset to a default state.
 * @param onConfirm new settings have been accepted, change them as necessary.
 */
@Composable
fun UISettingsDialog(
    value: UISettingsModel,
    onDismiss: () -> Unit,
    pickImage: ((String?) -> Unit) -> Unit,
    onReset: () -> Unit,
    onConfirm: (UISettingsModel) -> Unit,
) {
    var current by remember { mutableStateOf(value) }
    var error by remember { mutableStateOf<Exception?>(null) }

    AppTheme.Dialog(
        title = stringResource(id = string.ui_settings),
        onDismiss = onDismiss,
        dismissButton = {
            AppTheme.OutlinedButton(text = stringResource(string.cancel), onClick = onDismiss)
        },
        confirmButton = {
            AppTheme.OutlinedButton(text = stringResource(string.ok)) {
                UISettingsState.value = current
                try {
                    onConfirm(current)
                } catch (exc: java.lang.Exception) {
                    error = exc
                }
                onDismiss()
            }
        }
    ) {
        SettingsView(settings = current, pickImage, onChanged = { current = it }, onReset = onReset)

        if (error != null) {
            AlertDialog(
                onDismissRequest = { error = null },
                confirmButton = {
                    AppTheme.OutlinedButton(text = stringResource(string.ok)) {
                        error = null
                    }
                },
                title = { Text(stringResource(string.error_saving_settings)) },
                text = { Text(error?.localizedMessage ?: stringResource(string.unknown_error)) }
            )
        }
    }
}

@Composable
private fun SettingsView(
    settings: UISettingsModel,
    pickImage: ((String?) -> Unit) -> Unit,
    onChanged: (UISettingsModel) -> Unit,
    onReset: () -> Unit,
) {
    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .height(Min)
    ) {
        ImagePicker(settings, pickImage, onChanged)
        Divider()
        ColorsSection(settings, onChanged)
        Divider()
        Row(
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = space.large),
        ) {
            AppTheme.OutlinedButton(text = stringResource(string.set_defaults), onClick = onReset)
        }
    }
}

@Composable
private fun ColorsSection(
    settings: UISettingsModel,
    onChanged: (UISettingsModel) -> Unit,
    modifier: Modifier = Modifier,
) {
    val modes = listOf(stringResource(string.day), stringResource(string.night))
    var mode by remember { mutableIntStateOf(0) }
    Column(modifier.padding(vertical = space.medium)) {
        Row(
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
        ) {
            AppTheme.MultiToggleButton(currentSelection = modes[mode], toggleStates = modes) { next ->
                mode = modes.indexOf(next)
            }
        }
        ColorsView(colors = if (mode == 0) settings.lightModeColors else settings.darkModeColors) {
            when (mode) {
                0 -> onChanged(settings.copy(lightModeColors = it))
                1 -> onChanged(settings.copy(darkModeColors = it))
            }
        }
    }
}

@Composable
private fun ImagePicker(
    settings: UISettingsModel,
    pickImage: ((String?) -> Unit) -> Unit,
    onChanged: (UISettingsModel) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier,
    ) {
        AsyncImage(
            imageLoader = ImageLoader.Builder(LocalContext.current).interceptorDispatcher(Dispatchers.IO).build(),
            placeholder = rememberVectorPainter(image = Icons.Default.Image),
            model = settings.logo,
            contentDescription = null,
            modifier = Modifier
                .padding(LocalSpace.current.medium)
                .size(44.dp, 44.dp)
        )
        Button(
            onClick = {
                pickImage { pickedImage ->
                    onChanged(settings.copy(storedLogo = pickedImage))
                }
            },
            modifier = Modifier
                .padding(horizontal = space.medium)
                .fillMaxWidth()
        ) {
            Text(stringResource(string.pick_a_logo_image))
        }
    }
}

@Composable
private fun ColorsView(colors: UISettingsModel.Colors, onColorsChanged: (UISettingsModel.Colors) -> Unit) {
    Column {
        ColorField(colors.primary, label = stringResource(string.primary)) {
            onColorsChanged(
                colors.copy(primary = it)
            )
        }
        ColorField(colors.onPrimary, label = stringResource(string.onPrimary)) {
            onColorsChanged(
                colors.copy(onPrimary = it)
            )
        }
        ColorField(colors.accent, label = stringResource(string.accent)) {
            onColorsChanged(
                colors.copy(accent = it)
            )
        }
        ColorField(colors.onAccent, label = stringResource(string.onAccent)) {
            onColorsChanged(
                colors.copy(onAccent = it)
            )
        }
        ColorField(colors.background, label = stringResource(string.background)) {
            onColorsChanged(
                colors.copy(background = it)
            )
        }
        ColorField(colors.onBackground, label = stringResource(string.on_background)) {
            onColorsChanged(
                colors.copy(onBackground = it)
            )
        }
        ColorField(colors.agentBackground, label = stringResource(string.agent_background)) {
            onColorsChanged(
                colors.copy(agentBackground = it)
            )
        }
        ColorField(colors.agentText, label = stringResource(string.agent_text)) {
            onColorsChanged(
                colors.copy(agentText = it)
            )
        }
        ColorField(colors.customerBackground, label = stringResource(string.customer_background)) {
            onColorsChanged(
                colors.copy(customerBackground = it)
            )
        }
        ColorField(colors.customerText, label = stringResource(string.customer_text)) {
            onColorsChanged(
                colors.copy(customerText = it)
            )
        }
    }
}

@Preview(apiLevel = 31, showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
private fun UISettingsDialogPreview() {
    val current = UISettings.collectAsState().value

    AppTheme {
        Card(
            Modifier
                .fillMaxWidth(1f)
                .fillMaxHeight(1f)
        ) {
            UISettingsDialog(current, {}, {}, {}, {})
        }
    }
}
