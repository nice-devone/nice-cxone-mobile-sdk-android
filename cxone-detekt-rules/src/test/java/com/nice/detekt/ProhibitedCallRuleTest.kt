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

package com.nice.detekt

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.rules.KotlinCoreEnvironmentTest
import io.gitlab.arturbosch.detekt.test.compileAndLintWithContext
import io.kotest.matchers.collections.shouldHaveSize
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.junit.jupiter.api.Test

@KotlinCoreEnvironmentTest
internal class ProhibitedCallRuleTest(private val env: KotlinCoreEnvironment) {
    @Test
    fun `custom Log should not be an error`() {
        val code = """
            object Foo {
                fun v(tag: String, message: String) {}                
            }

            fun foo() {
                Log.v("tag", "message")
            }
        """.trimMargin()

        val findings = ProhibitedCallRule(Config.empty).compileAndLintWithContext(env, code)

        findings shouldHaveSize 0
    }

    @Test
    fun `android_util_Log should be an error`() {
        val code = """
            fun foo() {
                android.util.Log.v("tag", "message", null)
                android.util.Log.d("tag", "message", null)
                android.util.Log.i("tag", "message", null)
                android.util.Log.w("tag", "message", null)
                android.util.Log.e("tag", "message", null)

                android.util.Log.v("tag", "message")
                android.util.Log.d("tag", "message")
                android.util.Log.i("tag", "message")
                android.util.Log.w("tag", "message")
                android.util.Log.e("tag", "message")
            }
        """.trimMargin()

        val findings = ProhibitedCallRule(Config.empty).compileAndLintWithContext(env, code)

        findings shouldHaveSize 10
    }

    @Test
    fun `println should generate an error`() {
        val code = """
            class Foo {
                fun println() {
                }
            }
            
            fun bar() {
                System.out.println("this should fail")
                println("this should fail")
                Foo().println()
            }
        """.trimMargin()

        val findings = ProhibitedCallRule(Config.empty).compileAndLintWithContext(env, code)

        findings shouldHaveSize 2
    }
}
