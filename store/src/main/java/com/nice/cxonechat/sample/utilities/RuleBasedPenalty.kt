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

package com.nice.cxonechat.sample.utilities

import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.os.Handler
import android.os.Looper
import android.os.PatternMatcher
import android.os.strictmode.Violation
import android.util.Log
import androidx.annotation.RequiresApi
import com.nice.cxonechat.sample.utilities.RuleBasedPenalty.Action
import com.nice.cxonechat.sample.utilities.RuleBasedPenalty.Companion.Actions.allow
import com.nice.cxonechat.sample.utilities.RuleBasedPenalty.Predicate
import kotlin.reflect.KClass

/**
 * Rules-based handling of Android StrictMode Violations.
 *
 * @param rules Defined rules to enforce on any violations that arise.  The rules will be matched in
 * sequence with only the first match being performed.
 */
@RequiresApi(VERSION_CODES.P)
class RuleBasedPenalty private constructor(
    private val rules: List<Rule>,
) {
    constructor(vararg rules: Rule?) : this(rules.filterNotNull())

    /**
     * A test predicate to match violations.
     */
    fun interface Predicate {
        /**
         * Test [violation] for match.
         *
         * @param violation [Violation] to test.
         * @return True iff [violation] matches this predicate.
         */
        fun test(violation: Violation): Boolean
    }

    /**
     * An action to perform once a predicate has been matched.
     */
    fun interface Action {
        /**
         * Perform the action on behalf of a named policy for a specific [Violation].
         *
         * @param violation Details of the violation that occurred.
         */
        fun perform(violation: Violation)
    }

    /**
     * A rule to be applied to violations that occur.
     *
     * @param predicate Predicate to test violations.
     * @param action Action to take on matching violations.
     */
    class Rule(
        private val predicate: Predicate,
        private val action: Action,
    ) {
        /**
         * Test [violation] for match.
         *
         * @param violation [Violation] to test.
         * @return True iff [violation] matches this predicate.
         */
        fun matches(violation: Violation) = predicate.test(violation)

        /**
         * Perform the action on behalf of a named policy for a specific [Violation].
         *
         * @param violation Details of the violation that occurred.
         */
        fun perform(violation: Violation) {
            action.perform(violation)
        }
    }

    /**
     * A violation has occurred, apply the rules.
     *
     * Each of the rules will be tested in sequence using [Rule.matches].  The first matching rule will be
     * applied per [Rule.perform].
     * @param violation The violation that has occurred.
     */
    fun perform(violation: Violation) {
        rules
            .firstOrNull { it.matches(violation) }
            ?.perform(violation)
    }

    companion object {
        /**
         * Create a rule to allow violations matching an exception.
         *
         * @param predicate [Predicate] to match.
         * @return A matching rule.
         */
        fun allow(predicate: Predicate) = Rule(
            predicate = predicate,
            action = allow()
        )

        /** A collection of common [Predicate]. */
        object Predicates {
            /**
             * Match any violation.
             *
             * @return A [Predicate] that always matches.
             */
            fun any() = Predicate { true }

            /**
             * Match a violation based on it's stack trace containing at least one instance
             * of a method in [methods] being defined by a class named [className].
             *
             * @param className Class name to match.
             * @param methods List of method names to match.
             * @return a Predicate that will return true iff a single entry in the stack trace
             * of [violation] matches both [className] or [methods].  If [methods] is empty, all
             * method names match.
             */
            fun classNamed(
                className: String,
                vararg methods: String,
            ) = Predicate { violation ->
                violation.stackTrace.toList().any { element ->
                    className == element.className &&
                            (methods.isEmpty() || methods.contains(element.methodName))
                }
            }

            /**
             * Match a violation based on it's stack trace containing at least one instance
             * of a matching class.
             *
             * @param classMatcher A [PatternMatcher] which will be used to match classes.
             *
             * @return A Predicate that will return true iff a single entry in the stack trace
             * of [violation] matches the [classMatcher].
             */
            fun classNamed(
                classMatcher: PatternMatcher,
            ) = Predicate { violation ->
                violation.stackTrace
                    .map(StackTraceElement::getClassName)
                    .any(classMatcher::match)
            }

            /**
             * Match a violation by class of violation.
             *
             * @param failures Violation subclasses to match
             * @return a [Predicate] that will only match if the class of the violation matches
             * one of [failures].
             */
            fun violation(
                vararg failures: KClass<out Violation>
            ) = Predicate { violation ->
                failures.contains(violation::class)
            }

            /**
             * Match only if all included predicates match.
             *
             * @param predicates list of predicates to match.
             * @return a [Predicate] that will only match a violation if all of the
             * predicates in [predicates] match.
             */
            fun allOf(vararg predicates: Predicate) = Predicate { violation ->
                !predicates.any {
                    !it.test(violation)
                }
            }

            /**
             * Match if any of the included predicates matches.
             *
             * @param predicates list of predicates to match.
             * @return a [Predicate] that will only match a violation if at least one
             * of the predicates in [predicates] matches.  Once a predicate has matched,
             * no further predicates will be tested on violation.
             */
            fun anyOf(vararg predicates: Predicate) = Predicate { violation ->
                predicates.any { it.test(violation) }
            }
        }

        /** A collection of common [Action]. */
        object Actions {
            /**
             * Perform each included [Action] in order.
             *
             * @param actions Actions to perform.
             */
            fun allOf(vararg actions: Action) = Action { violation ->
                actions.forEach { it.perform(violation) }
            }

            /**
             * A no op that precludes further matching.
             */
            fun allow() = Action { _ ->
            }

            /**
             * Log a violation.
             *
             * @param policyName Policy name to attach to the log message.
             */
            fun log(policyName: String) = Action { violation ->
                if (VERSION.SDK_INT >= VERSION_CODES.P) {
                    Log.e("StrictMode", "$policyName violation", violation)
                } else {
                    Log.e("StrictMode", "$policyName violation: $violation")
                }
            }

            /**
             * Terminate the application if the rule is matched.
             */
            fun terminate() = Action { violation ->
                Looper.getMainLooper().let(::Handler).post {
                    throw violation
                }
            }
        }
    }
}
