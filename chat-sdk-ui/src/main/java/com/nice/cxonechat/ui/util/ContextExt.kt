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

package com.nice.cxonechat.ui.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.nice.cxonechat.ui.util.PermissionState.DENIED
import com.nice.cxonechat.ui.util.PermissionState.GRANTED
import com.nice.cxonechat.ui.util.PermissionState.NOT_DECLARED
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Ask android to open a URL if possible.
 *
 * @param url URL to open
 * @param mimeType mime type of [url] if known
 * @return true iff android could open the URL
 */
internal fun Context.openWithAndroid(url: String, mimeType: String?): Boolean {
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(url.toUri(), mimeType)
    }

    return if (intent.resolveActivity(packageManager) == null) {
        false
    } else {
        startActivity(intent)
        true
    }
}

/**
 * Look up parent activity recursively.
 *
 * @return Parent [Activity] or `null`.
 */
internal tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    !is ContextWrapper -> null
    else -> baseContext.findActivity()
}

/**
 * Check which of the provided permissions are declared in the application's manifest.
 *
 * This is useful for determining if (optional) permissions are declared before
 * attempting to request them at runtime.
 *
 * @param permissions List of permission strings to check (e.g., android.permission.CAMERA).
 * @return List of permissions that are declared in the manifest and were supplied in the list of permissions.
 */
internal suspend fun Context.getDeclaredPermissions(permissions: List<String>): List<String> =
    withContext(Dispatchers.Default) {
        runCatching {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getPackageInfo(
                    packageName,
                    PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS.toLong())
                )
            } else {
                @Suppress("DEPRECATION")
                packageManager.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS)
            }
            val declaredPermissions = packageInfo.requestedPermissions?.toSet() ?: emptySet()
            permissions.filter { it in declaredPermissions }
        }.getOrDefault(emptyList())
    }

/**
 * Determines the current state of a single Android permission.
 *
 * This function checks whether a permission is declared in the app's manifest and if granted,
 * returns the appropriate [PermissionState]. It's useful for determining whether to request
 * a permission or skip the request entirely if the permission isn't declared.
 *
 * @param permission The Android permission string to check (e.g., `android.permission.CAMERA`).
 * @return [PermissionState.GRANTED] if the permission is declared and granted,
 * [PermissionState.DENIED] if declared but not granted,
 * [PermissionState.NOT_DECLARED] if not declared in the manifest.
 *
 * @see getDeclaredPermissions
 * @see PermissionState
 */
internal suspend fun Context.getPermissionState(permission: String): PermissionState =
    if (getDeclaredPermissions(listOf(permission)).contains(permission)) {
        if (ContextCompat.checkSelfPermission(applicationContext, permission) == PackageManager.PERMISSION_GRANTED) {
            GRANTED
        } else {
            DENIED
        }
    } else {
        NOT_DECLARED
    }

/**
 * Represents the possible states of an Android runtime permission.
 *
 * This enum is used to distinguish between permissions that are granted, denied, or not
 * declared in the app's manifest. This distinction is important for optional permissions
 * where the app should skip requesting permissions that aren't declared.
 */
internal enum class PermissionState {
    /**
     * The permission is declared in the manifest and has been granted by the user.
     */
    GRANTED,

    /**
     * The permission is declared in the manifest but has not been granted by the user.
     * The app may need to request this permission at runtime.
     */
    DENIED,

    /**
     * The permission is not declared in the app's manifest.
     * The app should not attempt to request this permission.
     */
    NOT_DECLARED
}
