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

package com.nice.cxonechat.ui.util

import android.app.Activity
import android.app.Activity.OVERRIDE_TRANSITION_CLOSE
import android.app.Activity.OVERRIDE_TRANSITION_OPEN
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.provider.Settings
import android.view.WindowManager.LayoutParams
import androidx.annotation.AnimRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle.State
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.nice.cxonechat.ui.R
import com.nice.cxonechat.ui.storage.ValueStorage
import com.nice.cxonechat.ui.storage.ValueStorage.StringKey.RequestedPermissionsKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

/**
 * Display dialog informing user that permission is required in order to provide functionality with supplied
 * rationale about details.
 * If user accepts the dialog is dismissed and the [onAcceptListener] is called.
 *
 * @param rationale String resource with the rationale.
 * @param onAcceptListener Action which is called when user clicks the positive button.
 */
internal fun Context.showRationale(@StringRes rationale: Int, onAcceptListener: () -> Unit) {
    MaterialAlertDialogBuilder(this)
        .setTitle(getString(R.string.permission_requested))
        .setMessage(rationale)
        .setNegativeButton(R.string.cancel, null)
        .setPositiveButton(R.string.ok) { dialog: DialogInterface, _ ->
            dialog.dismiss()
            onAcceptListener()
        }
        .show()
}

/**
 * Checks if the required permissions are granted and requests user to grant them if they are not granted.
 *
 * @param valueStorage Storage used to persist if the permission was already requested.
 * @param permissions Collection of required permissions
 * @param rationale Text with rationale which will explain user why we need said permissions.
 * @param onAcceptPermissionRequest Action which will be invoked when user accepts our request for permissions with an array of permissions
 * for which we can ask users to grant them (this can be a subset of required permission, since the system
 * can prevent requesting some permissions from user).
 *
 * @return `true` if all permissions were already granted, otherwise `false`.
 */
internal suspend fun Activity.checkPermissions(
    valueStorage: ValueStorage,
    permissions: Iterable<String>,
    @StringRes rationale: Int,
    onAcceptPermissionRequest: (Array<String>) -> Unit,
): Boolean {
    val missingPermissionsSet = permissions.filterNot { permission ->
        ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
    }.toSet()
    val missingPermissions = missingPermissionsSet.toTypedArray()
    val result = missingPermissions.isEmpty()
    if (!result) {
        if (missingPermissions.any(::shouldShowRequestPermissionRationale)) {
            // User has previously declined permission request, show rationale why the permission is important.
            showRationale(
                rationale = rationale,
                onAcceptListener = { onAcceptPermissionRequest(missingPermissions) }
            )
        } else {
            val requestedPermissions = valueStorage.getString(RequestedPermissionsKey)
                .firstOrNull()
                .orEmpty()
                .split(", ")
                .toSet()
            if (missingPermissionsSet.intersect(requestedPermissions).isEmpty()) {
                // Permission are requested for the first time
                valueStorage.setString(
                    RequestedPermissionsKey,
                    requestedPermissions.union(missingPermissionsSet).joinToString(", ")
                )
                onAcceptPermissionRequest(missingPermissions)
            } else {
                // Permissions were requested and the user has repeatedly denied the request.
                // Since the permissions can't be requested directly again, redirect user to the app settings.
                showRationale(rationale) {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    intent.data = Uri.fromParts("package", packageName, null)
                    startActivity(intent)
                }
            }
        }
    }
    return result
}

/**
 * This is workaround for issue when keyboard is shown window content pans under the toolbar and keyboard overlaps
 * window contents.
 * There should be a better solution.
 */
@Suppress("DEPRECATION")
internal fun Activity.applyFixesForKeyboardInput() {
    if (VERSION.SDK_INT >= VERSION_CODES.R) window.setDecorFitsSystemWindows(true)
    window.setSoftInputMode(LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
}

internal fun Activity.overrideOpenAnimation(
    @AnimRes enterAnim: Int,
    @AnimRes exitAnim: Int,
) {
    if (VERSION.SDK_INT < VERSION_CODES.UPSIDE_DOWN_CAKE) {
        @Suppress("DEPRECATION")
        overridePendingTransition(enterAnim, exitAnim)
    } else {
        overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, enterAnim, exitAnim)
    }
}

/*
 * This could be defined as a normal method on ChatActivity, but this seems to keep it paired with
 * overrideCloseAnimation better.
 */
internal fun Activity.overrideCloseAnimation(
    @AnimRes enterAnim: Int,
    @AnimRes exitAnim: Int,
) {
    if (VERSION.SDK_INT < VERSION_CODES.UPSIDE_DOWN_CAKE) {
        @Suppress("DEPRECATION")
        overridePendingTransition(enterAnim, exitAnim)
    } else {
        overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE, enterAnim, exitAnim)
    }
}

internal fun Activity.checkNotificationPermissions(permission: String, @StringRes rationale: Int, requestPermission: (String) -> Unit) {
    val isPermissionGranted = ContextCompat.checkSelfPermission(
        applicationContext,
        permission
    ) == PackageManager.PERMISSION_GRANTED
    when {
        isPermissionGranted -> Ignored
        shouldShowRequestPermissionRationale(permission) -> showRationale(rationale) { requestPermission(permission) }
        else -> requestPermission(permission)
    }
}

/**
 * Launch [repeatOnLifecycle] with supplied parameters using [LifecycleOwner]'s [lifecycleScope].
 *
 * @param state State on which the supplied [block] should be repeated, default is [State.RESUMED].
 * @param block Suspend function which should be launched in [androidx.lifecycle.LifecycleOwner.repeatOnLifecycle].
 */
internal fun LifecycleOwner.repeatOnOwnerLifecycle(
    state: State = State.RESUMED,
    block: suspend CoroutineScope.() -> Unit,
) {
    lifecycleScope.launch {
        repeatOnLifecycle(state, block)
    }
}
