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

package com.nice.cxonechat.sample.ui.uisettings

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowRight
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.nice.cxonechat.sample.R.string
import com.nice.cxonechat.sample.data.models.UISettingsModel
import com.nice.cxonechat.sample.ui.theme.AppTheme
import com.nice.cxonechat.sample.ui.theme.AppTheme.space
import com.nice.cxonechat.sample.ui.theme.Colors

@Composable
internal fun ColorsView(colors: UISettingsModel.Colors, onColorsChanged: (UISettingsModel.Colors) -> Unit) {
    Column {
        BrandColorsSection(colors, onColorsChanged)
        ContentColorsSection(colors, onColorsChanged)
        BorderColorsSection(colors, onColorsChanged)
        StatusColorsSection(colors, onColorsChanged)
        BackgroundColorsSection(colors, onColorsChanged)
    }
}

@Composable
private fun ColorsSectionCard(
    @StringRes headerRes: Int,
    content: @Composable () -> Unit,
) {
    val cardModifier = Modifier
        .padding(vertical = space.small)
        .animateContentSize()
    val columnModifier = Modifier
        .padding(space.medium)
        .fillMaxWidth()
    OutlinedCard(
        modifier = cardModifier,
        border = BorderStroke(1.dp, AppTheme.colorScheme.outline),
        colors = CardDefaults.outlinedCardColors(containerColor = Color.Transparent)
    ) {
        var expanded by remember { mutableStateOf(false) }
        Column(columnModifier) {
            TextButton(
                onClick = {
                    expanded = !expanded
                }
            ) {
                val animatedRotation by animateFloatAsState(if (expanded) 90f else 0f)
                Text(
                    text = stringResource(headerRes),
                    style = AppTheme.typography.titleMedium,
                    modifier = Modifier
                        .semantics { heading() }
                        .weight(1f)
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Default.ArrowRight,
                    contentDescription = null,
                    modifier = Modifier.rotate(animatedRotation)
                )
            }
            AnimatedVisibility(expanded) {
                Column {
                    content()
                }
            }
        }
    }
}

@Composable
private fun BrandColorsSection(
    colors: UISettingsModel.Colors,
    onColorsChanged: (UISettingsModel.Colors) -> Unit,
) {
    ColorsSectionCard(headerRes = string.brand) {
        ColorField(colors.brand.primary, label = stringResource(string.primary)) {
            onColorsChanged(colors.copy(brand = colors.brand.copy(primary = it)))
        }
        ColorField(colors.brand.onPrimary, label = stringResource(string.on_primary)) {
            onColorsChanged(colors.copy(brand = colors.brand.copy(onPrimary = it)))
        }
        ColorField(colors.brand.primaryContainer, label = stringResource(string.primary_container)) {
            onColorsChanged(colors.copy(brand = colors.brand.copy(primaryContainer = it)))
        }
        ColorField(colors.brand.onPrimaryContainer, label = stringResource(string.on_primary_container)) {
            onColorsChanged(colors.copy(brand = colors.brand.copy(onPrimaryContainer = it)))
        }
        ColorField(colors.brand.secondary, label = stringResource(string.secondary)) {
            onColorsChanged(colors.copy(brand = colors.brand.copy(secondary = it)))
        }
        ColorField(colors.brand.onSecondary, label = stringResource(string.on_secondary)) {
            onColorsChanged(colors.copy(brand = colors.brand.copy(onSecondary = it)))
        }
        ColorField(colors.brand.secondaryContainer, label = stringResource(string.secondary_container)) {
            onColorsChanged(colors.copy(brand = colors.brand.copy(secondaryContainer = it)))
        }
        ColorField(colors.brand.onSecondaryContainer, label = stringResource(string.on_secondary_container)) {
            onColorsChanged(colors.copy(brand = colors.brand.copy(onSecondaryContainer = it)))
        }
    }
}

@Composable
private fun ContentColorsSection(
    colors: UISettingsModel.Colors,
    onColorsChanged: (UISettingsModel.Colors) -> Unit,
) {
    ColorsSectionCard(headerRes = string.content_primary) {
        ColorField(colors.content.primary, label = stringResource(string.primary)) {
            onColorsChanged(colors.copy(content = colors.content.copy(primary = it)))
        }
        ColorField(colors.content.secondary, label = stringResource(string.secondary)) {
            onColorsChanged(colors.copy(content = colors.content.copy(secondary = it)))
        }
        ColorField(colors.content.tertiary, label = stringResource(string.tertiary)) {
            onColorsChanged(colors.copy(content = colors.content.copy(tertiary = it)))
        }
        ColorField(colors.content.inverse, label = stringResource(string.inverse)) {
            onColorsChanged(colors.copy(content = colors.content.copy(inverse = it)))
        }
    }
}

