package com.nice.cxonechat.detekt

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

            func foo() {
                Log.v("tag", "message")
            }
        """.trimIndent()

        val findings = ProhibitedCallRule(Config.empty).compileAndLintWithContext(env, code)

        findings shouldHaveSize 0
    }

    @Test
    fun `android_util_Log should be an error`() {
        val code = """
            func foo() {
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
        """.trimIndent()

        val findings = ProhibitedCallRule(Config.empty).compileAndLintWithContext(env, code)

        findings shouldHaveSize 10
    }

    @Test
    fun `println should generate an error`() {
        val code = """
            class Foo() {
                fun println() {
                }
            }

            fun bar() {
                System.out.println("this should fail")
                println("this should fail")
                Foo().println()
            }
        """.trimIndent()

        val findings = ProhibitedCallRule(Config.empty).compileAndLintWithContext(env, code)

        findings shouldHaveSize 2
    }
}
