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

package com.nice.cxonechat.sample

import android.annotation.SuppressLint
import android.os.Build
import android.os.PatternMatcher
import android.os.StrictMode
import android.os.StrictMode.ThreadPolicy
import android.os.StrictMode.VmPolicy
import android.os.strictmode.DiskReadViolation
import android.os.strictmode.ExplicitGcViolation
import android.os.strictmode.InstanceCountViolation
import android.os.strictmode.LeakedClosableViolation
import android.os.strictmode.ServiceConnectionLeakedViolation
import android.os.strictmode.UntaggedSocketViolation
import androidx.annotation.RequiresApi
import com.nice.cxonechat.sample.data.repository.ChatSettingsRepository
import com.nice.cxonechat.sample.data.repository.UISettingsRepository
import com.nice.cxonechat.sample.utilities.RuleBasedPenalty
import com.nice.cxonechat.sample.utilities.RuleBasedPenalty.Companion.Actions
import com.nice.cxonechat.sample.utilities.RuleBasedPenalty.Companion.Actions.log
import com.nice.cxonechat.sample.utilities.RuleBasedPenalty.Companion.Actions.terminate
import com.nice.cxonechat.sample.utilities.RuleBasedPenalty.Companion.Predicates.allOf
import com.nice.cxonechat.sample.utilities.RuleBasedPenalty.Companion.Predicates.any
import com.nice.cxonechat.sample.utilities.RuleBasedPenalty.Companion.Predicates.classNamed
import com.nice.cxonechat.sample.utilities.RuleBasedPenalty.Companion.Predicates.violation
import com.nice.cxonechat.sample.utilities.RuleBasedPenalty.Companion.allow
import com.nice.cxonechat.sample.utilities.RuleBasedPenalty.Rule
import java.util.concurrent.Executors

