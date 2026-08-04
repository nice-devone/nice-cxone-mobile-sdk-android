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

package com.nice.cxonechat

import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension
import org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation

/** Convention plugin for Android library modules. Configures targetSdk, unit test coverage, and packaging exclusions. */
abstract class AndroidLibraryConventionsPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            apply(plugin = "com.android.library")
            apply(plugin = "com.nice.cxone.android.kotlin")
            extensions.configure<LibraryExtension> {
                testOptions.targetSdk = 36
                lint.targetSdk = 36
                buildTypes.named("debug") {
                    enableUnitTestCoverage = true
                }
                packaging.resources {
                    pickFirsts.add("META-INF/AL2.0")
                    pickFirsts.add("META-INF/LGPL2.1")
                }
            }
            extensions.configure<KotlinAndroidProjectExtension> {
                @OptIn(ExperimentalAbiValidation::class)
                abiValidation {
                    filters {
                        exclude {
                            byNames.addAll(
                                "com.nice.cxonechat.internal.**",
                                "com.nice.cxonechat.core.BuildConfig",
                                "com.nice.cxonechat.ui.composable.**",
                                "com.nice.cxonechat.ui.storage.TemporaryFileProvider",
                                "com.nice.cxonechat.ui.screen.ComposableSingletons?ThreadListScreenKt",
                                "com.nice.cxonechat.ui.screen.ComposableSingletons?OfflineScreenKt",
                                "com.nice.cxonechat.ui.screen.ComposableSingletons?ChatErrorScreenKt",
                                "com.nice.cxonechat.ui.screen.ComposableSingletons?ChatDialogScreenKt",
                                "com.nice.cxonechat.ui.screen.ComposableSingletons?LoadingOverlayFullScreenKt",
                                "org.koin.plugin.hints.**",
                                "com.nice.cxonechat.ui.ComNiceCxonechatUiUiModuleModuleKt",
                            )
                            annotatedWith.addAll(
                                "com.nice.cxonechat.InternalApiAnnotation",
                                "androidx.compose.ui.tooling.preview.*",
                            )
                        }
                    }
                }
            }
        }
    }
}
