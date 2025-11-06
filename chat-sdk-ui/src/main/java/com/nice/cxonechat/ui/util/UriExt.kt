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

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ProviderInfo
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.os.Process
import android.system.Os
import android.system.OsConstants
import android.system.StructStat
import java.io.File
import java.util.UUID

internal fun Uri.parseThreadDeeplink(): Result<UUID> = runCatching {
    val threadIdString = getQueryParameter(PARAM_DEEPLINK)
    require(!threadIdString.isNullOrEmpty()) { "Invalid threadId in $this" }
    UUID.fromString(threadIdString)
}

internal fun Uri.addThreadDeeplink(uid: UUID): Uri = buildUpon()
    .appendQueryParameter(PARAM_DEEPLINK, uid.toString())
    .build()

@Suppress(
    "ReturnCount" // We want to return early in case of invalid conditions.
)
internal fun Uri.isValidFile(pfd: ParcelFileDescriptor?): Boolean {
    // Check preconditions for a file Uri validation
    val uriPath = this.path
    if (pfd == null || this.scheme != "file" || uriPath.isNullOrEmpty()) {
        return false
    }
    // Canonicalize to resolve symlinks and path traversals.
    val fdCanonical = File(uriPath).canonicalPath

    val pfdStat: StructStat = Os.fstat(pfd.fileDescriptor)

    // Lstat doesn't follow the symlink.
    val canonicalFileStat: StructStat = Os.lstat(fdCanonical)

    // Since we canonicalized (followed the links) the path already,
    // the path shouldn't point to symlink unless it was changed in the
    // meantime.
    if (OsConstants.S_ISLNK(canonicalFileStat.st_mode)) {
        return false
    }

    val sameFile =
        pfdStat.st_dev == canonicalFileStat.st_dev &&
                pfdStat.st_ino == canonicalFileStat.st_ino

    return when {
        sameFile -> !isBlockedPath(fdCanonical)
        else -> false
    }
}

private fun isBlockedPath(fdCanonical: String): Boolean {
    // Paths that should rarely be exposed
    return fdCanonical.startsWith("/proc/") ||
            fdCanonical.startsWith("/data/misc/")
}

internal fun Uri.belongsToCurrentApplication(ctx: Context): Boolean {
    val authority: String = this.authority.orEmpty()
    val info: ProviderInfo? =
        ctx.packageManager.resolveContentProvider(authority, 0)

    return info?.let { info -> ctx.packageName.equals(info.packageName) } ?: false
}

internal fun Uri.isExported(ctx: Context): Boolean {
    val authority = this.authority.orEmpty()
    val info: ProviderInfo? =
        ctx.packageManager.resolveContentProvider(authority, 0)

    return info?.exported ?: false
}

// grantFlag is one of: FLAG_GRANT_READ_URI_PERMISSION or FLAG_GRANT_WRITE_URI_PERMISSION
internal fun Uri?.wasGrantedPermission(ctx: Context, grantFlag: Int = Intent.FLAG_GRANT_WRITE_URI_PERMISSION): Boolean {
    val pid: Int = Process.myPid()
    val uid: Int = Process.myUid()
    return ctx.checkUriPermission(this, pid, uid, grantFlag) ==
            PackageManager.PERMISSION_GRANTED
}

private const val PARAM_DEEPLINK = "idOnExternalPlatform"
