/*
 * Copyright (c) 2021-2025. NICE Ltd. All rights reserved.
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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import coil3.ImageLoader
import coil3.compose.AsyncImage
import com.nice.cxonechat.sample.R.string
import com.nice.cxonechat.sample.data.models.UISettingsModel
import com.nice.cxonechat.sample.data.repository.UISettings
import com.nice.cxonechat.sample.data.repository.UISettingsState
import com.nice.cxonechat.sample.ui.TestModifier
import com.nice.cxonechat.sample.ui.theme.AppTheme
import com.nice.cxonechat.sample.ui.theme.AppTheme.colorScheme
import com.nice.cxonechat.sample.ui.theme.AppTheme.space
import com.nice.cxonechat.sample.ui.theme.LocalSpace
import com.nice.cxonechat.sample.ui.theme.MultiToggleButton
import com.nice.cxonechat.sample.ui.theme.Shapes
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
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UISettingsDialog(
    value: UISettingsModel,
    onDismiss: () -> Unit,
    pickImage: ((String?) -> Unit) -> Unit,
    onReset: () -> Unit,
    onConfirm: (UISettingsModel) -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        modifier = TestModifier.fillMaxHeight(1f)
    ) {
        UiSettingsContent(value, pickImage, onDismiss, onReset, onConfirm)
    }
}

@Composable
private fun ColumnScope.UiSettingsContent(
    value: UISettingsModel,
    pickImage: ((String?) -> Unit) -> Unit,
    onDismiss: () -> Unit,
    onReset: () -> Unit,
    onConfirm: (UISettingsModel) -> Unit,
) {
    var current by remember { mutableStateOf(value) }
    var error by remember { mutableStateOf<Exception?>(null) }
    Column(
        modifier = Modifier
            .padding(horizontal = space.medium)
            .verticalScroll(rememberScrollState())
            .weight(1f, fill = true) // Modal bottom sheet will try to use minimal size, we need to force the fill
    ) {
        Text(
            text = stringResource(id = string.ui_settings),
            style = AppTheme.typography.headlineSmall,
            modifier = Modifier.semantics { heading() }
        )
        SettingsView(settings = current, pickImage) { current = it }
        if (error != null) {
            AlertDialog(
                onDismissRequest = { error = null },
                confirmButton = {
                    TextButton(
                        modifier = Modifier.testTag("ui_setting_error_ok_button"),
                        onClick = {
                            error = null
                        },
                    ) {
                        Text(text = stringResource(string.ok))
                    }
                },
                title = { Text(stringResource(string.error_saving_settings)) },
                text = { Text(error?.localizedMessage ?: stringResource(string.unknown_error)) },
                modifier = TestModifier,
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        HorizontalDivider()
        SettingsBottomRow(onDismiss, onReset) {
            UISettingsState.value = current
            try {
                onConfirm(current)
            } catch (exc: java.lang.Exception) {
                error = exc
            }
            onDismiss()
        }
    }
}

@Composable
private fun SettingsView(
    settings: UISettingsModel,
    pickImage: ((String?) -> Unit) -> Unit,
    onChanged: (UISettingsModel) -> Unit,
) {
    ImagePicker(settings, pickImage, onChanged)
    HorizontalDivider()
    ColorsSection(settings, onChanged)
}

@Composable
private fun SettingsBottomRow(
    onDismiss: () -> Unit,
    onReset: () -> Unit,
    onConfirm: () -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = space.large, bottom = space.small)
    ) {
        TextButton(
            modifier = Modifier.testTag("ui_setting_cancel_button"),
            onClick = onDismiss
        ) {
            Text(text = stringResource(string.cancel))
        }
        TextButton(
            modifier = Modifier.testTag("ui_setting_set_default_button"),
            onClick = onReset
        ) {
            Text(text = stringResource(string.set_defaults))
        }
        TextButton(
            modifier = Modifier.testTag("ui_setting_ok_button"),
            onClick = onConfirm
        ) {
            Text(text = stringResource(string.ok))
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
                .padding(bottom = space.small)
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
            imageLoader = ImageLoader.Builder(LocalContext.current).coroutineContext(Dispatchers.IO).build(),
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
                .testTag("pick_logo_button")
                .padding(horizontal = space.medium)
                .fillMaxWidth(),
            shape = Shapes.medium
        ) {
            Text(stringResource(string.pick_a_logo_image))
        }
    }
}

@PreviewLightDark
@Composable
private fun UISettingsDialogPreview() {
    val current = UISettings.collectAsState().value
    AppTheme {
        Surface(
            color = colorScheme.background,
        ) {
            Column {
                UiSettingsContent(current, {}, {}, {}, {})
            }
        }
    }
}
