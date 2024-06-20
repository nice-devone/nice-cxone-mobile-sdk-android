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

import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity.Maintainability
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.psiUtil.getCallNameExpression
import org.jetbrains.kotlin.psi.psiUtil.getReceiverExpression

internal class ProhibitedCallRule(
    config: Config = Config.empty
) : Rule(config) {
    override val issue = Issue(
        "ProhibitedCall",
        Maintainability,
        "Using prohibited logging call.",
        Debt.FIVE_MINS
    )

    data class ProhibitedCall(
        val pack: Regex,
        val name: Regex,
        val recommendation: String
    ) {
        constructor(pack: String, name: String, recommendation: String)
                : this(Regex(pack), Regex(name), recommendation)
    }

    private val prohibitions = listOf(
        ProhibitedCall("android.util.Log", ".*", "Use Logger instead of android.util.Log"),
        ProhibitedCall("(|System.out)", "println", "use Logger instead of println")
    )

    override fun visitCallExpression(expression: KtCallExpression) {
        expression.getCallNameExpression()?.let { cne ->
            for (prohibition in prohibitions) {
                if (
                    (cne.text?.let(prohibition.name::matchEntire) == null) or
                    (prohibition.pack.matchEntire(cne.getReceiverExpression()?.text ?: "") == null)
                ) {
                    continue
                }

                report(
                    CodeSmell(
                        issue,
                        Entity.from(expression),
                        prohibition.recommendation
                    )
                )
            }
        }
    }
}
