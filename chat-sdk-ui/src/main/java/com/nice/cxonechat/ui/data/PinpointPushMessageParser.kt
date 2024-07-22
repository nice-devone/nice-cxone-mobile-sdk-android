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

package com.nice.cxonechat.ui.data

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.PackageManager.ApplicationInfoFlags
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import androidx.annotation.DrawableRes
import com.nice.cxonechat.ui.domain.PushMessage
import com.nice.cxonechat.ui.domain.PushMessageParser

/**
 * Pinpoint specific implementation of [PushMessageParser].
 */
internal class PinpointPushMessageParser(
    private val context: Context,
) : PushMessageParser {

    override fun parse(data: Map<String, String>): PushMessage {
        val imageUrl = data[PINPOINT_IMAGE_PUSH_KEY]
        val imageIconUrl = data[PINPOINT_IMAGE_ICON_PUSH_KEY]
        val imageSmallIconUrl = data[PINPOINT_IMAGE_SMALL_ICON_PUSH_KEY]
        val isSilent = "1".equals(data[NOTIFICATION_SILENT_PUSH_KEY], ignoreCase = true)
        val iconResId = getNotificationIconResourceId(context, data[NOTIFICATION_ICON_PUSH_KEY])
        val title = data[NOTIFICATION_TITLE_PUSH_KEY]
        val message = data[NOTIFICATION_BODY_PUSH_KEY]
        val colorString = data[NOTIFICATION_COLOR_PUSH_KEY]
        val url = data[EVENT_SOURCE_URL_PUSH_KEY]
        val deepLink = data[EVENT_SOURCE_DEEP_LINK_PUSH_KEY]
        val openApp = data[EVENT_SOURCE_OPEN_APP_PUSH_KEY]
        return PushMessage(
            imageUrl = imageUrl,
            imageIconUrl = imageIconUrl,
            imageSmallIconUrl = imageSmallIconUrl,
            isSilent = isSilent,
            iconResId = iconResId,
            title = title,
            message = message,
            colorString = colorString,
            url = url,
            deepLink = deepLink,
            openApp = openApp
        )
    }

    /**
     * Parser keys are based on constants from
     * [NotificationClientBase](com.amazonaws.mobileconnectors.pinpoint.targeting.notification.NotificationClientBase).
     */
    private companion object {
        private const val PINPOINT_PUSH_KEY_PREFIX = "pinpoint."
        private const val GCM_NOTIFICATION_PUSH_KEY_PREFIX = PINPOINT_PUSH_KEY_PREFIX + "notification."

        @Suppress("VariableMaxLength")
        private const val PINPOINT_IMAGE_SMALL_ICON_PUSH_KEY = GCM_NOTIFICATION_PUSH_KEY_PREFIX + "imageSmallIconUrl"
        private const val PINPOINT_IMAGE_PUSH_KEY = GCM_NOTIFICATION_PUSH_KEY_PREFIX + "imageUrl"
        private const val PINPOINT_IMAGE_ICON_PUSH_KEY = GCM_NOTIFICATION_PUSH_KEY_PREFIX + "imageIconUrl"

        private const val NOTIFICATION_SILENT_PUSH_KEY = GCM_NOTIFICATION_PUSH_KEY_PREFIX + "silentPush"
        private const val NOTIFICATION_TITLE_PUSH_KEY = GCM_NOTIFICATION_PUSH_KEY_PREFIX + "title"
        private const val NOTIFICATION_BODY_PUSH_KEY = GCM_NOTIFICATION_PUSH_KEY_PREFIX + "body"
        private const val NOTIFICATION_COLOR_PUSH_KEY = GCM_NOTIFICATION_PUSH_KEY_PREFIX + "color"
        private const val NOTIFICATION_ICON_PUSH_KEY = GCM_NOTIFICATION_PUSH_KEY_PREFIX + "icon"

        private const val EVENT_SOURCE_URL_PUSH_KEY = PINPOINT_PUSH_KEY_PREFIX + "url"
        private const val EVENT_SOURCE_DEEP_LINK_PUSH_KEY = PINPOINT_PUSH_KEY_PREFIX + "deeplink"
        private const val EVENT_SOURCE_OPEN_APP_PUSH_KEY = PINPOINT_PUSH_KEY_PREFIX + "openApp"

        @DrawableRes
        private fun getNotificationIconResourceId(context: Context, drawableResourceName: String?): Int =
            context.runCatching {
                val packageName = packageName
                val appInfo = packageManager.getApplicationInfoMeta(packageName)
                if (drawableResourceName != null) {
                    @SuppressLint("DiscouragedApi")
                    val drawableId = resources.getIdentifier(drawableResourceName, "drawable", packageName)
                    if (drawableId != 0) {
                        return@runCatching drawableId
                    }
                }
                appInfo.icon
            }.getOrDefault(0)

        private fun PackageManager.getApplicationInfoMeta(packageName: String) =
            if (VERSION.SDK_INT >= VERSION_CODES.TIRAMISU) {
                getApplicationInfo(packageName, ApplicationInfoFlags.of(PackageManager.GET_META_DATA.toLong()))
            } else {
                @Suppress("DEPRECATION")
                getApplicationInfo(packageName, PackageManager.GET_META_DATA)
            }
    }
}
