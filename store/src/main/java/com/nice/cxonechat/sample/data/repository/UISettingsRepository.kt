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

package com.nice.cxonechat.sample.data.repository

import android.content.Context
import androidx.annotation.Keep
import com.nice.cxonechat.sample.data.models.UISettingsModel
import com.nice.cxonechat.sample.data.models.UISettingsModel.Colors
import com.nice.cxonechat.ui.composable.theme.ChatThemeDetails
import com.nice.cxonechat.ui.composable.theme.Images
import com.nice.cxonechat.ui.composable.theme.ThemeColors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.core.annotation.Single

/** Current UI Settings as a mutable flow for compose theming. */
val UISettingsState = MutableStateFlow(UISettingsModel())

/** Current UI Settings as an immutable flow for compose theming. */
val UISettings: StateFlow<UISettingsModel> = UISettingsState.asStateFlow()

/**
 * Repository to save and restore UI settings to a json file.
 *
 * @param context Application context for file access.
 */
@Single
class UISettingsRepository(
    val context: Context,
) : FileRepository<UISettingsModel>(
    fileName = "UISettings.json",
    type = UISettingsModel::class
) {
    private val Colors.asChatThemeColors: ThemeColors
        get() = ThemeColors(
            primary = primary,
            onPrimary = onPrimary,
            background = background,
            onBackground = onBackground,
            accent = accent,
            onAccent = onAccent,
            agentBackground = agentBackground,
            agentText = agentText,
            customerBackground = customerBackground,
            customerText = customerText
        )

    /**
     * Update the saved UI Settings.
     *
     * @param uiSettingsModel New UI Settings to saved.
     */
    fun save(uiSettingsModel: UISettingsModel) {
        UISettingsState.value = uiSettingsModel
        uiSettingsModel.applyToChatSdk()
        super.save(context, uiSettingsModel)
    }

    /**
     * Load any available saved UI Settings.  Default settings will be applied if no saved
     * settings are located.
     */
    @Keep // Remove once the  DE-117407 is resolved
    fun load() = super.load(context).also {
        UISettingsState.value = (it ?: UISettingsModel()).apply {
            applyToChatSdk()
        }
    }

    /**
     * Clear any saved ui settings and restore default settings to the theme.
     */
    fun clear() {
        CoroutineScope(Dispatchers.IO).launch {
            super.clear(context)
            UISettingsState.value = UISettingsModel()
        }
    }

    private fun UISettingsModel.applyToChatSdk() {
        ChatThemeDetails.darkColors = darkModeColors.asChatThemeColors
        ChatThemeDetails.lightColors = lightModeColors.asChatThemeColors
        ChatThemeDetails.images = Images(logo)
    }
}
