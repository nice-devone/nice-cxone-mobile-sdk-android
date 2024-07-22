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

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.AlertDialog
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.godaddy.android.colorpicker.HsvColor
import com.godaddy.android.colorpicker.harmony.ColorHarmonyMode
import com.godaddy.android.colorpicker.harmony.HarmonyColorPicker
import com.nice.cxonechat.sample.R.string
import com.nice.cxonechat.sample.extensions.asHexColor
import com.nice.cxonechat.sample.extensions.asHexString
import com.nice.cxonechat.sample.ui.theme.AppTheme
import com.nice.cxonechat.sample.ui.theme.LocalSpace
import com.nice.cxonechat.sample.ui.theme.TextField
import com.nice.cxonechat.sample.utilities.Requirement
import com.nice.cxonechat.sample.utilities.Requirements.allOf
import com.nice.cxonechat.sample.utilities.Requirements.required

/**
 * A ColorField to edit a color, specifically in the [UISettingsDialog].
 *
 * @param color current color.
 * @param label label to be displayed.
 * @param modifier Any modifiers to apply to the field.
 * @param onColorChanged Invoked when the color has been changed either by accepting
 * a new color in the picker or by completing the text field, i.e., the field loses
 * focus.
 */
@Composable
fun ColorField(
    color: Color,
    label: String,
    modifier: Modifier = Modifier,
    onColorChanged: (Color) -> Unit
) {
    var text by remember { mutableStateOf(color.asHexString) }
    var lastColor by remember { mutableStateOf(color) }

    if(lastColor != color) {
        lastColor = color
        text = color.asHexString
    }

    val isHexColor: Requirement = { context, txt ->
        if (txt.asHexColor == null) {
            context.getString(string.error_invalid_color)
        } else {
            null
        }
    }
    var showPicker by remember { mutableStateOf(false) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth()
    ) {
        ColorButton(color = color, modifier = modifier.padding(LocalSpace.current.medium)) {
            showPicker = true
        }
        AppTheme.TextField(
            label = label,
            value = text,
            modifier = modifier
                .fillMaxWidth(1f)
                .onFocusChanged {
                    if (!it.isFocused) {
                        text.asHexColor?.also { newColor ->
                            onColorChanged(newColor)
                        }
                    }
                },
            requirement = allOf(required, isHexColor),
            onValueChanged = { newText ->
                text = newText
            },
        )

        if(showPicker) {
            ColorPickerAlert(color = color, title = label, modifier = modifier, dismiss = { showPicker = false }) { picked ->
                showPicker = false
                onColorChanged(picked)
                text = picked.asHexString
            }
        }
    }
}

@Composable
private fun ColorPickerAlert(color: Color, title: String, modifier: Modifier, dismiss: () -> Unit, onConfirm: (Color) -> Unit) {
    var updatedHsvColor by remember(color) {
        mutableStateOf(HsvColor.from(color))
    }

    AlertDialog(
        dismiss,
        modifier = modifier,
        confirmButton = {
            TextButton(onClick = { onConfirm(updatedHsvColor.toColor()) }) {
                Text(stringResource(string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = dismiss) {
                Text(stringResource(string.cancel))
            }
        },
        title = { Text(title) },
        text = {
            HarmonyColorPicker(
                modifier = Modifier.size(400.dp),
                harmonyMode = ColorHarmonyMode.NONE,
                color = updatedHsvColor,
                onColorChanged = { hsvColor ->
                    updatedHsvColor = hsvColor
                }
            )
        }
    )
}

@Composable
private fun ColorButton(color: Color, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .width(44.dp)
            .height(44.dp)
            .border(1.dp, Color.Black)
            .clickable { onClick() }
    ) {
        Box(
            Modifier
                .fillMaxWidth(0.5f)
                .fillMaxHeight(1.0f)
                .background(Color.LightGray)
        )
        Box(
            Modifier
                .fillMaxWidth(0.5f)
                .fillMaxHeight(1.0f)
                .background(Color.DarkGray)
        )
        Box(
            Modifier
                .fillMaxWidth(1.0f)
                .fillMaxHeight(1.0f)
                .background(color)
                .padding(8.dp)
        )
    }
}

@Preview(apiLevel = 31, showBackground = true)
@Composable
private fun ColorFieldPreview() {
    AppTheme {
        var color by remember { mutableStateOf(Color(0xff7f7f7f)) }

        Column {
            ColorField(color, label = "Color", onColorChanged = { color = it })
        }
    }
}