@RequiresApi(Build.VERSION_CODES.P)
internal object StrictModePolicy {
    private const val VM_POLICY_TAG = "VMPolicy"
    private val threadPolicy = RuleBasedPenalty(
        // DE-117407
        allow(
            allOf(
                violation(DiskReadViolation::class),
                classNamed(UISettingsRepository::class.qualifiedName!!, "load"),
            ),
        ),
        // DE-117407
        allow(
            allOf(
                violation(DiskReadViolation::class),
                classNamed(ChatSettingsRepository::class.qualifiedName!!, "load"),
            )
        ),
        // Appium automated test hooks into Android Activity are causing DiskReadViolation, ServiceConnectionLeakedViolation
        allow(
            allOf(
                violation(DiskReadViolation::class, ServiceConnectionLeakedViolation::class),
                classNamed(PatternMatcher("com.nexperience.android.", PatternMatcher.PATTERN_PREFIX))
            )
        ),
        // Samsung Galaxy Note 10 - Android 9
        allow(
            allOf(
                violation(DiskReadViolation::class),
                classNamed("android.graphics.Typeface", "SetAppTypeFace")
            )
        ),
        // Samsung devices are causing DiskReadViolation when permission is requested
        allow(
            allOf(
                violation(DiskReadViolation::class),
                classNamed(PatternMatcher("""com.samsung.android.knox.""", PatternMatcher.PATTERN_PREFIX))
            )
        ),
        // Samsung A20 - Android 10 - first app run
        allow(
            allOf(
                violation(DiskReadViolation::class),
                classNamed(
                    PatternMatcher(
                        """com.android.server.am.freecess.FreecessController""",
                        PatternMatcher.PATTERN_PREFIX,
                    ),
                )
            )
        ),
        // Samsung A20 - ComposeActivity
        allow(
            allOf(
                violation(DiskReadViolation::class),
                classNamed("android.graphics.Typeface", "setFlipFonts")
            )
        ),
        // Samsung S22 - Android 13
        allow(
            allOf(
                violation(DiskReadViolation::class),
                classNamed(PatternMatcher("""android.app.IdsController""", PatternMatcher.PATTERN_PREFIX))
            )
        ),
        allow(
            allOf(
                violation(DiskReadViolation::class),
                classNamed("android.app.SharedPreferencesImpl\$EditorImpl", "isSpeg")
            )
        ),
        // Samsung S8 - Android 9 - Coil - ImageLoader.Builder.build
        allow(
            allOf(
                violation(DiskReadViolation::class),
                classNamed("com.samsung.android.feature.SemCscFeature", "isUseOdmProduct")
            )
        ),
        // Samsung Galaxy Note10+ - Android 12
        allow(
            allOf(
                violation(DiskReadViolation::class),
                classNamed("android.app.ActivityThread", "getProfileSizeOfApp"),
                { Build.BRAND.contentEquals("samsung", ignoreCase = true) }
            )
        ),
        // LGE - V40 ThinQ - Reported via Crashlytics
        allow(
            allOf(
                violation(DiskReadViolation::class),
                classNamed("android.content.res.Resources", "startParallelLoading")
            )
        ),
        // Samsung and possibly other Qualcomm devices make bad disk reads passing through here.
        allow(
            allOf(
                violation(DiskReadViolation::class),
                classNamed("android.util.BoostFramework", "<init>")
            )
        ),
        // Samsung A25 - 5G (possibly other devices) - Android 14 - some functionality tied to its game mode
        allow(
            allOf(
                violation(DiskReadViolation::class),
                classNamed("android.content.pm.PackageManager", "isSpeg")
            )
        ),
        // Emulator - Android 12 - Reported via Crashlytics
        allow(
            allOf(
                violation(DiskReadViolation::class),
                classNamed("com.android.server.wm.WindowManagerService", "getTaskSnapshot")
            )
        ),
        // Huawei Y6s - Android 9 - Reported via Crashlytics
        allow(
            allOf(
                violation(DiskReadViolation::class),
                classNamed("android.graphics.HwTypefaceUtil", "updateFont")
            )
        ),
        // Xiaomi - Redmi Note 10 Pro &  Mi 11 Lite - Android 11 - Reported via Crashlytics
        allow(
            allOf(
                violation(DiskReadViolation::class),
                classNamed("miui.contentcatcher.InterceptorProxy", "create")
            )
        ),
        // Debugging
        allow(
            allOf(
                violation(DiskReadViolation::class),
                classNamed("dalvik.system.VMDebug")
            )
        ),
        allow(
            allOf(
                violation(DiskReadViolation::class),
                classNamed("dalvik.system.InMemoryDexClassLoader")
            )
        ),
        // Perfecto instrumentation rewrites app and adds it's own methods
        allow(
            allOf(
                violation(DiskReadViolation::class),
                classNamed(StoreActivity::class.qualifiedName!!, "onCreatePerfectoMobile")
            )
        ),
        // Amazon SSO SDK
        allow(
            allOf(
                violation(DiskReadViolation::class),
                classNamed("com.amazon.identity.auth.device.StoredPreferences", "setTokenObtainedFromSSO")
            )
        ),
        // Android 35 emulator - Debug - shouldShowRequestPermissionRationale
        allow(
            allOf(
                violation(DiskReadViolation::class),
                classNamed("android.app.ActivityThread\$AndroidOs", "stat")
            )
        ),
        // Samsung Galaxy Note 8 - Android 8.0 - Instance is retained when using System component to select an attachment
        @SuppressLint("ObsoleteSdkInt") // Runtime check for Android 8.0
        if (Build.MANUFACTURER.equals("Samsung", ignoreCase = true) &&
            Build.MODEL.equals("SM-N950", ignoreCase = true) &&
            Build.VERSION.SDK_INT == Build.VERSION_CODES.O
        ) {
            allow(
                allOf(
                    violation(InstanceCountViolation::class),
                )
            )
        } else {
            null
        },
        // There seems to be an issue on Android 14, 15 and 16 that results in releasing an Activity
        // throwing an ExplicitGcViolation.  There may be something we're doing to trigger
        // it, but I don't find any more information about it.
        if (Build.VERSION.SDK_INT in Build.VERSION_CODES.UPSIDE_DOWN_CAKE..Build.VERSION_CODES.BAKLAVA) {
            allow(
                allOf(
                    violation(ExplicitGcViolation::class),
                    classNamed("android.app.ActivityThread", "performDestroyActivity")
                )
            )
        } else {
            null
        },
        @Suppress("KotlinConstantConditions") // Build time variant
        if (!BuildConfig.DEBUG) {
            // For QA (Non-debug) build only log DiskReadViolation (because it occurs too often due to the Manufacturer modifications)
            Rule(
                violation(DiskReadViolation::class),
                log("ThreadPolicy")
            )
        } else {
            null
        },
        // Default action is to log and crash
        Rule(any(), Actions.allOf(log("ThreadPolicy"), terminate()))
    )

    private val vmPolicy = RuleBasedPenalty(
        // Platform bug https://github.com/aosp-mirror/platform_frameworks_base/commit/e7ae30f76788bcec4457c4e0b0c9cbff2cf892f3
        allow(
            allOf(
                violation(LeakedClosableViolation::class),
                classNamed("sun.nio.fs.UnixSecureDirectoryStream", "finalize"),
                { _ -> Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE }
            )
        ),

        // DE-66827
        Rule(
            violation(LeakedClosableViolation::class),
            log(VM_POLICY_TAG)
        ),
        // Crashlytics
        allow(
            classNamed("com.google.android.datatransport.runtime.SafeLoggingExecutor\$SafeLoggingRunnable")
        ),
        allow(
            classNamed(
                PatternMatcher("""com.google.firebase.""", PatternMatcher.PATTERN_PREFIX)
            )
        ),
        // Multiple
        Rule(
            violation(UntaggedSocketViolation::class),
            log(VM_POLICY_TAG)
        ),
        // Default action is to log and crash
        Rule(any(), Actions.allOf(log(VM_POLICY_TAG), terminate()))
    )

    private val executor = Executors.newSingleThreadScheduledExecutor()

    fun apply() {
        StrictMode.setThreadPolicy(
            ThreadPolicy.Builder()
                .detectAll()
                .penaltyListener(executor, threadPolicy::perform)
                .build()
        )

        StrictMode.setVmPolicy(
            VmPolicy.Builder()
                .detectAll()
                .penaltyListener(executor, vmPolicy::perform)
                .build()
        )
    }
}