@Composable
private fun BorderColorsSection(
    colors: UISettingsModel.Colors,
    onColorsChanged: (UISettingsModel.Colors) -> Unit,
) {
    ColorsSectionCard(headerRes = string.border) {
        ColorField(colors.border.default, label = stringResource(string.background)) {
            onColorsChanged(colors.copy(border = colors.border.copy(default = it)))
        }
        ColorField(colors.border.subtle, label = stringResource(string.subtle)) {
            onColorsChanged(colors.copy(border = colors.border.copy(subtle = it)))
        }
    }
}

@Composable
private fun StatusColorsSection(
    colors: UISettingsModel.Colors,
    onColorsChanged: (UISettingsModel.Colors) -> Unit,
) {
    ColorsSectionCard(headerRes = string.status) {
        ColorField(colors.status.success, label = stringResource(string.status_success)) {
            onColorsChanged(colors.copy(status = colors.status.copy(success = it)))
        }
        ColorField(colors.status.onSuccess, label = stringResource(string.status_on_success)) {
            onColorsChanged(colors.copy(status = colors.status.copy(onSuccess = it)))
        }
        ColorField(colors.status.successContainer, label = stringResource(string.status_success_container)) {
            onColorsChanged(colors.copy(status = colors.status.copy(successContainer = it)))
        }
        ColorField(colors.status.onSuccessContainer, label = stringResource(string.status_on_success_container)) {
            onColorsChanged(colors.copy(status = colors.status.copy(onSuccessContainer = it)))
        }
        ColorField(colors.status.warning, label = stringResource(string.status_warning)) {
            onColorsChanged(colors.copy(status = colors.status.copy(warning = it)))
        }
        ColorField(colors.status.onWarning, label = stringResource(string.status_on_warning)) {
            onColorsChanged(colors.copy(status = colors.status.copy(onWarning = it)))
        }
        ColorField(colors.status.warningContainer, label = stringResource(string.status_warning_container)) {
            onColorsChanged(colors.copy(status = colors.status.copy(warningContainer = it)))
        }
        ColorField(colors.status.onWarningContainer, label = stringResource(string.status_on_warning_container)) {
            onColorsChanged(colors.copy(status = colors.status.copy(onWarningContainer = it)))
        }
        ColorField(colors.status.error, label = stringResource(string.status_error)) {
            onColorsChanged(colors.copy(status = colors.status.copy(error = it)))
        }
        ColorField(colors.status.onError, label = stringResource(string.status_on_error)) {
            onColorsChanged(colors.copy(status = colors.status.copy(onError = it)))
        }
        ColorField(colors.status.errorContainer, label = stringResource(string.status_error_container)) {
            onColorsChanged(colors.copy(status = colors.status.copy(errorContainer = it)))
        }
        ColorField(colors.status.onErrorContainer, label = stringResource(string.status_on_error_container)) {
            onColorsChanged(colors.copy(status = colors.status.copy(onErrorContainer = it)))
        }
    }
}

@Composable
private fun BackgroundColorsSection(
    colors: UISettingsModel.Colors,
    onColorsChanged: (UISettingsModel.Colors) -> Unit,
) {
    ColorsSectionCard(headerRes = string.background) {
        ColorField(colors.background.default, label = stringResource(string.background)) {
            onColorsChanged(colors.copy(background = colors.background.copy(default = it)))
        }
        ColorField(colors.background.inverse, label = stringResource(string.inverse)) {
            onColorsChanged(colors.copy(background = colors.background.copy(inverse = it)))
        }
        Text(stringResource(string.surface))
        ColorField(colors.background.surface.default, label = stringResource(string.surface)) {
            onColorsChanged(colors.copy(background = colors.background.copy(surface = colors.background.surface.copy(default = it))))
        }
        ColorField(colors.background.surface.subtle, label = stringResource(string.subtle)) {
            onColorsChanged(colors.copy(background = colors.background.copy(surface = colors.background.surface.copy(subtle = it))))
        }
        ColorField(colors.background.surface.variant, label = stringResource(string.variant)) {
            onColorsChanged(colors.copy(background = colors.background.copy(surface = colors.background.surface.copy(variant = it))))
        }
        ColorField(colors.background.surface.container, label = stringResource(string.primary_container)) {
            onColorsChanged(colors.copy(background = colors.background.copy(surface = colors.background.surface.copy(container = it))))
        }
        ColorField(colors.background.surface.emphasis, label = stringResource(string.emphasis)) {
            onColorsChanged(colors.copy(background = colors.background.copy(surface = colors.background.surface.copy(emphasis = it))))
        }
    }
}

@PreviewLightDark
@Composable
private fun ColorsViewPreview() {
    AppTheme {
        val isSystemInDarkTheme = isSystemInDarkTheme()
        var colors by remember(isSystemInDarkTheme) {
            mutableStateOf(UISettingsModel.Colors(if (isSystemInDarkTheme) Colors.Dark else Colors.Light))
        }

        Surface(
            color = AppTheme.colorScheme.background,
            modifier = Modifier.verticalScroll(rememberScrollState())
        ) {
            ColorsView(colors = colors) { new -> colors = new }
        }
    }
}
